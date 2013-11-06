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
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.ShadowDocument;
import org.structr.web.entity.relation.Sync;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;


/**
 * Create a component as a clone of the source node.
 *
 * This command will create default SYNC relationships (bi-directional)
 *
 * @author Axel Morgner
 */
public class CreateComponentCommand extends AbstractCommand {

	static {

		StructrWebSocket.addCommand(CreateComponentCommand.class);
	}

	@Override
	public void processMessage(WebSocketMessage webSocketData) {

		final SecurityContext securityContext = getWebSocket().getSecurityContext();
		final App app                         = StructrApp.getInstance(securityContext);
		String id                             = webSocketData.getId();

		if (id != null) {

			final DOMNode node = getDOMNode(id);

			try {

				app.beginTx();

				DOMNode clonedNode = (DOMNode) node.cloneNode(false);
				moveChildNodes(node, clonedNode);

				ShadowDocument hiddenDoc = getOrCreateHiddenDocument();
				clonedNode.setProperty(DOMNode.ownerDocument, hiddenDoc);

				// Change page (owner document) of all children recursively
				for (DOMNode child : DOMNode.getAllChildNodes(clonedNode)) {
					child.setProperty((DOMNode.ownerDocument), hiddenDoc);
				}

				app.create(node, clonedNode, Sync.class);
				app.create(clonedNode, node, Sync.class);
				
				app.commitTx();

			} catch (Exception ex) {

				// send DOM exception
				getWebSocket().send(MessageBuilder.status().code(422).message(ex.getMessage()).build(), true);

			} finally {

				app.finishTx();
			}

		} else {

			getWebSocket().send(MessageBuilder.status().code(422).message("Cannot append node without id").build(), true);
		}

	}

	@Override
	public String getCommand() {

		return "CREATE_COMPONENT";

	}


}
