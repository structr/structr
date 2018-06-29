/**
 * Copyright (C) 2010-2018 Structr GmbH
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
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.websocket.command;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.TransactionCommand;
import org.structr.web.entity.AbstractFile;
import org.structr.web.entity.Folder;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

/**
 *
 *
 */
public class AppendFileCommand extends AbstractCommand {

	private static final Logger logger     = LoggerFactory.getLogger(AppendFileCommand.class.getName());

	static {

		StructrWebSocket.addCommand(AppendFileCommand.class);
	}

	@Override
	public void processMessage(final WebSocketMessage webSocketData) {

		setDoTransactionNotifications(true);

		String id                    = webSocketData.getId();
		Map<String, Object> nodeData = webSocketData.getNodeData();
		String parentId              = (String) nodeData.get("parentId");

		// check node to append
		if (id == null) {

			getWebSocket().send(MessageBuilder.status().code(422).message("Cannot append node, no id is given").build(), true);

			return;

		}

		// check for parent ID
		if (parentId == null) {

			getWebSocket().send(MessageBuilder.status().code(422).message("Cannot append node without parentId").build(), true);

			return;

		}

		// never append to self
		if (parentId.equals(id)) {

			getWebSocket().send(MessageBuilder.status().code(422).message("Cannot append node as its own child.").build(), true);

			return;

		}


		// check if parent node with given ID exists
		AbstractNode parentNode = getNode(parentId);

		if (parentNode == null) {

			getWebSocket().send(MessageBuilder.status().code(404).message("Parent node not found").build(), true);

			return;

		}

		if (parentNode instanceof Folder) {

			Folder folder = (Folder) parentNode;

			AbstractFile file = (AbstractFile) getNode(id);

			if (file != null) {

				try {
					// Remove from existing parent
					Folder currentParent = (Folder)file.treeGetParent();
					if (currentParent != null) {

						currentParent.treeRemoveChild(file);
					}

					folder.treeAppendChild(file);

					TransactionCommand.registerNodeCallback(file, callback);

				} catch (FrameworkException ex) {
					logger.error("", ex);
					getWebSocket().send(MessageBuilder.status().code(422).message("Cannot append file").build(), true);
				}
			}


		} else {

			// send exception
			getWebSocket().send(MessageBuilder.status().code(422).message("Parent node is not instance of Folder").build(), true);
		}

	}

	@Override
	public String getCommand() {

		return "APPEND_FILE";

	}

}
