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
package org.structr.websocket.command.dom;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.web.entity.dom.Template;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.command.AbstractCommand;
import org.structr.websocket.command.CreateComponentCommand;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;
import org.w3c.dom.DOMException;

import java.util.Map;

/**
 * Clone a DOMNode to an arbitrary location in the same or another document (page).
 *
 *
 */
public class CloneNodeCommand extends AbstractCommand {

	static {
		StructrWebSocket.addCommand(CloneNodeCommand.class);
	}

	@Override
	public void processMessage(final WebSocketMessage webSocketData) {

		setDoTransactionNotifications(true);

		final String id                             = webSocketData.getId();
		final Map<String, Object> nodeData          = webSocketData.getNodeData();
		final String parentId                       = (String) nodeData.get("parentId");
		final boolean deep                          = (Boolean) nodeData.get("deep");
		final String refId                          = (String) nodeData.get("refId");
		final String relativePosition               = (String) nodeData.remove("relativePosition");
		final RelativePosition position;

		if (relativePosition != null) {

			try {

				position = RelativePosition.valueOf(relativePosition);

			} catch (final IllegalArgumentException iae) {

				// default to Before
				getWebSocket().send(MessageBuilder.status().code(422).message("Unsupported relative position: " + relativePosition).build(), true);
				return;
			}

		} else {

			position = RelativePosition.Before;
		}

		if (id != null) {

			DOMNode parent = null;

			final DOMNode node = getDOMNode(id);

			if (parentId != null) {

				// check if parent node with given ID exists
				parent = getDOMNode(parentId);

				if (parent == null) {

					getWebSocket().send(MessageBuilder.status().code(404).message("Parent node not found").build(), true);

					return;
				}
			}

			Page ownerPage = null;

			if (parent != null) {

				if (parent instanceof Page) {

					ownerPage = (Page)parent;

				} else {

					ownerPage = parent.getOwnerDocument();
				}

			} else {

				ownerPage = node.getOwnerDocument();
			}

			if (ownerPage == null) {

				getWebSocket().send(MessageBuilder.status().code(422).message("Cannot clone node without a target page").build(), true);
				return;
			}

			try {

				if (parent != null) {

					final boolean isShadowPage = ownerPage.equals(CreateComponentCommand.getOrCreateHiddenDocument());
					final boolean isTemplate   = (parent instanceof Template);

					if (isShadowPage && isTemplate && parent.getParent() == null) {

						getWebSocket().send(MessageBuilder.status().code(422).message("Appending children to root-level shared component Templates is not allowed").build(), true);
						return;
					}
				}

				final DOMNode clonedNode = (DOMNode) node.cloneNode(deep);
				final DOMNode refNode    = (refId != null) ? getDOMNode(refId) : null;

				if (parent != null) {

					final DOMNode nextSibling          = node.getNextSibling();
					final DOMNode sourceNodeParent     = node.getParent();
					final boolean cloneUnderSameParent = (sourceNodeParent != null) && parent.getUuid().equals(sourceNodeParent.getUuid());

					if (ownerPage.equals(node.getOwnerDocument()) && !parent.equals(nextSibling) && cloneUnderSameParent) {

						parent.insertBefore(clonedNode, nextSibling);

					} else if (refNode != null) {

						parent.insertBefore(clonedNode, refNode);

						if (RelativePosition.Before.equals(position)) {

							parent.insertBefore(clonedNode, refNode);

						} else {

							final DOMNode nextNode = refNode.getNextSibling();

							if (nextNode != null) {

								parent.insertBefore(clonedNode, nextNode);

							} else {

								parent.appendChild(clonedNode);
							}
						}

					} else {

						parent.appendChild(clonedNode);
					}
				}

				setOwnerPageRecursively(clonedNode, clonedNode.getSecurityContext(), ownerPage);

			} catch (DOMException | FrameworkException ex) {

				getWebSocket().send(MessageBuilder.status().code(422).message(ex.getMessage()).build(), true);
			}

		} else {

			getWebSocket().send(MessageBuilder.status().code(422).message("Cannot append node without id").build(), true);
		}
	}

	public void setOwnerPageRecursively(final DOMNode node, final SecurityContext securityContext, final Page page) throws FrameworkException {

		node.setOwnerDocument(page);

		for (final DOMNode child : node.getChildren()) {
			setOwnerPageRecursively(child, securityContext, page);
		}
	}

	@Override
	public String getCommand() {
		return "CLONE_NODE";
	}
}
