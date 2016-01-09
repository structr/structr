/**
 * Copyright (C) 2010-2016 Structr GmbH
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
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.error.FrameworkException;

import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Principal;
import org.structr.core.entity.Group;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

/**
 *
 *
 */
public class AppendUserCommand extends AbstractCommand {
		
	private static final Logger logger     = Logger.getLogger(AppendUserCommand.class.getName());

	static {

		StructrWebSocket.addCommand(AppendUserCommand.class);
	}

	@Override
	public void processMessage(WebSocketMessage webSocketData) {

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

			getWebSocket().send(MessageBuilder.status().code(422).message("Cannot add node without parentId").build(), true);

			return;

		}

		// check if parent node with given ID exists
		AbstractNode parentNode = getNode(parentId);

		if (parentNode == null) {

			getWebSocket().send(MessageBuilder.status().code(404).message("Parent node not found").build(), true);

			return;

		}

		if (parentNode instanceof Group) {

			Group group = (Group) parentNode;
			
			Principal user = (Principal) getNode(id);

			if (user != null) {
				try {
					group.addMember(user);
				} catch (FrameworkException ex) {
					logger.log(Level.SEVERE, null, ex);
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

		return "APPEND_USER";

	}

}
