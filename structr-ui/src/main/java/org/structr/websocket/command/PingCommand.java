/**
 * Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 * This file is part of structr <http://structr.org>.
 *
 * structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */


package org.structr.websocket.command;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.WebSocketMessage;

//~--- classes ----------------------------------------------------------------

/**
 * Websocket heartbeat command
 * 
 * @author Axel Morgner
 */
public class PingCommand extends AbstractCommand {
	
	private static final Logger logger = Logger.getLogger(PingCommand.class.getName());
	
	static {
		
		StructrWebSocket.addCommand(PingCommand.class);
		
	}

	@Override
	public void processMessage(final WebSocketMessage webSocketData) {


		logger.log(Level.INFO, "PING recieved from session {0}", webSocketData.getSessionId());
	}

	//~--- get methods ----------------------------------------------------

	@Override
	public String getCommand() {
		return "PING";
	}
}
