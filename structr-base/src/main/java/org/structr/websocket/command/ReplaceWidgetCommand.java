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

public class ReplaceWidgetCommand extends AbstractCommand {

	private static final Logger logger     = LoggerFactory.getLogger(ReplaceWidgetCommand.class.getName());

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
				final DOMNode newRoot = ReplaceWidgetCommand.replaceWidget(securityContext, page, nodeToReplace, baseUrl, data, processDeploymentInfo);

				// Inherit visibility flags from the replacement's parent onto
				// the widget root and its descendants. Runs for every replace
				// so the new subtree picks up the page's audience the same
				// way a manually-created DOMNode does.
				WidgetVisibilityFlagInheritor.apply(newRoot, parentNode);

				// Attach auto-VM (no-op if no spec was sent or trait is missing).
				WidgetAutoVisibilityMappingHelper.applyAndConsume(securityContext, newRoot, data);

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

		return "REPLACE_WIDGET";
	}

	public static DOMNode replaceWidget(final SecurityContext securityContext, final Page page, final DOMNode nodeToReplace, final String baseUrl, final Map<String, Object> data, final boolean processDeploymentInfo) throws FrameworkException {

		// create temporary parent for Widget to expand in
		final DOMNode tmpParent  = page.createElement("div");
		final DOMNode parentNode = nodeToReplace.getParent();

		// expand Widget
		Widget.expandWidget(page, tmpParent, baseUrl, data, processDeploymentInfo);

		// get Widget node (can only be a single node!)
		final DOMNode newNode = tmpParent.getFirstChild();

		// move content and children to new node
		moveContent(securityContext, nodeToReplace, newNode);

		// replace current node with new one
		parentNode.replaceChild(newNode, nodeToReplace);

		// remove temporary parent
		StructrApp.getInstance(securityContext).delete(tmpParent);

		return newNode;
	}

	// ----- private methods -----
	private static void moveContent(final SecurityContext securityContext, final DOMNode oldNode, final DOMNode newNode) throws FrameworkException{

		final Map<String, ItemData> items           = new LinkedHashMap<>();
		final Map<String, RepeaterData> repeaters   = new LinkedHashMap<>();
		final Set<String> idsOfOldNodesWithItemType = new LinkedHashSet<>();
		final String sourceName                     = oldNode.getName();
		final String targetName                     = newNode.getName();

		// collect slot children from old node
		for (final NodeInterface node : collectChildren(oldNode)) {

			if (node.is(StructrTraits.DOM_NODE)) {

				final DOMNode slotNode = node.as(DOMNode.class);

				// itemType (=> children)
				final String itemType = getItemType(slotNode);
				if (itemType != null) {

					System.out.println("Storing slot data for itemType " + itemType + " in " + nameOrTag(slotNode) + " (uuid: " + slotNode.getUuid() + ")");

					idsOfOldNodesWithItemType.add(slotNode.getUuid());

					final ItemData slotData = new ItemData(slotNode, itemType);
					if (items.put(itemType, slotData) != null) {

						throw new FrameworkException(422, "Content slot " + itemType + " exists more than once in " + nameOrTag(slotNode));
					}
				}

				// repeaterType (=> queries)
				final String repeaterType = getRepeaterType(slotNode);
				if (repeaterType != null) {

					final RepeaterData slotData = new RepeaterData(slotNode, repeaterType);
					if (repeaters.put(repeaterType, slotData) != null) {

						throw new FrameworkException(422, "Repeater slot " + itemType + " exists more than once in " + slotNode);
					}
				}
			}
		}

		// find slots in new node
		for (final NodeInterface node : collectChildren(newNode)) {

			if (node.is(StructrTraits.DOM_NODE)) {

				final DOMNode slotNode          = node.as(DOMNode.class);
				final String itemType           = getItemType(slotNode);
				final String repeaterType       = getRepeaterType(slotNode);
				final ItemData itemData         = items.get(itemType);
				final RepeaterData repeaterData = repeaters.get(repeaterType);

				if (itemData != null) {

					// move node to new slot
					itemData.applyTo(securityContext, slotNode);
				}

				if (repeaterData != null) {

					// move node to new slot
					repeaterData.applyTo(securityContext, slotNode);
				}
			}
		}

		final App app = StructrApp.getInstance(securityContext);

		// remove children with itemType from new node that were present on the old node
		for (final NodeInterface child : newNode.getAllChildNodes()) {

			if (idsOfOldNodesWithItemType.contains(child.getUuid())) {

				app.delete(child);
			}
		}

		final List<String> buffer = new LinkedList<>();

		for (final ItemData slotData : items.values()) {

			if (!slotData.wasProcessed()) {

				buffer.add("\"" + targetName + "\" has no slot for itemType \"" + slotData.getIdentifier() + "\"");
			}
		}

		for (final RepeaterData repeaterData : repeaters.values()) {

			if (!repeaterData.wasProcessed()) {

				buffer.add("\"" + targetName + "\" has no slot for repeaterType \"" + repeaterData.getIdentifier() + "\"");
			}
		}

		if (!buffer.isEmpty()) {

			throw new FrameworkException(422, "Widgets are not compatible: cannot replace \"" + sourceName + "\" with \"" + targetName + "\" because " + StringUtils.join(buffer, ", ") + ".");
		}
	}

	private static String getItemType(final DOMNode node) {

		if (node.getSharedComponent() != null) {

			return node.getSharedComponent().getItemType();
		}

		return node.getItemType();
	}

	private static String getRepeaterType(final DOMNode node) {

		if (node.getSharedComponent() != null) {

			return node.getSharedComponent().getRepeaterType();
		}

		return node.getRepeaterType();
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

	private static String format(final DOMNode node) {

		return node.getType() + "(" + nameOrTag(node) + ")";
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

	private static class ItemData extends SlotData {

		private final List<DOMNode> children = new LinkedList<>();

		public ItemData(final DOMNode node, final String identifier) throws FrameworkException {

			super(identifier);

			for (final DOMNode child : node.getChildren()) {

				this.children.add(child);
			}
		}

		public void applyTo(final SecurityContext securityContext, final DOMNode newNode) throws FrameworkException {

			final App app = StructrApp.getInstance(securityContext);

			// remove template slot content (will be replaced with children from source node)
			for (final DOMNode child : newNode.getChildren()) {

				if (child.getItemType() == null) {

					app.delete(child);
				}
			}

			// move slot content from source to destination
			for (final DOMNode child : this.children) {

				newNode.appendChild(child);
			}

			processed = true;
		}
	}

	private static class RepeaterData extends SlotData {

		private final Set<String> keys       = Set.of(DOMNodeTraitDefinition.DATA_KEY_PROPERTY, DOMNodeTraitDefinition.FUNCTION_QUERY_PROPERTY);
		private final PropertyMap properties = new PropertyMap();

		public RepeaterData(final DOMNode node, final String identifier) throws FrameworkException {

			super(identifier);

			final Traits traits = node.getTraits();

			for (final String keyName : keys) {

				final PropertyKey key = traits.key(keyName);
				properties.put(key, node.getProperty(key));
			}
		}

		public void applyTo(final SecurityContext securityContext, final DOMNode newNode) throws FrameworkException {

			 newNode.setProperties(securityContext, properties);
			processed = true;
		}
	}

	private abstract static class SlotData {

		protected final String identifier;
		protected boolean processed = false;

		public SlotData(final String identifier) {

			this.identifier = identifier;
		}

		public String getIdentifier() {

			return identifier;
		}

		public boolean wasProcessed() {

			return processed;
		}
	}
}