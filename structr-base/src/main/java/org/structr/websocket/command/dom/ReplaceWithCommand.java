/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.websocket.command.dom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.Traits;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.web.traits.wrappers.dom.DOMNodeTraitWrapper;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

import java.util.Collections;
import java.util.Map;

/**
 *
 *
 */
public class ReplaceWithCommand extends CreateAndAppendDOMNodeCommand {

	private static final Logger logger = LoggerFactory.getLogger(ReplaceWithCommand.class.getName());

	static {

		StructrWebSocket.addCommand(ReplaceWithCommand.class);
	}

	@Override
	public void processMessage(final WebSocketMessage webSocketData) {

		setDoTransactionNotifications(true);

		final SecurityContext securityContext = getWebSocket().getSecurityContext();
		final Map<String, Object> nodeData    = webSocketData.getNodeData();
		final String pageId                   = webSocketData.getPageId();
		final String tagName                  = (String) nodeData.remove("tagName");
		final String nodeId                   = (String) nodeData.remove("nodeId");
		final Boolean inheritVisibilityFlags  = (Boolean) nodeData.getOrDefault("inheritVisibilityFlags", false);
		final Boolean inheritGrantees         = (Boolean) nodeData.getOrDefault("inheritGrantees", false);

		// remove configuration elements from the nodeData so we don't set it on the node
		nodeData.remove("inheritVisibilityFlags");
		nodeData.remove("inheritGrantees");

		if (pageId == null) {

			getWebSocket().send(MessageBuilder.status().code(422).message("Cannot insert node without pageId").build(), true);
			return;
		}

		if (nodeId == null) {

			getWebSocket().send(MessageBuilder.status().code(422).message("Cannot insert node without a reference nodeId").build(), true);
			return;
		}

		if (tagName == null || tagName.isEmpty()) {

			getWebSocket().send(MessageBuilder.status().code(404).message("Cannot create node without tagname").build(), true);
			return;
		}

		// check if content node with given ID exists
		final DOMNode refNode = getDOMNode(nodeId);
		if (refNode == null) {

			getWebSocket().send(MessageBuilder.status().code(404).message("Node not found").build(), true);
			return;
		}

		final Page document = getPage(pageId);

		if (document == null) {

			getWebSocket().send(MessageBuilder.status().code(404).message("Page not found").build(), true);
			return;
		}

		final DOMNode parentNode = refNode.getParent();

		if (parentNode == null) {
			getWebSocket().send(MessageBuilder.status().code(404).message("Node has no parent node").build(), true);
			return;
		}

		try {
			DOMNode newNode;

			if ("comment".equals(tagName)) {

				newNode = document.createComment("#comment");

			} else if ("#template".equals(tagName)) {

				newNode = document.createTextNode("#template");

				try {

					newNode.unlockSystemPropertiesOnce();
					newNode.setProperties(newNode.getSecurityContext(), new PropertyMap(Traits.typeProperty(), "Template"));

				} catch (FrameworkException fex) {

					logger.warn("Unable to set type of node {} to Template: {}", new Object[] { newNode.getUuid(), fex.getMessage() } );
				}

			} else if ("#content".equals(tagName)) {

				// TODO: this can not work - content elements can not have children!
				newNode = document.createTextNode("#text");

			} else {

				newNode = document.createElement(tagName);
			}

			newNode = getDOMNode(newNode.getUuid());

			if (newNode != null) {

				// move all children from refNode to newNode
				for (final DOMNode child : refNode.getChildren()) {
					refNode.removeChild(child);
					newNode.appendChild(child);
				}

				// replace current node with new one..
				parentNode.replaceChild(newNode, refNode);

				// copy attributes etc..
				DOMNodeTraitWrapper.copyAllAttributes(refNode, newNode);

				// Remove old node from page
				final PropertyMap changedProperties = new PropertyMap();
				final Traits traits                 = Traits.of("DOMNode");

				changedProperties.put(traits.key("syncedNodes"), Collections.EMPTY_LIST);
				changedProperties.put(traits.key("pageId"),      null);

				refNode.setProperties(securityContext, changedProperties);


				if (inheritVisibilityFlags) {

					copyVisibilityFlags(parentNode, newNode);
				}

				if (inheritGrantees) {

					copyGrantees(parentNode, newNode);
				}
			}

			TransactionCommand.registerNodeCallback(newNode.getWrappedNode(), callback);

		} catch (Exception ex) {

			// send DOM exception
			getWebSocket().send(MessageBuilder.status().code(422).message(ex.getMessage()).build(), true);
		}
	}

	@Override
	public String getCommand() {
		return "REPLACE_WITH";
	}

	@Override
	public boolean requiresEnclosingTransaction() {
		return true;
	}
}
