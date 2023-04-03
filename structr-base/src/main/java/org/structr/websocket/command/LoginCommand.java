/*
 * Copyright (C) 2010-2023 Structr GmbH
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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.common.AccessMode;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.auth.Authenticator;
import org.structr.core.auth.exception.*;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Principal;
import org.structr.core.graph.Tx;
import org.structr.rest.auth.AuthHelper;
import org.structr.rest.auth.SessionHelper;
import org.structr.schema.action.ActionContext;
import org.structr.web.function.BarcodeFunction;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

import java.io.UnsupportedEncodingException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class LoginCommand extends AbstractCommand {

	private static final Logger logger = LoggerFactory.getLogger(LoginCommand.class.getName());

	static {

		StructrWebSocket.addCommand(LoginCommand.class);
	}

	@Override
	public void processMessage(final WebSocketMessage webSocketData) throws FrameworkException {

		final SecurityContext ctx       = SecurityContext.getSuperUserInstance();
		final App app                   = StructrApp.getInstance(ctx);

		if (Settings.CallbacksOnLogin.getValue() == false) {
			ctx.disableInnerCallbacks();
		}

		boolean sendSuccess = false;

		try (final Tx tx = app.tx(true, true, true)) {

			String username             = webSocketData.getNodeDataStringValue("username");
			final String password       = webSocketData.getNodeDataStringValue("password");
			final String twoFactorToken = webSocketData.getNodeDataStringValue("twoFactorToken");
			final String twoFactorCode  = webSocketData.getNodeDataStringValue("twoFactorCode");
			Principal user              = null;

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

					getWebSocket().send(MessageBuilder.status().code(403).build(), false);
				}

				if (user != null) {

					Services.getInstance().broadcastLogin(user);

					final boolean twoFactorAuthenticationSuccessOrNotNecessary = AuthHelper.handleTwoFactorAuthentication(user, twoFactorCode, twoFactorToken, ActionContext.getRemoteAddr(getWebSocket().getRequest()));

					if (twoFactorAuthenticationSuccessOrNotNecessary) {

						SessionHelper.clearInvalidSessions(user);

						String sessionId = webSocketData.getSessionId();
						if (sessionId == null) {

							logger.debug("Unable to login {}: No sessionId found", new Object[]{ username, password });
							getWebSocket().send(MessageBuilder.status().code(403).build(), true);

						} else {

							sessionId = SessionHelper.getShortSessionId(sessionId);

							// Clear possible existing sessions
							SessionHelper.clearSession(sessionId);

							if (!user.addSessionId(sessionId)) {

								logger.debug("Unable to login {}: Unable to add new sessionId", new Object[]{ username, password });
								getWebSocket().send(MessageBuilder.status().code(403).data("reason", "sessionLimitExceeded").build(), true);

							} else {

								AuthHelper.updateLastLoginDate(user);
								AuthHelper.sendLoginNotification(user, getWebSocket().getRequest());

								// store token in response data
								webSocketData.getNodeData().clear();
								webSocketData.setSessionId(sessionId);
								webSocketData.getNodeData().put("username", user.getProperty(AbstractNode.name));

								// authenticate socket
								getWebSocket().setAuthenticated(sessionId, user);

								tx.setSecurityContext(getWebSocket().getSecurityContext());

								// send success message later (to first commit transaction)
								sendSuccess = true;
							}
						}
					}

				} else {

					getWebSocket().send(MessageBuilder.status().code(401).build(), true);

				}

			} catch (PasswordChangeRequiredException | TooManyFailedLoginAttemptsException | TwoFactorAuthenticationFailedException | TwoFactorAuthenticationTokenInvalidException ex) {

				logger.info("Unable to login {}: {}", username, ex.getMessage());
				getWebSocket().send(MessageBuilder.status().message(ex.getMessage()).code(401).data("reason", ex.getReason()).build(), true);

			} catch (TwoFactorAuthenticationRequiredException ex) {

				logger.debug(ex.getMessage());

				final MessageBuilder msg = MessageBuilder.status().message(ex.getMessage()).data("token", ex.getNextStepToken());

				if (ex.showQrCode()) {

					try {

						final Principal principal       = ex.getUser();
						final Map<String, Object> hints = new HashMap();
						hints.put("MARGIN", 0);
						hints.put("ERROR_CORRECTION", "M");

						final String qrdata = Base64.getEncoder().encodeToString(BarcodeFunction.getQRCode(Principal.getTwoFactorUrl(principal), "QR_CODE", 200, 200, hints).getBytes("ISO-8859-1"));

						msg.data("qrdata", qrdata);

					} catch (UnsupportedEncodingException uee) {
						logger.warn("Charset ISO-8859-1 not supported!?", uee);
					}
				}

				getWebSocket().send(msg.code(202).build(), true);

			} catch (AuthenticationException e) {

				logger.info("Unable to login {}, probably wrong password", username);
				getWebSocket().send(MessageBuilder.status().code(403).build(), true);

			} catch (FrameworkException fex) {

				logger.warn("Unable to execute command", fex);
				getWebSocket().send(MessageBuilder.status().code(401).build(), true);

			}

			tx.success();
		}

		if (sendSuccess) {
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
