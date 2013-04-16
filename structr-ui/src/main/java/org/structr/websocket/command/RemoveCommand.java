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

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.structr.web.common.RelType;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.graph.DeleteRelationshipCommand;
import org.structr.web.entity.dom.DOMNode;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

/**
 *
 * @author Christian Morgner
 * @author Axel Morgner
 */
public class RemoveCommand extends AbstractCommand {

	static {

		StructrWebSocket.addCommand(RemoveCommand.class);

	}

	Logger logger = Logger.getLogger(RemoveCommand.class.getName());

	@Override
	public void processMessage(WebSocketMessage webSocketData) {

		final SecurityContext securityContext = getWebSocket().getSecurityContext();
		String id                             = webSocketData.getId();
		String key                            = (String) webSocketData.getNodeData().get("key");

		if (id != null) {

			final AbstractNode node = getNode(id);

			if (node != null) {

				if (node instanceof DOMNode) {

					// Use new DOM interface
					DOMNode domNode = (DOMNode) node;

					domNode.getParentNode().removeChild(domNode);
					
				} else {

					// Old style: Delete all incoming CONTAINS rels
					DeleteRelationshipCommand deleteRel = Services.command(securityContext, DeleteRelationshipCommand.class);
					List<AbstractRelationship> rels     = node.getIncomingRelationships(RelType.CONTAINS);

					for (AbstractRelationship rel : rels) {

						try {

							deleteRel.execute(rel);

						} catch (FrameworkException ex) {

							logger.log(Level.SEVERE, "Could not remove relationship " + rel, ex);

						}

					}
				}

			} else {

				getWebSocket().send(MessageBuilder.status().code(404).build(), true);
			}

		} else {

			getWebSocket().send(MessageBuilder.status().code(400).message("RemoveCommand called with empty id").build(), true);
		}

	}

	@Override
	public String getCommand() {

		return "REMOVE";

	}

}
