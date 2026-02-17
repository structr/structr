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
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.util.ResultStream;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.StructrTraits;
import org.structr.web.entity.Widget;
import org.structr.web.entity.dom.DOMElement;
import org.structr.web.entity.dom.DOMNode;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/**
 * Websocket command to load suggestions for a given HTML element.
 */
public class GetSuggestionsCommand extends AbstractCommand {

	private static final Logger logger = LoggerFactory.getLogger(GetSuggestionsCommand.class.getName());

	static {

		StructrWebSocket.addCommand(GetSuggestionsCommand.class);

	}

	@Override
	public void processMessage(final WebSocketMessage webSocketData) {

		setDoTransactionNotifications(false);

		final SecurityContext securityContext = getWebSocket().getSecurityContext();
		final String mode                     = webSocketData.getNodeDataStringValueTrimmedOrDefault("mode", "insert");

		final String id = webSocketData.getId();
		if (id != null) {

			final DOMNode domNode = getDOMNode(id);
			if (domNode != null) {

				try {
					final List<GraphObject> result = new LinkedList<>();

					switch (mode) {

						case "insert":
							result.addAll(getWidgetsForInsert(securityContext, domNode));
							break;

						case "replace":
							result.addAll(getWidgetsForReplace(securityContext, domNode));
					}

					webSocketData.setResult(result);

				} catch (Throwable t) {

					logger.error("", t);
					getWebSocket().send(MessageBuilder.status().code(422).build(), true);

					return;
				}
			}
		}

		getWebSocket().send(webSocketData, true);
	}

	@Override
	public String getCommand() {
		return "GET_SUGGESTIONS";
	}

	// ----- private methods -----
	private List<NodeInterface> getWidgetsForInsert(final SecurityContext securityContext, final DOMNode domNode) throws FrameworkException {

		final List<NodeInterface> result = new LinkedList<>();
		final List<String> classes       = splitClasses(domNode.getCssClass());
		final String name                = domNode.getName();
		final String htmlId              = getHtmlId(domNode);
		final String tag                 = getTag(domNode);
		final App app                    = StructrApp.getInstance(securityContext);

		if (tag != null) {

			final String dataTypeAttribute = getComponentType(domNode);
			final Element element = new Element(tag);

			for (final String css : classes) {
				element.addClass(css);
			}

			if (domNode.hasSharedComponent()) {

				element.attr("type", domNode.getSharedComponent().getComponentType());

			} else if (dataTypeAttribute != null) {

				element.attr("type", dataTypeAttribute);
			}

			if (name != null) {
				element.attr("name", name);
			}

			if (htmlId != null) {
				element.attr("id", htmlId);
			}

			try (final ResultStream<NodeInterface> resultStream = app.nodeQuery(StructrTraits.WIDGET).getResultStream()) {

				for (final NodeInterface node : resultStream) {

					final Widget widget      = node.as(Widget.class);
					final String[] selectors = widget.getSelectors();

					if (selectors != null) {

						for (final String selector : selectors) {

							if (element.select(selector).first() != null) {

								// skip exclusive widgets (only one in each parent allowed)
								if (widget.isExclusiveInParent() && alreadyPresent(domNode, widget)) {
									continue;
								}

								result.add(widget);
								break;
							}
						}
					}
				}
			}

			// sort result by name
			Collections.sort(result, Comparator.comparing(NodeInterface::getName));
		}

		return result;
	}

	private List<NodeInterface> getWidgetsForReplace(final SecurityContext securityContext, final DOMNode domNode) throws FrameworkException {

		// We can only replace widgets with a matching data-type
		final App app                    = StructrApp.getInstance(securityContext);
		final List<NodeInterface> result = new LinkedList<>();
		final String nodeType            = getComponentType(domNode);
		final Integer dimensions         = getDimensions(domNode);

		try (final ResultStream<NodeInterface> resultStream = app.nodeQuery(StructrTraits.WIDGET).getResultStream()) {

			for (final NodeInterface node : resultStream) {

				final Widget widget            = node.as(Widget.class);
				final String widgetType        = widget.getComponentType();
				final Integer widgetDimensions = widget.getDimensions();

				if (widgetType != null && nodeType != null && widgetType.equals(nodeType)) {

					// if dimensions are set, they must match
					if (dimensions != null && widgetDimensions != null && dimensions != widgetDimensions) {
						continue;
					}

					// mismatched dimensions
					if (dimensions != null && widgetDimensions == null) {
						continue;
					}

					// mismatched dimensions
					if (dimensions == null && widgetDimensions != null) {
						continue;
					}

					result.add(widget);
				}
			}

			// sort result by name
			Collections.sort(result, Comparator.comparing(NodeInterface::getName));
		}

		return result;
	}

	private List<String> splitClasses(final String input) {

		final List<String> classes = new LinkedList<>();

		if (StringUtils.isNotBlank(input)) {

			for (final String css : input.split(" ")) {

				final String timmed = css.trim();

				if (StringUtils.isNotBlank(timmed)) {
					classes.add(timmed);
				}
			}
		}

		return classes;
	}

	private String getTag(final DOMNode node) {

		if (node.is("DOMElement")) {

			return node.as(DOMElement.class).getTag();
		}

		return node.getType();
	}

	private String getHtmlId(final DOMNode node) {

		if (node.is("DOMElement")) {
			return node.as(DOMElement.class).getHtmlId();
		}

		return null;
	}

	private boolean alreadyPresent(final DOMNode node, final Widget widget) {

		final String widgetName = widget.getName();

		for (final DOMNode child : node.getChildren()) {

			if (widgetName.equals(child.getName())) {

				return true;
			}
		}

		return false;
	}

	private String getComponentType(final DOMNode node) {

		if (node.hasSharedComponent()) {

			return node.getSharedComponent().getComponentType();
		}

		return node.getComponentType();
	}

	private Integer getDimensions(final DOMNode node) {

		if (node.hasSharedComponent()) {

			return node.getSharedComponent().getDimensions();
		}

		return node.getDimensions();
	}
}
