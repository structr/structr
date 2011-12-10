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

import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.SecurityContext;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.node.CreateNodeCommand;
import org.structr.core.node.StructrTransaction;
import org.structr.core.node.TransactionCommand;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

/**
 *
 * @author Christian Morgner
 */
public class CreateCommand extends AbstractCommand {

	private static final Logger logger = Logger.getLogger(CreateCommand.class.getName());

	@Override
	public void processMessage(final WebSocketMessage webSocketData) {

		StructrTransaction transaction = new StructrTransaction() {

			@Override
			public Object execute() throws Throwable {
				return (AbstractNode)Services.command(SecurityContext.getSuperUserInstance(), CreateNodeCommand.class).execute(webSocketData.getData());
			}
		};

		// create node in transaction
		Services.command(SecurityContext.getSuperUserInstance(), TransactionCommand.class).execute(transaction);

		// check for errors
		if(transaction.getCause() != null) {

			logger.log(Level.WARNING, "Could not create node.", transaction.getCause());
			getWebSocket().send(MessageBuilder.status().code(400).message(transaction.getCause().getMessage()).build(), true);
		}
	}

	@Override
	public String getCommand() {
		return "CREATE";
	}
}
