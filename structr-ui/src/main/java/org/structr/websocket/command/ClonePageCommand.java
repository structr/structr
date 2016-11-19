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

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractNode;
import org.structr.core.property.PropertyMap;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;
import org.w3c.dom.DOMException;

//~--- classes ----------------------------------------------------------------
/**
 * Websocket command to clone a page
 *
 *
 */
public class ClonePageCommand extends AbstractCommand {

	private static final Logger logger = LoggerFactory.getLogger(ClonePageCommand.class.getName());

	static {

		StructrWebSocket.addCommand(ClonePageCommand.class);

	}

	//~--- methods --------------------------------------------------------
	@Override
	public void processMessage(final WebSocketMessage webSocketData) {

		final SecurityContext securityContext = getWebSocket().getSecurityContext();

		// Node to clone
		String nodeId = webSocketData.getId();
		final AbstractNode nodeToClone = getNode(nodeId);
		final Map<String, Object> nodeData = webSocketData.getNodeData();
		final String newName;

		if (nodeData.containsKey(AbstractNode.name.dbName())) {

			newName = (String) nodeData.get(AbstractNode.name.dbName());
		} else {

			newName = "unknown";
		}

		if (nodeToClone != null) {

			try {
				final Page pageToClone = nodeToClone instanceof Page ? (Page) nodeToClone : null;

				if (pageToClone != null) {

					//final List<DOMNode> elements = pageToClone.getProperty(Page.elements);

					DOMNode firstChild = (DOMNode) pageToClone.getFirstChild().getNextSibling();

					if (firstChild == null) {
						firstChild = (DOMNode) pageToClone.treeGetFirstChild();
					}

					final DOMNode newHtmlNode = DOMNode.cloneAndAppendChildren(securityContext, firstChild);
					final Page newPage = (Page) pageToClone.cloneNode(false);
					newPage.setProperties(securityContext, new PropertyMap(Page.name, pageToClone.getProperty(Page.name) + "-" + newPage.getIdString()));

					newPage.appendChild(newHtmlNode);

				}

			} catch (FrameworkException fex) {

				logger.warn("Could not create node.", fex);
				getWebSocket().send(MessageBuilder.status().code(fex.getStatus()).message(fex.getMessage()).build(), true);

			} catch (DOMException dex) {

				logger.warn("Could not create node.", dex);
				getWebSocket().send(MessageBuilder.status().code(422).message(dex.getMessage()).build(), true);

			}

		} else {

			logger.warn("Node with uuid {} not found.", webSocketData.getId());
			getWebSocket().send(MessageBuilder.status().code(404).build(), true);

		}

	}

	//~--- get methods ----------------------------------------------------
	@Override
	public String getCommand() {

		return "CLONE_PAGE";

	}

}
