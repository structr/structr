/**
 * Copyright (C) 2010-2014 Structr, c/o Morgner UG (haftungsbeschränkt) <structr@structr.org>
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
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.websocket.command;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.graph.DeleteRelationshipCommand;
import org.structr.core.graph.NodeInterface;
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

	private static final Logger logger = Logger.getLogger(RemoveCommand.class.getName());

	@Override
	public void processMessage(WebSocketMessage webSocketData) {

		final SecurityContext securityContext = getWebSocket().getSecurityContext();
		String id                             = webSocketData.getId();

		if (id != null) {

			final NodeInterface node = getNode(id);

			if (node != null) {

				App app = StructrApp.getInstance(securityContext);
				
				if (node instanceof DOMNode) {

					// Use new DOM interface
					DOMNode domNode = (DOMNode) node;

					domNode.getParentNode().removeChild(domNode);
					
					try {

						// Remove node from page
						domNode.setProperty(DOMNode.pageId, null);

					} catch (FrameworkException ex) {

						logger.log(Level.SEVERE, "Could not remove node from page " + domNode, ex);

					}
					
				} else {
					try {

						// Old style: Delete all incoming CONTAINS rels
						app.beginTx();

						for (AbstractRelationship rel : node.getIncomingRelationships()) {

							try {

								if ("CONTAINS".equals(rel.getType())) {

									app.delete(rel);

								}

							} catch (FrameworkException ex) {

								logger.log(Level.SEVERE, "Could not remove relationship " + rel, ex);

							}

						}
						
						app.commitTx();
						
					} catch (Throwable t) {
						
						getWebSocket().send(MessageBuilder.status().code(400).message("Error in RemoveCommand: " + t.getMessage()).build(), true);
						
					} finally {
						
						app.finishTx();
						
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
