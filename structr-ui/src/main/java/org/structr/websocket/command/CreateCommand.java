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

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.CreateNodeCommand;
import org.structr.core.graph.StructrTransaction;
import org.structr.core.graph.TransactionCommand;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

//~--- JDK imports ------------------------------------------------------------

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.core.property.PropertyMap;
import org.structr.web.entity.File;
import org.structr.websocket.StructrWebSocket;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Christian Morgner
 */
public class CreateCommand extends AbstractCommand {

	private static final Logger logger = Logger.getLogger(CreateCommand.class.getName());
	
	static {
		
		StructrWebSocket.addCommand(CreateCommand.class);

	}

	//~--- methods --------------------------------------------------------

	@Override
	public void processMessage(final WebSocketMessage webSocketData) {

		final SecurityContext securityContext = getWebSocket().getSecurityContext();
		StructrTransaction transaction        = new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {

				Map<String, Object> nodeData = webSocketData.getNodeData();

				nodeData.put(AbstractNode.visibleToAuthenticatedUsers.jsonName(), true);

				// convertFromInput
				PropertyMap properties = PropertyMap.inputTypeToJavaType(securityContext, nodeData);
				
				return Services.command(securityContext, CreateNodeCommand.class).execute(properties);
			}
		};

		try {

			// create node in transaction
			AbstractNode newNode = (AbstractNode) Services.command(securityContext, TransactionCommand.class).execute(transaction);

			// check for File node and store in WebSocket to receive chunks
			if (newNode instanceof File) {

				File fileNode = (File) newNode;
				String uuid   = newNode.getProperty(AbstractNode.uuid);

				fileNode.setRelativeFilePath(File.getDirectoryPath(uuid) + "/" + uuid);
				getWebSocket().createFileUploadHandler((File) newNode);

			}
		} catch (FrameworkException fex) {

			logger.log(Level.WARNING, "Could not create node.", fex);
			getWebSocket().send(MessageBuilder.status().code(fex.getStatus()).message(fex.getMessage()).build(), true);
		}
	}

	//~--- get methods ----------------------------------------------------

	@Override
	public String getCommand() {
		return "CREATE";
	}
}
