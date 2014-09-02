/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * Structr is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr. If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.websocket.command;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

//~--- classes ----------------------------------------------------------------
/**
 * Websocket command to clone a page
 *
 * @author Axel Morgner
 */
public class ClonePageCommand extends AbstractCommand {

	private static final Logger logger = Logger.getLogger(ClonePageCommand.class.getName());

	static {

		StructrWebSocket.addCommand(ClonePageCommand.class);

	}

	//~--- methods --------------------------------------------------------
	@Override
	public void processMessage(WebSocketMessage webSocketData) {

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

		final App app = StructrApp.getInstance(securityContext);

		if (nodeToClone != null) {

			try {
				final Page pageToClone = nodeToClone instanceof Page ? (Page) nodeToClone : null;

				if (pageToClone != null) {

					//final List<DOMNode> elements = pageToClone.getProperty(Page.elements);
					
					DOMNode firstChild = (DOMNode) pageToClone.getFirstChild().getNextSibling();
					
					if (firstChild == null) {
						firstChild = (DOMNode) pageToClone.treeGetFirstChild();
					}
					
					final DOMNode newHtmlNode = cloneAndAppendChildren(securityContext, firstChild);
					final Page newPage = Page.createNewPage(securityContext, newName);
					
					newPage.appendChild(newHtmlNode);

				}

			} catch (FrameworkException fex) {

				logger.log(Level.WARNING, "Could not create node.", fex);
				getWebSocket().send(MessageBuilder.status().code(fex.getStatus()).message(fex.getMessage()).build(), true);

			}

		} else {

			logger.log(Level.WARNING, "Node with uuid {0} not found.", webSocketData.getId());
			getWebSocket().send(MessageBuilder.status().code(404).build(), true);

		}

	}

	/**
	 * Recursively clone given node, all its direct children and connect the cloned
	 * child nodes to the clone parent node.
	 *
	 * @param nodeToClone
	 */
	private DOMNode cloneAndAppendChildren(final SecurityContext securityContext, final DOMNode nodeToClone) {

		final App app = StructrApp.getInstance(securityContext);

		final DOMNode newNode = (DOMNode) nodeToClone.cloneNode(false);

		final List<DOMNode> childrenToClone = (List<DOMNode>) nodeToClone.getChildNodes();

		for (final DOMNode childNodeToClone : childrenToClone) {

			final DOMNode newChildNode = (DOMNode) cloneAndAppendChildren(securityContext, childNodeToClone);
			newNode.appendChild(newChildNode);

		}

		return newNode;
	}

	//~--- get methods ----------------------------------------------------
	@Override
	public String getCommand() {

		return "CLONE_PAGE";

	}

}
