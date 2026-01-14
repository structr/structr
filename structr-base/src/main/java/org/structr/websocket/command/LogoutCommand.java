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

import org.structr.api.config.Settings;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Principal;
import org.structr.core.graph.Tx;
import org.structr.rest.auth.AuthHelper;
import org.structr.rest.auth.SessionHelper;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

/**
 *
 *
 */
public class LogoutCommand extends AbstractCommand {

	static {

		StructrWebSocket.addCommand(LogoutCommand.class);

	}

	@Override
	public void processMessage(final WebSocketMessage webSocketData) throws FrameworkException {

		if (Settings.CallbacksOnLogout.getValue() == false) {
			getWebSocket().getSecurityContext().disableInnerCallbacks();
		}

		final App app = StructrApp.getInstance(getWebSocket().getSecurityContext());

		try (final Tx tx = app.tx(true, true, true)) {

			final Principal user = getWebSocket().getCurrentUser();

			if (user != null) {

				final String sessionId = SessionHelper.getShortSessionId(webSocketData.getSessionId());
				if (sessionId != null) {

					SessionHelper.clearSession(sessionId);
					SessionHelper.invalidateSession(sessionId);
				}

				AuthHelper.sendLogoutNotification(user, getWebSocket().getRequest());

				getWebSocket().setAuthenticated(null, null);

				getWebSocket().send(MessageBuilder.status().code(401).build(), true);

				getWebSocket().invalidateConsole();
			}

			tx.success();
		}
	}

	@Override
	public String getCommand() {
		return "LOGOUT";
	}

	@Override
	public boolean requiresEnclosingTransaction () {
		return false;
	}
}
