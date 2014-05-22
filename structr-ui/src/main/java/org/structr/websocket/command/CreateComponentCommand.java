/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
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


import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.web.entity.dom.DOMElement;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.ShadowDocument;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;


/**
 * Create a shared component as a clone of the source node.
 *
 * This command will create a SYNC relationship: (source)<-[:SYNC]-(component)
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

			final DOMElement node = (DOMElement) getDOMNode(id);

			try {

				DOMElement clonedNode = (DOMElement) node.cloneNode(false);
				moveChildNodes(node, clonedNode);

				ShadowDocument hiddenDoc = getOrCreateHiddenDocument();
				clonedNode.setProperty(DOMNode.ownerDocument, hiddenDoc);

				// Change page (owner document) of all children recursively
				for (DOMNode child : DOMNode.getAllChildNodes(clonedNode)) {
					child.setProperty((DOMNode.ownerDocument), hiddenDoc);
				}

				node.setProperty(DOMElement.sharedComponent, clonedNode);
				
			} catch (FrameworkException ex) {

				// send DOM exception
				getWebSocket().send(MessageBuilder.status().code(422).message(ex.getMessage()).build(), true);

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
