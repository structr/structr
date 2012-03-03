/*
 *  Copyright (C) 2011 Axel Morgner
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

import org.structr.web.Importer;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.node.StructrTransaction;
import org.structr.core.node.TransactionCommand;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

//~--- JDK imports ------------------------------------------------------------

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.websocket.StructrWebSocket;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Axel Morgner
 */
public class ImportCommand extends AbstractCommand {

	private static final Logger logger = Logger.getLogger(ImportCommand.class.getName());

	static {
		// import a web page
		StructrWebSocket.addCommand(ImportCommand.class);

	}

	//~--- methods --------------------------------------------------------

	@Override
	public void processMessage(WebSocketMessage webSocketData) {

		final SecurityContext securityContext = SecurityContext.getSuperUserInstance();
		Map<String, Object> properties        = webSocketData.getData();
		final String address                  = (String) properties.get("address");
		final String name                     = (String) properties.get("name");
		final int timeout                     = Integer.parseInt((String) properties.get("timeout"));
		StructrTransaction transaction        = new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {

				Importer.start(address, name, timeout);

				return null;
			}
		};

		try {
			Services.command(securityContext, TransactionCommand.class).execute(transaction);
		} catch (FrameworkException fex) {

			logger.log(Level.WARNING, "Error while importing content", fex);
			getWebSocket().send(MessageBuilder.status().code(fex.getStatus()).message(fex.getMessage()).build(), true);
		}
	}

	//~--- get methods ----------------------------------------------------

	@Override
	public String getCommand() {
		return "IMPORT";
	}
}
