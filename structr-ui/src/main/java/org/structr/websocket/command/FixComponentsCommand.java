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


import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Result;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.CreateRelationshipCommand;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.web.entity.relation.Sync;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;


/**
 * Fix 'lost' components
 *
 *
 */
public class FixComponentsCommand extends AbstractCommand {

	private static final Logger logger                                  = Logger.getLogger(FixComponentsCommand.class.getName());

	static {

		StructrWebSocket.addCommand(FixComponentsCommand.class);
	}

	@Override
	public void processMessage(final WebSocketMessage webSocketData) {

		final SecurityContext securityContext = getWebSocket().getSecurityContext();

		try {
			fixLostComponents();

		} catch (FrameworkException ex) {

			// send DOM exception
			getWebSocket().send(MessageBuilder.status().code(422).message(ex.getMessage()).build(), true);

		}

	}

	@Override
	public String getCommand() {

		return "FIX_LOST_COMPONENTS";

	}

	/**
	 * Iterate over all DOM nodes and connect all nodes to the shadow document which
	 * fulfill the following criteria:
	 *
	 * - has child nodes
	 * - has at least one SYNC relationship (out or in)
	 *
	 * @throws FrameworkException
	 */
	private void fixLostComponents() throws FrameworkException {

		Page hiddenDoc                            = CreateComponentCommand.getOrCreateHiddenDocument();
		SecurityContext securityContext           = SecurityContext.getSuperUserInstance();
		Result<DOMNode> result                    = StructrApp.getInstance(securityContext).nodeQuery(DOMNode.class).getResult();
		final CreateRelationshipCommand createRel = StructrApp.getInstance(securityContext).command(CreateRelationshipCommand.class);

		for (DOMNode node : result.getResults()) {

			if (node.hasChildNodes()
				&& (node.hasIncomingRelationships(Sync.class) || node.hasRelationship(Sync.class))
				&& (!hiddenDoc.equals(node.getOwnerDocument()))
				) {

				try {

					DOMNode clonedNode = (DOMNode) node.cloneNode(false);

					moveChildNodes(node, clonedNode);
					clonedNode.setProperty(DOMNode.ownerDocument, hiddenDoc);

					createRel.execute((DOMNode) node, clonedNode, Sync.class);
					createRel.execute(clonedNode, (DOMNode) node, Sync.class);

				} catch (Exception ex) {

					logger.log(Level.SEVERE, "Could not fix component " + node, ex);

				}

			}

		}

	}

}
