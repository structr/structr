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
import org.structr.core.entity.AbstractNode;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

//~--- JDK imports ------------------------------------------------------------

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyMap;
import org.structr.core.schema.SchemaHelper;
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
		final App app = StructrApp.getInstance(securityContext);

		Map<String, Object> nodeData = webSocketData.getNodeData();

		try {

			app.beginTx();
			
			final PropertyMap properties	= PropertyMap.inputTypeToJavaType(securityContext, nodeData);
			Class type			= SchemaHelper.getEntityClassForRawType(properties.get(AbstractNode.type));
			final NodeInterface newNode	= app.create(type, properties);

			// check for File node and store in WebSocket to receive chunks
			if (newNode instanceof File) {
				
				long size		= (Long) webSocketData.getNodeData().get("size");
				String contentType	= (String) webSocketData.getNodeData().get("contentType");
				String name		= (String) webSocketData.getNodeData().get("name");

				File fileNode = (File) newNode;
				String uuid   = newNode.getProperty(AbstractNode.uuid);

				fileNode.setRelativeFilePath(File.getDirectoryPath(uuid) + "/" + uuid);
				fileNode.setSize(size);
				fileNode.setContentType(contentType);
				fileNode.setProperty(AbstractNode.name, name);
				
				getWebSocket().createFileUploadHandler(fileNode);

			}
			
			app.commitTx();
			
		} catch (FrameworkException fex) {

			logger.log(Level.WARNING, "Could not create node.", fex);
			getWebSocket().send(MessageBuilder.status().code(fex.getStatus()).message(fex.getMessage()).build(), true);

		} finally {

			app.finishTx();
		}
	}

	//~--- get methods ----------------------------------------------------

	@Override
	public String getCommand() {
		return "CREATE";
	}
}
