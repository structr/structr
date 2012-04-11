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
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.File;
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
			public Object execute() throws FrameworkException {
				return Services.command(SecurityContext.getSuperUserInstance(), CreateNodeCommand.class).execute(webSocketData.getData());
			}
		};

		try {
			// create node in transaction
			AbstractNode newNode = (AbstractNode)Services.command(SecurityContext.getSuperUserInstance(), TransactionCommand.class).execute(transaction);

			// check for File node and store in WebSocket to receive chunks
			if(newNode instanceof File) {

				File fileNode = (File) newNode;

				String uuid = newNode.getStringProperty(AbstractNode.Key.uuid);
				String directory = uuid.substring(0,1) + "/" + uuid.substring(1,2) + "/" + uuid.substring(2,3) + "/" + uuid.substring(3,4);
				fileNode.setRelativeFilePath(directory + "/" + uuid);

				getWebSocket().handleFileCreation((File)newNode);
			}

		} catch(FrameworkException fex) {

			logger.log(Level.WARNING, "Could not create node.", fex);
			getWebSocket().send(MessageBuilder.status().code(fex.getStatus()).message(fex.getMessage()).build(), true);
		}

	}

	@Override
	public String getCommand() {
		return "CREATE";
	}
}
