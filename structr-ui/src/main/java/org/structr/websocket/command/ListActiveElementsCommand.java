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
import org.structr.core.GraphObjectMap;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.Tx;
import org.structr.web.entity.dom.Content;
import org.structr.web.entity.dom.DOMElement;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.web.entity.dom.relationship.DOMChildren;
import org.structr.web.entity.html.Input;
import org.structr.web.entity.html.Link;
import org.structr.web.entity.html.Script;

//~--- classes ----------------------------------------------------------------

/**
 * Websocket command to retrieve DOM nodes which are not attached to a parent element
 *
 * @author Axel Morgner
 */
public class ListActiveElementsCommand extends AbstractCommand {

	private static final Logger logger = Logger.getLogger(ListActiveElementsCommand.class.getName());

	static {

		StructrWebSocket.addCommand(ListActiveElementsCommand.class);

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

				collectActiveElements(result, page, null);

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
	private void collectActiveElements(final List<GraphObject> resultList, final DOMNode root, final String parentDataKey) {

		final String childDataKey = root.getProperty(DOMElement.dataKey);
		final String dataKey      = childDataKey != null ? childDataKey : parentDataKey;

		if (isActive(root, dataKey)) {
			resultList.add(root);
		}

		for (final DOMChildren children : root.getChildRelationships()) {

			final DOMNode child = children.getTargetNode();
			collectActiveElements(resultList, child, dataKey);
		}

	}

	private GraphObject extractActiveElement(final DOMNode node) {

		final GraphObjectMap activeElement = new GraphObjectMap();

		activeElement.put(GraphObject.id,     node.getUuid());
		activeElement.put(DOMElement.restQuery, "query");

		return activeElement;
	}

	private boolean isActive(final DOMNode node, final String dataKey) {

		if (node.getProperty(DOMElement.dataKey) != null) {
			return true;
		}

		if (node.getProperty(DOMElement.restQuery) != null) {
			return true;
		}

		if (node.getProperty(DOMElement.cypherQuery) != null) {
			return true;
		}

		if (node.getProperty(DOMElement.xpathQuery) != null) {
			return true;
		}

		if (node.getProperty(DOMNode.hideConditions) != null) {
			return true;
		}

		if (node.getProperty(DOMNode.showConditions) != null) {
			return true;
		}

		if (node.getProperty(DOMNode.hideOnIndex)) {
			return true;
		}

		if (node.getProperty(DOMNode.hideOnDetail)) {
			return true;
		}

		/*
		 attributes to check for !isEmpty:
		  - data-structr-action
		*/
		if (node.getProperty(DOMElement._action) != null) {
			return true;
		}

		if (node.getProperty(Content.content) != null && dataKey != null) {
			return node.getProperty(Content.content).contains(dataKey);
		}

		if (node.getProperty(DOMElement._id) != null && dataKey != null) {
			return node.getProperty(DOMElement._id).contains(dataKey);
		}

		if (node.getProperty(Link._href) != null && dataKey != null) {
			return node.getProperty(Link._href).contains(dataKey);
		}

		if (node.getProperty(Script._src) != null && dataKey != null) {
			return node.getProperty(Script._src).contains(dataKey);
		}

		if (node.getProperty(Input._value) != null && dataKey != null) {
			return node.getProperty(Input._value).contains(dataKey);
		}

		return false;
	}
}
