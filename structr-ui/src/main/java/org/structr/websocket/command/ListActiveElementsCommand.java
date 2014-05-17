/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
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
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.websocket.command;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.websocket.message.WebSocketMessage;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;

//~--- JDK imports ------------------------------------------------------------

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.structr.core.GraphObjectMap;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.Tx;
import org.structr.core.property.IntProperty;
import org.structr.core.property.Property;
import org.structr.core.property.StringProperty;
import org.structr.web.entity.dom.Content;
import org.structr.web.entity.dom.DOMElement;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.web.entity.dom.relationship.DOMChildren;
import org.structr.web.entity.html.Input;
import org.structr.web.entity.html.Link;

//~--- classes ----------------------------------------------------------------

/**
 * Websocket command to retrieve DOM nodes which are not attached to a parent element
 *
 * @author Axel Morgner
 */
public class ListActiveElementsCommand extends AbstractCommand {

	private static final Logger logger = Logger.getLogger(ListActiveElementsCommand.class.getName());
	private static final Property<Integer> recursionDepthProperty = new IntProperty("recursionDepth");
	private static final Property<String>  parentIdProperty       = new StringProperty("parentId");
	private static final Property<String>  stateProperty          = new StringProperty("state");
	private static final Property<String>  actionProperty         = new StringProperty("action");
	private static final Property<String>  queryProperty          = new StringProperty("query");

	static {

		StructrWebSocket.addCommand(ListActiveElementsCommand.class);

	}

	public enum ActiveElementState {
		None, Query, Content, Input, Button, Link
	}

	@Override
	public void processMessage(WebSocketMessage webSocketData) {

		final SecurityContext securityContext = getWebSocket().getSecurityContext();
		final App app                         = StructrApp.getInstance(securityContext);
		final String id                       = webSocketData.getId();

		try (final Tx tx = app.tx()) {

			final Page page                = app.get(Page.class, id);
			final List<GraphObject> result = new LinkedList<>();

			if (page != null) {

				collectActiveElements(result, page, Collections.EMPTY_SET, null, 0);

				// set full result list
				webSocketData.setResult(result);
				webSocketData.setRawResultCount(result.size());

				// send only over local connection
				getWebSocket().send(webSocketData, true);

			} else {

				getWebSocket().send(MessageBuilder.status().code(404).message("Page with ID " + id + " not found.").build(), true);
			}

		} catch (FrameworkException fex) {

			logger.log(Level.WARNING, "Exception occured", fex);
			getWebSocket().send(MessageBuilder.status().code(fex.getStatus()).message(fex.getMessage()).build(), true);
		}
	}

	@Override
	public String getCommand() {

		return "LIST_ACTIVE_ELEMENTS";

	}

	// ----- private methods -----
	private void collectActiveElements(final List<GraphObject> resultList, final DOMNode root, final Set<String> parentDataKeys, final String parent, final int depth) {

		final String childDataKey  = root.getProperty(DOMElement.dataKey);
		final Set<String> dataKeys = new LinkedHashSet<>(parentDataKeys);
		String parentId            = parent;
		int dataCentricDepth       = depth;

		if (!StringUtils.isEmpty(childDataKey)) {

			dataKeys.add(childDataKey);
			dataCentricDepth++;
		}

		final ActiveElementState state = isActive(root, dataKeys);
		if (!state.equals(ActiveElementState.None)) {

			resultList.add(extractActiveElement(root, dataKeys, parentId, state, depth));
			if (state.equals(ActiveElementState.Query)) {

				parentId = root.getUuid();
			}
		}

		for (final DOMChildren children : root.getChildRelationships()) {

			final DOMNode child = children.getTargetNode();
			collectActiveElements(resultList, child, dataKeys, parentId, dataCentricDepth);
		}

	}

	private GraphObject extractActiveElement(final DOMNode node, final Set<String> dataKeys, final String parentId, final ActiveElementState state, final int depth) {

		final GraphObjectMap activeElement = new GraphObjectMap();

		activeElement.put(GraphObject.id, node.getUuid());
		activeElement.put(GraphObject.type, node.getType());
		activeElement.put(DOMElement.dataKey, StringUtils.join(dataKeys, ","));
		activeElement.put(Content.content, node.getProperty(Content.content));

		switch (state) {

			case Button:
				activeElement.put(actionProperty, node.getProperty(DOMElement._action));
				break;

			case Link:
				activeElement.put(actionProperty, node.getProperty(Link._href));
				break;

			case Query:
				extractQueries(activeElement, node);
				break;

		}

		activeElement.put(stateProperty, state.name());
		activeElement.put(recursionDepthProperty, depth);
		activeElement.put(parentIdProperty, parentId);

		return activeElement;
	}

	private ActiveElementState isActive(final DOMNode node, final Set<String> dataKeys) {

		if (!StringUtils.isEmpty(node.getProperty(DOMElement.dataKey))) {
			return ActiveElementState.Query;
		}

		if (!StringUtils.isEmpty(node.getProperty(DOMElement.restQuery))) {
			return ActiveElementState.Query;
		}

		if (!StringUtils.isEmpty(node.getProperty(DOMElement.cypherQuery))) {
			return ActiveElementState.Query;
		}

		if (!StringUtils.isEmpty(node.getProperty(DOMElement.xpathQuery))) {
			return ActiveElementState.Query;
		}

		/*
		 attributes to check for !isEmpty:
		  - data-structr-action
		  - data-structr-attributes
		  - data-structr-raw-value
		*/
		if (node.getProperty(DOMElement._action) != null || node.getProperty(DOMElement._attributes) != null || node.getProperty(DOMElement._rawValue) != null) {
			return ActiveElementState.Button;
		}

		if (node.getProperty(Content.content) != null && !dataKeys.isEmpty()) {

			if (containsDataKeyReference(node.getProperty(Content.content), dataKeys)) {
				return ActiveElementState.Content;
			}
		}

		if (node.getProperty(DOMElement._id) != null && !dataKeys.isEmpty()) {

			if (containsDataKeyReference(node.getProperty(DOMElement._id), dataKeys)) {
				return ActiveElementState.Content;
			}
		}

		if (node.getProperty(Link._href) != null && !dataKeys.isEmpty()) {

			if (containsDataKeyReference(node.getProperty(Link._href), dataKeys)) {
				return ActiveElementState.Link;
			}
		}

		if (node.getProperty(Input._value) != null && !dataKeys.isEmpty()) {

			if (containsDataKeyReference(node.getProperty(Input._value), dataKeys)) {

				return ActiveElementState.Input;
			}
		}

		// last option: just some hidden element..
		if (!StringUtils.isEmpty(node.getProperty(DOMNode.hideConditions))) {
			return ActiveElementState.Content;
		}

		if (!StringUtils.isEmpty(node.getProperty(DOMNode.showConditions))) {
			return ActiveElementState.Content;
		}

		if (node.getProperty(DOMNode.hideOnIndex)) {
			return ActiveElementState.Content;
		}

		if (node.getProperty(DOMNode.hideOnDetail)) {
			return ActiveElementState.Content;
		}

		return ActiveElementState.None;
	}

	private boolean containsDataKeyReference(final String value, final Set<String> dataKeys) {

		boolean contains = false;

		for (final String dataKey : dataKeys) {

			contains |= value.contains(dataKey);
		}

		return contains;
	}

	private void extractQueries(final GraphObjectMap activeElement, final DOMNode node) {

		if (extractQuery(activeElement, node, DOMElement.restQuery)) {
			return;
		}

		if (extractQuery(activeElement, node, DOMElement.cypherQuery)) {
			return;
		}

		if (extractQuery(activeElement, node, DOMElement.xpathQuery)) {
			return;
		}
	}

	private boolean extractQuery(final GraphObjectMap activeElement, final DOMNode node, final Property<String> queryKey) {

		if (!StringUtils.isEmpty(node.getProperty(queryKey))) {

			activeElement.put(queryProperty, node.getProperty(queryKey));
			return true;
		}

		return false;
	}
}
