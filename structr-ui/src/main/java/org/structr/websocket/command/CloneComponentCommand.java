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


import java.util.Map;
import org.structr.web.common.RelType;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.graph.CreateRelationshipCommand;
import org.structr.core.graph.StructrTransaction;
import org.structr.core.graph.TransactionCommand;
import org.structr.web.entity.dom.DOMNode;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;


/**
 * Create a node as clone of a component.
 *
 * This command will create default SYNC relationships (bi-directional)
 *
 * @author Axel Morgner
 */
public class CloneComponentCommand extends AbstractCommand {

	static {

		StructrWebSocket.addCommand(CloneComponentCommand.class);
	}

	@Override
	public void processMessage(WebSocketMessage webSocketData) {

		final SecurityContext securityContext	= getWebSocket().getSecurityContext();
		String id				= webSocketData.getId();
		Map<String, Object> nodeData		= webSocketData.getNodeData();
		String parentId				= (String) nodeData.get("parentId");
		
		// check node to append
		if (id == null) {

			getWebSocket().send(MessageBuilder.status().code(422).message("Cannot clone component, no id is given").build(), true);

			return;

		}

		// check for parent ID
		if (parentId == null) {

			getWebSocket().send(MessageBuilder.status().code(422).message("Cannot clone compoinent node without parentId").build(), true);

			return;

		}

		// check if parent node with given ID exists
		final DOMNode parentNode = (DOMNode) getNode(parentId);

		if (parentNode == null) {

			getWebSocket().send(MessageBuilder.status().code(404).message("Parent node not found").build(), true);

			return;

		}


		final DOMNode node = getDOMNode(id);

		try {

			final CreateRelationshipCommand<?> createRel = Services.command(securityContext, CreateRelationshipCommand.class);
			StructrTransaction transaction               = new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {

					DOMNode clonedNode = (DOMNode) node.cloneNode(false);

					parentNode.appendChild(clonedNode);
					//clonedNode.setProperty(DOMNode.parent, parentNode);

					createRel.execute(node, clonedNode, RelType.SYNC, true);
					createRel.execute(clonedNode, node, RelType.SYNC, true);

					return null;

				}

			};

			Services.command(securityContext, TransactionCommand.class).execute(transaction);

		} catch (Exception ex) {

			// send DOM exception
			getWebSocket().send(MessageBuilder.status().code(422).message(ex.getMessage()).build(), true);
		}


	}

	@Override
	public String getCommand() {

		return "CLONE_COMPONENT";

	}
	
}
