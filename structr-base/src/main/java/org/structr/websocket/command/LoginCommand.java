/*
 * Copyright (C) 2010-2026 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.websocket.command;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.HttpHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.api.graph.Identity;
import org.structr.common.AccessMode;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.auth.Authenticator;
import org.structr.core.auth.exception.*;
import org.structr.core.entity.Principal;
import org.structr.core.entity.SuperUser;
import org.structr.core.graph.Tx;
import org.structr.core.traits.definitions.PrincipalTraitDefinition;
import org.structr.rest.auth.AuthHelper;
import org.structr.rest.auth.DeviceTrustHelper;
import org.structr.rest.auth.SessionHelper;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;
import org.structr.websocket.servlet.WebSocketLoginServlet;

import java.util.HashMap;

public class LoginCommand extends AbstractCommand {

	private static final Logger logger = LoggerFactory.getLogger(LoginCommand.class.getName());

	@Override
	public void processMessage(final WebSocketMessage webSocketData) throws FrameworkException {

		final SecurityContext ctx       = SecurityContext.getSuperUserInstance();
		final App app                   = StructrApp.getInstance(ctx);

		if (Settings.CallbacksOnLogin.getValue() == false) {

			ctx.disableInnerCallbacks();
		}

		boolean sendSuccess = false;
		Principal   user   = null;
		Identity<?> userId = null;

		try (final Tx tx = app.tx(true, true, true)) {

			String username             = webSocketData.getNodeDataStringValue("username");
			final String password       = webSocketData.getNodeDataStringValue(PrincipalTraitDefinition.PASSWORD_PROPERTY);
			final String twoFactorToken = webSocketData.getNodeDataStringValue(PrincipalTraitDefinition.TWO_FACTOR_TOKEN_PROPERTY);
			final String twoFactorCode  = webSocketData.getNodeDataStringValue("twoFactorCode");

			try {

				Authenticator auth = getWebSocket().getAuthenticator();

				if (StringUtils.isNotEmpty(twoFactorToken)) {

					user = AuthHelper.getUserForTwoFactorToken(twoFactorToken);

				} else if (StringUtils.isNotEmpty(username) && StringUtils.isNotEmpty(password)) {

					// cleanup user input
					if (StringUtils.contains(username, "@")) {

						username = username.toLowerCase();
					}

					username = username.trim();

					final String superUserName = Settings.SuperUserName.getValue();
					if (StringUtils.equals(superUserName, username)) {

						throw new SuperuserLoginNotSupportedException();
					}

					user = auth.doLogin(getWebSocket().getRequest(), username, password);

					tx.setSecurityContext(SecurityContext.getInstance(user, AccessMode.Backend));

				} else {

					getWebSocket().send(MessageBuilder.status().code(HttpServletResponse.SC_FORBIDDEN).build(), false);
				}

				if (user != null && !(user instanceof SuperUser)) {

					final HttpServletRequest request = getWebSocket().getRequest();
					final boolean userRequestedTrust = webSocketData.getNodeDataBooleanValue(DeviceTrustHelper.DEVICE_TRUST_REQUESTED_STRING);
					final String userAgentString     = request.getHeader(HttpHeader.USER_AGENT.asString());
					final AuthHelper.TwoFactorAuthenticationResult result = AuthHelper.handleTwoFactorAuthentication(user, twoFactorCode, twoFactorToken, userAgentString, AuthHelper.getDeviceTrustCookie(request));

					if (result == AuthHelper.TwoFactorAuthenticationResult.FAILURE) {

						throw new AuthenticationException(AuthHelper.STANDARD_ERROR_MSG);
					}

					SessionHelper.clearInvalidSessions(user);

					final boolean sessionIdFromMessage = webSocketData.getSessionId() != null;
					String sessionId                   = getWebSocket().getSessionId(webSocketData);
					if (sessionId == null) {

						logger.debug("Unable to login {}: No sessionId found", username);
						getWebSocket().send(MessageBuilder.status().code(HttpServletResponse.SC_FORBIDDEN).data("reason", "noSessionCookie").build(), true);

					} else if (SessionHelper.getSessionBySessionId(SessionHelper.getShortSessionId(sessionId)) == null) {

						/* The id may arrive in the message, so the client picks it, and it is what the login
						   ticket is tied to (ticket 1594). The websocket is opened from a page that already
						   has a session; requiring it to exist costs that flow nothing. */
						logger.info("Refusing websocket login for {}: no session exists for the id the client sent.", username);

						getWebSocket().send(MessageBuilder.status().code(HttpServletResponse.SC_FORBIDDEN).data("reason", "noSessionCookie").build(), true);

					} else {

						sessionId = SessionHelper.getShortSessionId(sessionId);

						if (isSessionLimitReached(user, sessionId)) {

							logger.debug("Unable to login {}: Unable to add new sessionId", username);
							getWebSocket().send(MessageBuilder.status().code(HttpServletResponse.SC_FORBIDDEN).data("reason", "sessionLimitExceeded").build(), true);

						} else {

							/* Nothing is bound to the id the browser brought along: it may have been planted by
							   somebody else, and binding it is session fixation. A websocket cannot set a cookie,
							   so the page redeems this ticket over HTTP, where the id is rotated and the new one
							   is bound, see WebSocketLoginServlet. */
							webSocketData.getNodeData().clear();
							// a session id taken from the HttpOnly cookie is not handed to the page
							webSocketData.setSessionId(sessionIdFromMessage ? sessionId : null);
							webSocketData.getNodeData().put("username", user.getName());

							// only for a user the websocket lets in; the login of any other user is answered with 403
							if (getWebSocket().isPrivilegedUser(user)) {

								webSocketData.getNodeData().put("loginTicket", WebSocketLoginServlet.issueTicket(user, sessionId));
							}

							if (result == AuthHelper.TwoFactorAuthenticationResult.SUCCESS && userRequestedTrust) {

								if (user.isDeviceTrustPossible()) {

									// we can not set cookies via WebSocket. We let the frontend know which cookie to set
									logger.info("Two factor authentication: User '{}' requested trust in admin UI login, sending trust cookie data for the client to set", user.getName());

									webSocketData.getNodeData().put("trustTokenCookieName", Settings.TwoFactorDeviceTrustCookieName.getValue());
									webSocketData.getNodeData().put("trustTokenCookieValue", DeviceTrustHelper.generateDeviceTrustToken(userAgentString, user.getDeviceTrustSecret()));

								} else {

									logger.info("Two factor authentication: User '{}' requested trust, but feature is disabled or not allowed for the user ({})", user.getName(), Settings.TwoFactorDeviceTrustEnabled.getKey());
								}
							}

							// the connection that proved the credentials is logged in until the page replaces it with one whose handshake carries the new session id
							getWebSocket().setAuthenticated(sessionId, user);

							tx.setSecurityContext(getWebSocket().getSecurityContext());

							// send success message later (to first commit transaction)
							sendSuccess = true;
						}
					}

					userId = user.getNode().getId();
				}

			} catch (PasswordChangeRequiredException | TooManyFailedLoginAttemptsException | TwoFactorAuthenticationFailedException | TwoFactorAuthenticationTokenInvalidException | LoginAttemptBeforeConfirmationException ex) {

				logger.info("Unable to login {}: {}", username, ex.getMessage());
				getWebSocket().send(MessageBuilder.status().message(ex.getMessage()).code(HttpServletResponse.SC_UNAUTHORIZED).data("reason", ex.getReason()).build(), true);

			} catch (final TwoFactorAuthenticationRequiredException ex) {

				logger.debug(ex.getMessage());

				getWebSocket().send(MessageBuilder.status().message(ex.getMessage()).data(new HashMap<>(ex.getData())).code(HttpServletResponse.SC_ACCEPTED).build(), true);

			} catch (AuthenticationException e) {

				logger.info("Unable to login {}, probably wrong password", username);
				getWebSocket().send(MessageBuilder.status().code(HttpServletResponse.SC_FORBIDDEN).build(), true);

			} catch (SuperuserLoginNotSupportedException s) {

				getWebSocket().send(MessageBuilder.status().code(HttpServletResponse.SC_FORBIDDEN).data("reason", s.getMessage()).build(), true);

			} catch (FrameworkException fex) {

				logger.warn("Unable to execute command", fex);
				getWebSocket().send(MessageBuilder.status().code(HttpServletResponse.SC_UNAUTHORIZED).build(), true);
			}

			tx.success();
		}

		if (sendSuccess) {

			// send broadcast to cluster members to refresh user from db
			Services.getInstance().broadcastLogin(userId.hash());

			getWebSocket().send(webSocketData, false);
		}
	}

	/** The limit as user.addSessionId() applies it when the ticket is redeemed, checked here so the connection is not logged in past it. */
	private boolean isSessionLimitReached(final Principal user, final String sessionId) {

		final int max      = Settings.MaxSessionsPerUser.getValue();
		final String[] ids = user.getSessionIds();

		return max > 0 && ids != null && !ArrayUtils.contains(ids, sessionId) && ids.length >= max;
	}

	@Override
	public String getCommand() {

		return "LOGIN";
	}

	@Override
	public boolean requiresEnclosingTransaction () {

		return false;
	}
}
