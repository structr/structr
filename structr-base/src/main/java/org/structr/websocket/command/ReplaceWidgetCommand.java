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
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.command.dom.ReplaceWithCommand;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;


public class ReplaceWidgetCommand extends AbstractCommand {

	private static final Logger logger     = LoggerFactory.getLogger(ReplaceWidgetCommand.class.getName());

	static {

		StructrWebSocket.addCommand(ReplaceWidgetCommand.class);
	}

	@Override
	public void processMessage(final WebSocketMessage webSocketData) throws FrameworkException {

		setDoTransactionNotifications(true);

		final String pageId                   = webSocketData.getPageId();
		final String nodeId                   = webSocketData.getNodeDataStringValue("nodeId");
		final String baseUrl                  = webSocketData.getNodeDataStringValue("widgetHostBaseUrl");
		final boolean processDeploymentInfo   = webSocketData.getNodeDataBooleanValue("processDeploymentInfo");
		final SecurityContext securityContext = webSocketData.getSecurityContext();

		// check for node ID
		if (nodeId == null) {

			getWebSocket().send(MessageBuilder.status().code(422).message("Cannot replace node without nodeId").build(), true);

			return;
		}

		// check if parent node with given ID exists
		final DOMNode nodeToReplace = getDOMNode(nodeId);
		if (nodeToReplace == null) {

			getWebSocket().send(MessageBuilder.status().code(404).message("Node to replace is not a DOMNode or node not found").build(), true);
			return;
		}

		Page page = getPage(pageId);
		if (page != null) {

			try {

				final DOMNode parentNode = nodeToReplace.getParent();
				if (parentNode != null) {

					final DOMNode tmpParent = page.createElement("div");

					Widget.expandWidget(getWebSocket().getSecurityContext(), page, tmpParent, baseUrl, webSocketData.getNodeData(), processDeploymentInfo);

					// which is the new node?
					final DOMNode newNode = tmpParent.getFirstChild();

					ReplaceWithCommand.replaceNode(securityContext, parentNode, nodeToReplace, newNode, false, true, true);

					// remove temporary parent
					StructrApp.getInstance(securityContext).delete(tmpParent);

					TransactionCommand.registerNodeCallback(parentNode, callback);

				} else {

					// log error?
				}

				// send success
				getWebSocket().send(webSocketData, true);

			} catch (Throwable fex) {

				logger.warn(fex.toString());

				// send exception
				getWebSocket().send(MessageBuilder.status().code(422).message(fex.toString()).build(), true);
			}
		}
	}

	@Override
	public String getCommand() {
		return "REPLACE_WIDGET";
	}
}
