/*
 * Copyright (C) 2010-2026 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.websocket.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.Group;
import org.structr.core.entity.Principal;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.traits.StructrTraits;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

/**
 * Apppend a member (user or group) to a group.
 */
public class AppendMemberCommand extends AbstractCommand {

	private static final Logger logger     = LoggerFactory.getLogger(AppendMemberCommand.class.getName());

	static {

		StructrWebSocket.addCommand(AppendMemberCommand.class);
	}

	@Override
	public void processMessage(final WebSocketMessage webSocketData) {

		setDoTransactionNotifications(true);

		String id       = webSocketData.getId();
		String parentId = webSocketData.getNodeDataStringValue("parentId");

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
		NodeInterface parentNode = getNode(parentId);
		if (parentNode == null) {

			getWebSocket().send(MessageBuilder.status().code(404).message("Parent node not found").build(), true);
			return;
		}

		if (parentNode.is(StructrTraits.GROUP)) {

			final Group group       = parentNode.as(Group.class);
			NodeInterface principal = getNode(id);

			if (principal != null) {

				try {

					group.addMember(getWebSocket().getSecurityContext(), principal.as(Principal.class));

					TransactionCommand.registerNodeCallback(group, callback);

				} catch (final FrameworkException ex) {

					if (ex.getStatus() == 403) {
						getWebSocket().send(MessageBuilder.status().code(403).message("Client is not allowed to add member " + principal.getName() + " to group " + group.getName()).build(), true);
					}
				}
			}

		} else {

			// send exception
			getWebSocket().send(MessageBuilder.status().code(422).message("Parent node is not instance of Group").build(), true);
		}
	}

	@Override
	public String getCommand() {
		return "APPEND_MEMBER";
	}
}
