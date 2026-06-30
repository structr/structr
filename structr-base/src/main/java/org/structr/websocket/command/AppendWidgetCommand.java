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
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.traits.StructrTraits;
import org.structr.web.entity.Widget;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.web.traits.definitions.dom.DOMNodeTraitDefinition;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;


public class AppendWidgetCommand extends AbstractCommand {

	private static final Logger logger     = LoggerFactory.getLogger(AppendWidgetCommand.class.getName());

	@Override
	public void processMessage(final WebSocketMessage webSocketData) throws FrameworkException {

		setDoTransactionNotifications(true);

		final SecurityContext securityContext = webSocketData.getSecurityContext();
		final String pageId                   = webSocketData.getPageId();
		final String parentId                 = webSocketData.getNodeDataStringValue("parentId");
		final String baseUrl                  = webSocketData.getNodeDataStringValue("widgetHostBaseUrl");
		final boolean processDeploymentInfo   = webSocketData.getNodeDataBooleanValue("processDeploymentInfo");

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

		if (parentNode.is(StructrTraits.DOM_NODE)) {

			DOMNode parentDOMNode = getDOMNode(parentId);
			if (parentDOMNode == null) {

				getWebSocket().send(MessageBuilder.status().code(422).message("Parent node is no DOM node").build(), true);

				return;
			}

			Page page = getPage(pageId);
			if (page != null) {

				try {

					// create temporary parent for Widget to expand in
					final DOMNode tmpParent = page.createElement("div");

					// Pull the auto-VM spec out of the node-data map BEFORE XML
					// expansion so it does not pollute slot substitution; we
					// apply it ourselves once the widget root is in place.
					final java.util.Map<String, Object> nodeData = webSocketData.getNodeData();

					Widget.expandWidget(page, tmpParent, baseUrl, nodeData, processDeploymentInfo);

					// move children
					DOMNode firstAppendedRoot = null;
					for (final DOMNode newNode : tmpParent.getChildren()) {
						if (firstAppendedRoot == null) {
							firstAppendedRoot = newNode;
						}
						parentDOMNode.appendChild(newNode);
					}

					// remove temporary parent
					StructrApp.getInstance(securityContext).delete(tmpParent);

					// Inherit visibility flags from the append parent onto the
					// widget root and its descendants. Runs for every widget
					// append so the appended subtree picks up the page's
					// audience the same way a manually-created DOMNode does.
					WidgetVisibilityFlagInheritor.apply(firstAppendedRoot, parentDOMNode);

					// Attach auto-VM (no-op if no spec was sent or trait is missing).
					WidgetAutoVisibilityMappingHelper.applyAndConsume(securityContext, firstAppendedRoot, nodeData);

					TransactionCommand.registerNodeCallback(parentDOMNode, callback);

					// send success
					getWebSocket().send(webSocketData, true);

				} catch (Throwable fex) {

					logger.warn(fex.toString());

					// send exception
					getWebSocket().send(MessageBuilder.status().code(422).message(fex.toString()).build(), true);
				}
			}

		} else {

			// send exception
			getWebSocket().send(MessageBuilder.status().code(422).message("Cannot use given node, not instance of DOMNode").build(), true);
		}
	}

	@Override
	public String getCommand() {
		return "APPEND_WIDGET";
	}
}
