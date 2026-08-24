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
import org.structr.schema.action.ActionContext;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

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

					user = auth.doLogin(getWebSocket().getRequest(), username, password);

					tx.setSecurityContext(SecurityContext.getInstance(user, AccessMode.Backend));

				} else {

					getWebSocket().send(MessageBuilder.status().code(HttpServletResponse.SC_FORBIDDEN).build(), false);
				}

				if (user != null && !(user instanceof SuperUser)) {

					final HttpServletRequest request = getWebSocket().getRequest();
					final boolean userRequestedTrust = webSocketData.getNodeDataBooleanValue(DeviceTrustHelper.DEVICE_TRUST_REQUESTED_STRING);
					final String userAgentString     = request.getHeader(HttpHeader.USER_AGENT.asString());

					final AuthHelper.TwoFactorAuthenticationResult result = AuthHelper.handleTwoFactorAuthentication(user, twoFactorCode, twoFactorToken, ActionContext.getRemoteAddr(request), userAgentString, AuthHelper.getDeviceTrustCookie(request));

					if (result == AuthHelper.TwoFactorAuthenticationResult.FAILURE) {
						throw new AuthenticationException(AuthHelper.STANDARD_ERROR_MSG);
					}

					SessionHelper.clearInvalidSessions(user);

					String sessionId = webSocketData.getSessionId();
					if (sessionId == null) {

						logger.debug("Unable to login {}: No sessionId found", username);
						getWebSocket().send(MessageBuilder.status().code(HttpServletResponse.SC_FORBIDDEN).build(), true);

					} else {

						sessionId = SessionHelper.getShortSessionId(sessionId);

						// Clear possible existing sessions
						SessionHelper.clearSession(sessionId);

						if (!user.addSessionId(sessionId)) {

							logger.debug("Unable to login {}: Unable to add new sessionId", username);
							getWebSocket().send(MessageBuilder.status().code(HttpServletResponse.SC_FORBIDDEN).data("reason", "sessionLimitExceeded").build(), true);

						} else {

							AuthHelper.updateLastLoginDate(user);
							AuthHelper.sendLoginNotification(user, getWebSocket().getRequest());

							// store token in response data
							webSocketData.getNodeData().clear();
							webSocketData.setSessionId(sessionId);
							webSocketData.getNodeData().put("username", user.getName());

							if (result == AuthHelper.TwoFactorAuthenticationResult.SUCCESS && userRequestedTrust) {

								if (Settings.TwoFactorDeviceTrustEnabled.getValue()) {

									// in WS we can not set cookies. We let the frontend know which cookie to set
									logger.info("Two factor authentication: User '{}' requested trust in admin UI login, sending trust cookie data for the client to set", user.getName());

									webSocketData.getNodeData().put("trustTokenCookieName", Settings.TwoFactorDeviceTrustCookieName.getValue());
									webSocketData.getNodeData().put("trustTokenCookieValue", DeviceTrustHelper.generateDeviceTrustToken(userAgentString, user.getDeviceTrustSecret()));

								} else {

									logger.info("Two factor authentication: User '{}' requested trust, but feature is disabled ({})", user.getName(), Settings.TwoFactorDeviceTrustEnabled.getKey());
								}
							}

							// authenticate socket
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

	@Override
	public String getCommand() {

		return "LOGIN";
	}

	@Override
	public boolean requiresEnclosingTransaction () {

		return false;
	}
}
