/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.websocket.command;

import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Principal;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.WebSocketMessage;

/**
 *
 * @author Christian Morgner
 */
public class LogoutCommand extends AbstractCommand {
	
	static {
		
		StructrWebSocket.addCommand(LogoutCommand.class);

	}

	@Override
	public void processMessage(WebSocketMessage webSocketData) {

		final App app        = StructrApp.getInstance(getWebSocket().getSecurityContext());
		final Principal user = getWebSocket().getCurrentUser();
		
		if (user != null) {

			try {
				
				app.beginTx();
				user.setProperty(Principal.sessionId, null);
				app.commitTx();

			} catch(FrameworkException fex) {
				
				fex.printStackTrace();
				
			} finally {
				
				app.finishTx();
			}

			getWebSocket().setAuthenticated(null, null);
		}
	}

	@Override
	public String getCommand() {
		return "LOGOUT";
	}

}
