/*
 * Copyright (C) 2010-2023 Structr GmbH
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
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractNode;
import org.structr.web.entity.Widget;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;


public class AppendWidgetCommand extends AbstractCommand {

	private static final Logger logger     = LoggerFactory.getLogger(AppendWidgetCommand.class.getName());

	static {

		StructrWebSocket.addCommand(AppendWidgetCommand.class);
	}

	@Override
	public void processMessage(final WebSocketMessage webSocketData) throws FrameworkException {

		setDoTransactionNotifications(true);

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
		AbstractNode parentNode = getNode(parentId);

		if (parentNode == null) {

			getWebSocket().send(MessageBuilder.status().code(404).message("Parent node not found").build(), true);

			return;
		}

		if (parentNode instanceof DOMNode) {

			DOMNode parentDOMNode = getDOMNode(parentId);
			if (parentDOMNode == null) {

				getWebSocket().send(MessageBuilder.status().code(422).message("Parent node is no DOM node").build(), true);

				return;
			}

			Page page = getPage(pageId);
			if (page != null) {

				try {
					Widget.expandWidget(getWebSocket().getSecurityContext(), page, parentDOMNode, baseUrl, webSocketData.getNodeData(), processDeploymentInfo);

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
