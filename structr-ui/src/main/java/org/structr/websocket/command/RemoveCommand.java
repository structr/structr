/*
 *  Copyright (C) 2010-2012 Axel Morgner
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

import org.neo4j.graphdb.Direction;

import org.structr.common.RelType;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.graph.DeleteRelationshipCommand;
import org.structr.core.graph.StructrTransaction;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.graph.search.Search;
import org.structr.core.graph.search.SearchAttribute;
import org.structr.core.graph.search.SearchNodeCommand;
import org.structr.web.common.RelationshipHelper;
import org.structr.web.entity.Page;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

//~--- JDK imports ------------------------------------------------------------

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import org.apache.commons.lang.StringUtils;
import org.structr.core.property.LongProperty;
import org.structr.core.property.PropertyKey;
import org.structr.core.Result;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Christian Morgner
 * @author Axel Morgner
 */
public class RemoveCommand extends AbstractCommand {

	@Override
	public void processMessage(WebSocketMessage webSocketData) {

		final SecurityContext securityContext = getWebSocket().getSecurityContext();

		// create static relationship
		String id                = webSocketData.getId();
		// String parentId          = (String) webSocketData.getNodeData().get("id");
		//final String componentId = (String) webSocketData.getNodeData().get("componentId");
		final String treeAddress = (String) webSocketData.getNodeData().get("treeAddress");
		final String pageId;
		String position;
		
		if (StringUtils.isNotBlank(treeAddress)) {
			pageId		= treeAddress.substring(0, 32);
			position	= StringUtils.substringAfterLast(treeAddress, "_");
		} else {
			pageId		= (String) webSocketData.getNodeData().get("pageId");
			position        = (String) webSocketData.getNodeData().get("position");
		}

		if (id != null) {

			final AbstractNode nodeToRemove = getNode(id);
			//final AbstractNode parentNode   = getNode(parentId);
			final Long pos                  = (position != null)
							  ? Long.parseLong(position)
							  : null;
			
			if (nodeToRemove != null) {

				final List<AbstractRelationship> rels = nodeToRemove.getRelationships(RelType.CONTAINS, Direction.INCOMING);
				StructrTransaction transaction        = new StructrTransaction() {

					@Override
					public Object execute() throws FrameworkException {

						DeleteRelationshipCommand deleteRel      = Services.command(securityContext, DeleteRelationshipCommand.class);
						List<AbstractRelationship> relsToReorder = new ArrayList<AbstractRelationship>();
						PropertyKey<Long> pageIdProperty         = new LongProperty(pageId);
						boolean hasPageId;
						
						for (AbstractRelationship rel : rels) {

							if (pageId == null || rel.getProperty(pageIdProperty) != null) {

//                                                              if (rel.getEndNode().equals(nodeToRemove) && ((componentId == null) || componentId.equals(rel.getProperty("componentId")))) {
								if (rel.getEndNode().equals(nodeToRemove)) {

									relsToReorder.remove(rel);

									if (pos == null) {

										deleteRel.execute(rel);
										
									} else {

										if (pos.equals(rel.getLongProperty(pageIdProperty))) {

											rel.removeProperty(pageIdProperty);
											//RelationshipHelper.untagOutgoingRelsFromPageId(nodeToRemove, nodeToRemove, pageId, pageId);

											hasPageId = hasPageIds(securityContext, rel);

											// If no pageId property is left, remove relationship
											if (!hasPageId) {

												deleteRel.execute(rel);

												break;

											}

										}

									}

								} else {

									relsToReorder.add(rel);
								}
							}

						}

						// Re-order relationships
						RelationshipHelper.reorderRels(relsToReorder, pageId);

						return null;

					}

				};

				// execute transaction
				try {

					Services.command(securityContext, TransactionCommand.class).execute(transaction);

				} catch (FrameworkException fex) {

					getWebSocket().send(MessageBuilder.status().code(400).message(fex.getMessage()).build(), true);

				}

			} else {

				getWebSocket().send(MessageBuilder.status().code(404).build(), true);
			}

		} else {

			getWebSocket().send(MessageBuilder.status().code(400).message("Add needs id and data.id!").build(), true);
		}

	}

	//~--- get methods ----------------------------------------------------

	@Override
	public String getCommand() {

		return "REMOVE";

	}

	private boolean hasPageIds(final SecurityContext securityContext, final AbstractRelationship rel) throws FrameworkException {

		SearchNodeCommand searchNode = Services.command(securityContext, SearchNodeCommand.class);
		long count                   = 0;

		for (Entry<PropertyKey, Object> entry : rel.getProperties().entrySet()) {

			PropertyKey key = entry.getKey();

			// Object val = entry.getValue();
			// Check if key is a node id (UUID format)
			if (key.dbName().matches("[a-zA-Z0-9]{32}")) {

				List<SearchAttribute> attrs = new LinkedList<SearchAttribute>();

				attrs.add(Search.andExactType(Page.class.getSimpleName()));
				attrs.add(Search.andExactUuid(key.dbName()));

				Result results = searchNode.execute(attrs);

				if (results != null && !results.isEmpty()) {

					count++;
					
				} else {

					// UUID, but not page found: Remove this property
					rel.removeProperty(key);
				}

			}

		}

		return count > 0;

	}

}
