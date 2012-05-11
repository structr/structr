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

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Command;
import org.structr.core.EntityContext;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.entity.RelationClass;
import org.structr.core.node.DeleteRelationshipCommand;
import org.structr.core.node.StructrTransaction;
import org.structr.core.node.TransactionCommand;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

//~--- JDK imports ------------------------------------------------------------

import java.util.LinkedList;
import java.util.List;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Christian Morgner
 */
public class RemoveCommand extends AbstractCommand {

	@Override
	public void processMessage(WebSocketMessage webSocketData) {

		final SecurityContext securityContext = getWebSocket().getSecurityContext();

		// create static relationship
		String id                = webSocketData.getId();
		String parentId          = (String) webSocketData.getNodeData().get("id");
		final String componentId = (String) webSocketData.getNodeData().get("componentId");
		final String resourceId  = (String) webSocketData.getNodeData().get("resourceId");
		String position          = (String) webSocketData.getNodeData().get("position");

		if ((id != null) && (parentId != null)) {

			final AbstractNode nodeToRemove = getNode(id);
			final AbstractNode parentNode   = getNode(parentId);
			final Long pos                  = (position != null)
							  ? Long.parseLong(position)
							  : null;

			if ((nodeToRemove != null) && (parentNode != null)) {

				RelationClass rel = EntityContext.getRelationClass(nodeToRemove.getClass(), parentNode.getClass());

				if (rel != null) {

					final List<AbstractRelationship> rels = nodeToRemove.getRelationships(rel.getRelType(), rel.getDirection());
					StructrTransaction transaction        = new StructrTransaction() {

						@Override
						public Object execute() throws FrameworkException {

							Command deleteRel                      = Services.command(securityContext, DeleteRelationshipCommand.class);
							List<AbstractRelationship> relsToShift = new LinkedList<AbstractRelationship>();

							for (AbstractRelationship rel : rels) {

								if (rel.getOtherNode(nodeToRemove).equals(parentNode)
									&& ((componentId == null) || componentId.equals(rel.getStringProperty("componentId")))
									&& ((resourceId == null) || (rel.getProperty(resourceId) != null))) {

									relsToShift.add(rel);

									if (pos.equals(rel.getLongProperty(resourceId))) {

										deleteRel.execute(rel);
										relsToShift.remove(rel);

										// Stop after removal of one relationship!
										break;

									}

								}

							}

							// After removal of a relationship, all other rels must get a new position id
							if (pos != null) {

								moveUpRels(relsToShift, resourceId);

							}

							return null;
						}
					};

					// execute transaction
					try {
						Services.command(securityContext, TransactionCommand.class).execute(transaction);
					} catch (FrameworkException fex) {
						getWebSocket().send(MessageBuilder.status().code(400).message(fex.getMessage()).build(), true);
					}

				}

			} else {

				getWebSocket().send(MessageBuilder.status().code(404).build(), true);

			}

		} else {

			getWebSocket().send(MessageBuilder.status().code(400).message("Add needs id and data.id!").build(), true);

		}
	}

	private void moveUpRels(final List<AbstractRelationship> rels, final String resourceId) throws FrameworkException {

		long i = 0;

		for (AbstractRelationship rel : rels) {

			try {
				rel.getId();
			} catch (IllegalStateException ise) {

				// Silently ignore this exception and continue, omitting deleted rels
				continue;
			}

			rel.setProperty(resourceId, i);

			i++;

		}
	}

	//~--- get methods ----------------------------------------------------

	@Override
	public String getCommand() {
		return "REMOVE";
	}
}
