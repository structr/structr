/*
 *  Copyright (C) 2010-2013 Axel Morgner
 * 
 *  This file is part of structr <http://structr.org>.
 * 
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.structr.websocket.command;

import org.structr.common.error.FrameworkException;
import org.structr.core.entity.Principal;
import org.structr.websocket.message.WebSocketMessage;

/**
 *
 * @author Christian Morgner
 */
public class LogoutCommand extends AbstractCommand {

	@Override
	public void processMessage(WebSocketMessage webSocketData) {

		Principal user = getWebSocket().getCurrentUser();
		if(user != null) {

			try {
				user.setProperty(Principal.sessionId, null);

			} catch(FrameworkException fex) {
				fex.printStackTrace();
			}

			// TODO: remove lastAccessedAt property
			// user.setProperty(Principal.Key.session, null);

			getWebSocket().setAuthenticated(null, null);
		}
	}

	@Override
	public String getCommand() {
		return "LOGOUT";
	}

}
