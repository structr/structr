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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.web.entity.Widget;
import org.structr.web.entity.dom.Content;
import org.structr.web.entity.dom.DOMElement;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.web.traits.definitions.dom.DOMNodeTraitDefinition;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

import java.util.*;


public class WrapInWidgetCommand extends AbstractCommand {

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

			final DOMNode parentNode = nodeToReplace.getParent();
			if (parentNode != null) {

				final Map<String, Object> data = webSocketData.getNodeData();

				WrapInWidgetCommand.wrapInWidget(securityContext, page, nodeToReplace, baseUrl, data, processDeploymentInfo);

				TransactionCommand.registerNodeCallback(parentNode, callback);

			} else {

				// log error?
			}

			// send success
			getWebSocket().send(webSocketData, true);
		}
	}

	@Override
	public String getCommand() {
		return "WRAP_IN_WIDGET";
	}

	public static void wrapInWidget(final SecurityContext securityContext, final Page page, final DOMNode nodeToWrap, final String baseUrl, final Map<String, Object> data, final boolean processDeploymentInfo) throws FrameworkException {

		// create temporary parent for Widget to expand in
		final DOMNode tmpParent      = page.createElement("div");
		final DOMNode parentNode     = nodeToWrap.getParent();
		final String destinationSlot = (String) data.get("destinationSlot");

		// expand Widget
		Widget.expandWidget(page, tmpParent, baseUrl, data, processDeploymentInfo);

		// get Widget node (can only be a single node!)
		final DOMNode newNode = tmpParent.getFirstChild();

		// move nodeToWrap into newNode
		moveInto(securityContext, nodeToWrap, newNode, destinationSlot);

		// replace current node with new one
		parentNode.replaceChild(newNode, nodeToWrap);

		// remove temporary parent
		StructrApp.getInstance(securityContext).delete(tmpParent);
	}

	// ----- private methods -----
	private static void moveInto(final SecurityContext securityContext, final DOMNode nodeToWrap, final DOMNode newNode, final String slotArgument) throws FrameworkException{

		final List<DOMNode> children = collectChildren(newNode);
		final List<String> slots     = new LinkedList<>();
		String destinationSlot       = slotArgument;

		for (final DOMNode child : children) {

			final String slotName = child.getItemType();
			if (slotName != null) {

				slots.add(slotName);
			}
		}

		if (StringUtils.isBlank(destinationSlot) && slots.size() > 1) {

			throw new FrameworkException(422, "Cannot wrap element in widget, please specify destination slot (destinationSlot property) in " + newNode.getName());

		} else {

			destinationSlot = slots.get(0);
		}

		// find slots in new node
		for (final NodeInterface node : children) {

			if (node.is(StructrTraits.DOM_NODE)) {

				final DOMNode slotNode = node.as(DOMNode.class);
				final String itemType  = slotNode.getItemType();

				// if destinationSlot is null, there is only one child to append to
				if (destinationSlot == null || destinationSlot.equals(itemType)) {

					slotNode.appendChild(nodeToWrap);
				}
			}
		}
	}

	private static List<DOMNode> collectChildren(final DOMNode node) throws FrameworkException {

		final List<DOMNode> nodes = new LinkedList<>();

		nodes.add(node);

		for (final DOMNode child : node.getChildren()) {

			// don't collect nested components
			if (child.isComponentRoot()) {
				continue;
			}

			nodes.addAll(collectChildren(child));
		}

		return nodes;
	}

	public static void print(final DOMNode node, int depth) {

		System.out.println(StringUtils.repeat(" ", depth * 4) +  nameOrTag(node) + " (" + node.getUuid() + ")");

		for (final DOMNode child2 : node.getChildren()) {

			print(child2, depth + 1);
		}
	}

	public static String nameOrTag(final DOMNode node) {

		if (node.getName() != null) {
			return node.getName();
		}

		if (node.is(StructrTraits.DOM_ELEMENT)) {
			return node.as(DOMElement.class).getTag();
		}

		if (node.is(StructrTraits.CONTENT)) {
			final String content = node.as(Content.class).getContent();
			return "Content[" + content.substring(0, Math.min(content.length(), 6)) + "]";
		}

		return node.getUuid();
	}
}