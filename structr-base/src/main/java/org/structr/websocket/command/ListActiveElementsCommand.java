/*
 * Copyright (C) 2010-2025 Structr GmbH
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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.util.Iterables;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.GraphObjectMap;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.function.Functions;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.StringProperty;
import org.structr.web.entity.dom.DOMElement;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Template;
import org.structr.web.entity.event.ActionMapping;
import org.structr.web.entity.event.ParameterMapping;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.WebSocketMessage;

import java.util.*;

/**
 * Websocket command to retrieve DOM nodes which are not attached to a parent element.
 */
public class ListActiveElementsCommand extends AbstractCommand {

	private static final Logger logger = LoggerFactory.getLogger(ListActiveElementsCommand.class.getName());

	static {

		StructrWebSocket.addCommand(ListActiveElementsCommand.class);
	}

	@Override
	public void processMessage(final WebSocketMessage webSocketData) {

		setDoTransactionNotifications(false);

		final SecurityContext securityContext = getWebSocket().getSecurityContext();
		final App app                         = StructrApp.getInstance(securityContext);

		try (final Tx tx = app.tx()) {

			final String id                = webSocketData.getId();
			final List<GraphObject> result = getResult(getNode(id));

			webSocketData.setResult(result);
			webSocketData.setRawResultCount(result.size());

			// send only over local connection
			getWebSocket().send(webSocketData, true);

			tx.success();

		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	@Override
	public String getCommand() {
		return "LIST_ACTIVE_ELEMENTS";
	}

	// ----- private methods -----
	private List<GraphObject> getResult(final NodeInterface page) throws FrameworkException {

		final Map<String, Object> activeElements = getActiveElements(page.as(DOMNode.class));
		final GraphObjectMap result              = new GraphObjectMap();
		final Gson gson                          = new GsonBuilder().create();

		result.put(new StringProperty("json"), gson.toJson(activeElements));

		return List.of(result);
	}

	private Map<String, Object> getActiveElements(final DOMNode page) throws FrameworkException {

		final Map<String, Object> activeElements     = new LinkedHashMap<>();
		final Map<String, Object> nodeData           = new LinkedHashMap<>();
		final List<Map<String, Object>> nodes        = new LinkedList<>();
		final List<Map<String, Object>> edges        = new LinkedList<>();
		final Map<String, Map<String, Object>> index = new LinkedHashMap<>();

		activeElements.put("nodeData", nodeData);
		activeElements.put("children", nodes);
		activeElements.put("edges",    edges);
		activeElements.put("id",       "root");

		// Always add Page element
		addIfNotPresent(nodes, createNodeFromNodeInterface(nodeData, index, page));

		// everything is on the top level
		addElements(nodeData, index, nodes, edges, page, page, nodes, edges);

		return activeElements;
	}

	private void addElements(final Map<String, Object> nodeData, final Map<String, Map<String, Object>> index, final List<Map<String, Object>> nodes, final List<Map<String, Object>> edges, final DOMNode current, final DOMNode lastParent, final List<Map<String, Object>> pageChildren, final List<Map<String, Object>> pageEdges) {

		boolean included = isActiveNode(current);

		if (current instanceof Template && current.getName() != null && hasDirectActiveDescendant(current)) {
			included = true;
		}

		if (current.is("Table") && hasActiveDescendant(current)) {
			included = true;
		}

		if (included) {

			final Map<String, Object> currentNode = createNodeFromNodeInterface(nodeData, index, current);
			final boolean addNestedChildren       = false;

			addIfNotPresent(pageChildren, currentNode);
			addAndLinkDataSourceIfPresent(index, nodes, edges, current);

			if (current.is("DOMElement")) {
				addAndLinkActionMappingIfPresent(nodeData, index, nodes, edges, current.as(DOMElement.class));
			}

			// conditions add an intermediate node so we don't have to connect them here..
			if (!addAndLinkConditionsIfPresent(nodeData, index, pageChildren, pageEdges, current, lastParent)) {

				if (lastParent != null && !addNestedChildren) {
					addEdge(edges, lastParent, current, Map.of());
				}
			}

			if (addNestedChildren) {

				final List<Map<String, Object>> nestedChildren = (List)currentNode.get("children");
				final List<Map<String, Object>> nestedEdges    = (List)currentNode.get("edges");

				for (final DOMNode child : current.getChildren()) {

					addElements(nodeData, index, nodes, edges, child, current, nestedChildren, nestedEdges);
				}

			} else {

				for (final DOMNode child : current.getChildren()) {

					addElements(nodeData, index, nodes, edges, child, current, pageChildren, pageEdges);
				}
			}

		} else {

			for (final DOMNode child : current.getChildren()) {

				addElements(nodeData, index, nodes, edges, child, lastParent, pageChildren, pageEdges);
			}
		}
	}

	private Map<String, Object> createNodeFromNodeInterface(final Map<String, Object> nodeData, final Map<String, Map<String, Object>> index, final NodeInterface node) {

		final String id = node.getUuid();

		// add returns true if the set did NOT already contain the value
		if (!index.containsKey(id)) {

			final Map<String, Object> map = new LinkedHashMap<>();
			final String name             = getNameOrText(node);
			final String type             = node.getType();

			map.put("id",       id);
			map.put("width",    getWidthForType(type, name));
			map.put("height",   getHeightForType(type, name));
			map.put("children", new LinkedList<>());
			map.put("edges",    new LinkedList<>());

			if (node.is("DOMElement")) {

				final DOMElement elem = node.as(DOMElement.class);

				final String tag  = "<" + elem.getTag() + "/>";

				map.put("labels", List.of(
					Map.of("text", (name != null ? name : tag)),
					Map.of("text", type)
				));

			} else {

				map.put("labels", List.of(
					Map.of("text", (name != null ? name : type)),
					Map.of("text", type)
				));
			}

			index.put(id, map);

			if (node.is("DOMNode")) {

				final DOMNode domNode            = node.as(DOMNode.class);
				final Map<String, Object> values = new LinkedHashMap<>();

				nodeData.put(id, values);

				values.put("id", id);
				values.put("type", domNode.getType());
				values.put("name", domNode.getName());
				values.put("dataKey", domNode.getDataKey());
			}
		}

		return index.get(id);
	}

	private boolean contains(final List<Map<String, Object>> list, final Map<String, Object> node) {

		final String id = (String)node.get("id");

		return list.stream().anyMatch(p -> id.equals(p.get("id")));
	}

	private boolean addIfNotPresent(final List<Map<String, Object>> list, final Map<String, Object> node) {

		if (!contains(list, node)) {

			list.add(node);
			return true;
		}

		return false;
	}

	private Map<String, Object> createNodeFromMap(final Map<String, Map<String, Object>> index, final String id, final String name, final String type) {

		// add returns true if the set did NOT already contain the value
		if (!index.containsKey(id)) {

			final Map<String, Object> map = new LinkedHashMap<>();

			map.put("id",       id);
			map.put("width",    getWidthForType(type, name));
			map.put("height",   getHeightForType(type, name));
			map.put("children", new LinkedList<>());

			map.put("labels", List.of(
				Map.of("text", (name != null ? name : type)),
				Map.of("text", type)
			));

			index.put(id, map);
		}

		return index.get(id);
	}

	private int getWidthForType(final String type, final String name) {

		if ("PropertyDataSource".equals(type) || "ListDataSource".equals(type)) {
			return 40;
		}

		if ("Condition".equals(type)) {
			return 40;
		}

		if ("Page".equals(type)) {
			return 120;
		}

		if (name != null && name.length() > 30) {
			return 360;
		}

		if (name != null && name.length() > 10) {
			return 240;
		}

		switch (type) {

			case "Template"           -> { return 120; }
			case "ActionMapping"      -> { return 120; }
			case "Table"              -> { return  80; }
			default                   -> { return  70; }
		}
	}

	private int getHeightForType(final String type, final String name) {

		if ("PropertyDataSource".equals(type) || "ListDataSource".equals(type)) {
			return 40;
		}

		if ("Condition".equals(type)) {
			return 40;
		}

		if ("Page".equals(type)) {
			return 180;
		}

		if (!"ActionMapping".equals(type) && name != null && name.length() > 10) {
			return 80;
		}

		switch (type) {

			case "Table"              -> { return 100; }
			case "Template"           -> { return  80; }
			case "ActionMapping"      -> { return  40; }
			default                   -> { return  20; }
		}
	}

	private void addEdge(final List<Map<String, Object>> edges, final NodeInterface source, final NodeInterface target, final Map<String, Object> data) {
		addEdge(edges, source.getUuid(), target.getUuid(), data);
	}

	private void addEdge(final List<Map<String, Object>> edges, final String source, final String target, final Map<String, Object> data) {

		if (!source.equals(target)) {

			final Map<String, Object> map = new LinkedHashMap<>();

			map.put("id",      source + "-" + target);
			map.put("sources", List.of(source));
			map.put("targets", List.of(target));

			map.putAll(data);

			addIfNotPresent(edges, map);
		}
	}

	private DOMNode findCommonParent(final DOMNode node1, final DOMNode node2) {

		// nodes are the same
		if (node1.getUuid().equals(node2.getUuid())) {
			return null;
		}

		final List<DOMNode> ancestors1 = getAncestors(node1);
		final List<DOMNode> ancestors2 = getAncestors(node2);

		final int depth1               = ancestors1.size();
		final int depth2               = ancestors2.size();

		ancestors1.retainAll(ancestors2);

		if (ancestors1.isEmpty()) {
			return null;
		}

		System.out.println(depth1 + ", " + depth2 + ": " + ancestors1.size());

		// return last element
		return ancestors1.get(ancestors1.size() - 1);

	}

	private List<DOMNode> getAncestors(final DOMNode node) {

		final List<DOMNode> ancestors = new LinkedList<>();

		DOMNode current = node.getParent();

		while (current != null) {

			ancestors.add(0, current);
			current = current.getParent();
		}

		return ancestors;
	}

	private boolean hasActiveDescendant(final DOMNode node) {

		for (final NodeInterface child : node.getAllChildNodes()) {

			if (isActiveNode(child)) {
				return true;
			}
		}

		return false;
	}

	private boolean hasDirectActiveDescendant(final DOMNode node) {

		for (final DOMNode child : node.getChildren()) {

			if (isActiveNode(child)) {
				return true;
			}
		}

		return false;
	}

	private boolean isLowestNamedContainerInHierarchy(final DOMNode node) {

		// no name => not a named container
		if (node.getName() == null) {
			return false;
		}

		for (final DOMNode child : node.getChildren()) {

			if (isLowestNamedContainerInHierarchy(child)) {
				return false;
			}
		}

		return true;
	}

	private boolean isActiveNode(final NodeInterface n) {

		return true;

		/*

		final DOMNode node = n.as(DOMNode.class);

		if (node.getDataKey() != null) {
			return true;
		}

		if (node.getName() != null && hasActiveDescendant(node)) {
			return true;
		}

		if (node.is("DOMElement")) {

			final List<ActionMapping> actions = Iterables.toList(node.as(DOMElement.class).getTriggeredActions());
			if (!actions.isEmpty()) {

				return true;
			}
		}

		/*
		final DOMDataSource dataSource = node.getProperty(StructrApp.key(DOMNode.class, "dataSource"));
		if (dataSource != null) {

			return true;
		}
		*/

		/*

		if (node.getShowConditions() != null) {

			return true;
		}

		if (node.getHideConditions() != null) {

			return true;
		}

		return false;
		*/
	}

	private void addAndLinkDataSourceIfPresent(final Map<String, Map<String, Object>> index, final List<Map<String, Object>> nodes, final List<Map<String, Object>> edges, final DOMNode node) {

		/*
		final DOMDataSource dataSource = node.getProperty(StructrApp.key(DOMNode.class, "dataSource"));
		if (dataSource != null) {

			addIfNotPresent(nodes, createNodeFromNodeInterface(index, dataSource));

			addEdge(edges, dataSource, node, Map.of());

		} else */

		if (node.getFunctionQuery() != null) {

			final String id = "repeater_" + Functions.cleanString(node.getFunctionQuery());

			addIfNotPresent(nodes, createNodeFromMap(index, id, "FunctionQuery", "ListDataSource"));

			addEdge(edges, id, node.getUuid(), Map.of());
		}
	}

	private void addAndLinkSharedComponentIfPresent(final Map<String, Object> nodeData, final Map<String, Map<String, Object>> index, final List<Map<String, Object>> nodes, final List<Map<String, Object>> edges, final DOMNode node) {

		final DOMNode sharedComponent = node.getSharedComponent();
		if (sharedComponent != null) {

			addIfNotPresent(nodes, createNodeFromNodeInterface(nodeData, index, sharedComponent));

			addEdge(edges, node, sharedComponent, Map.of());
		}
	}

	private void addAndLinkActionMappingIfPresent(final Map<String, Object> nodeData, final Map<String, Map<String, Object>> index, final List<Map<String, Object>> nodes, final List<Map<String, Object>> edges, final DOMElement node) {

		for (final ActionMapping action : node.getTriggeredActions()) {

			addIfNotPresent(nodes, createNodeFromNodeInterface(nodeData, index, action));

			addEdge(edges, node, action, Map.of());
		}
	}

	private boolean addAndLinkConditionsIfPresent(final Map<String, Object> nodeData, final Map<String, Map<String, Object>> index, final List<Map<String, Object>> nodes, final List<Map<String, Object>> edges, final DOMNode node, final DOMNode lastParent) {

		if (node.getShowConditions() != null) {

			final DOMNode parent     = node.getParent();
			final String parentId    = parent.getUuid();
			final String conditionId = parentId + "_condition";

			addIfNotPresent(nodes, createNodeFromNodeInterface(nodeData, index, parent));
			addIfNotPresent(nodes, createNodeFromMap(index, conditionId, null, "Condition"));

			if (lastParent != null) {
				addEdge(edges, lastParent, parent, Map.of());
			}

			addEdge(edges, parentId, conditionId, Map.of());
			addEdge(edges, conditionId, node.getUuid(), Map.of());

			return true;
		}

		if (node.getHideConditions() != null) {

			final DOMNode parent     = node.getParent();
			final String parentId    = parent.getUuid();
			final String conditionId = parentId + "_condition";

			addIfNotPresent(nodes, createNodeFromNodeInterface(nodeData, index, parent));
			addIfNotPresent(nodes, createNodeFromMap(index, conditionId, null, "Condition"));

			if (lastParent != null) {
				addEdge(edges, lastParent, parent, Map.of());
			}

			addEdge(edges, parentId, conditionId, Map.of());
			addEdge(edges, conditionId, node.getUuid(), Map.of());

			return true;
		}

		return false;
	}

	private String getNameOrText(final NodeInterface node) {

		if (node.is("ActionMapping")) {

			final ActionMapping actionMapping = node.as(ActionMapping.class);
			final String action               = actionMapping.getAction();

			if ("method".equals(action)) {

				return actionMapping.getMethod();
			}

			if ("create".equals(action)) {

				return action + ": " + actionMapping.getDataType();
			}

			if ("delete".equals(action)) {

				return action + ": " + actionMapping.getIdExpression();
			}

			final List<DOMElement> triggerElements = Iterables.toList(actionMapping.getTriggerElements());
			if (!triggerElements.isEmpty()) {

				final DOMElement triggerElement = triggerElements.get(0);

				if (Set.of("input", "select").contains(triggerElement.getTag()) && StringUtils.isNotBlank(triggerElement.getHtmlName())) {

					return action + ": " + triggerElement.getHtmlName();

				} else if ("button".equals(triggerElement.getTag())) {

					final List<ParameterMapping> parameterMappings = Iterables.toList(actionMapping.getParameterMappings());
					if (!parameterMappings.isEmpty()) {

						final ParameterMapping param = parameterMappings.get(0);

						if (StringUtils.isNotBlank(param.getParameterName())) {

							return action + ": " + param.getParameterName();
						}
					}
				}
			}

			return action;
		}

		return node.getName();
	}
}
