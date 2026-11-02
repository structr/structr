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
package org.structr.web.appmap;

import org.apache.commons.lang3.StringUtils;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.web.entity.dom.Content;
import org.structr.web.entity.dom.DOMElement;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.event.ActionMapping;

import java.util.*;

/**
 * One node of the HTML structure of a page or shared component, with what the map reads
 * from it: its texts and scripts, its data key and the page or shared component it lives in
 * (its home).
 */
class HtmlNode {

	// the attributes whose values may hold a script expression
	private static final List<String> SCRIPT_ATTRIBUTES = List.of("_html_href", "_html_value", "_html_name", "_html_class", "_html_id", "_html_src");

	final List<HtmlNode> children     = new LinkedList<>();
	final List<ActionMapping> actions = new LinkedList<>();

	// the repeater and action mapping nodes of the map that live on this element
	final List<String> mapIds         = new LinkedList<>();
	final DOMNode node;
	final HtmlNode parent;
	final String id;
	final String home;
	final String type;
	final String tag;
	final String name;
	final String dataKey;
	final String functionQuery;
	final String cypherQuery;
	final String content;
	final boolean isElement;
	final boolean isActive;

	HtmlNode(final DOMNode node, final HtmlNode parent, final String home) {

		this.node          = node;
		this.parent        = parent;
		this.home          = home;
		this.id            = node.getUuid();
		this.type          = node.getType();
		this.isElement     = node.is(StructrTraits.DOM_ELEMENT);
		this.tag           = isElement ? node.as(DOMElement.class).getTag() : null;
		this.name          = node.getName();
		this.dataKey       = node.getDataKey();
		this.functionQuery = node.getFunctionQuery();
		this.cypherQuery   = node.getCypherQuery();
		this.content       = node.is(StructrTraits.CONTENT) ? node.as(Content.class).getContent() : null;
		this.isActive      = isActive(node);
	}

	/**
	 * The value of an attribute that not every kind of element has, or null.
	 */
	String attribute(final String key) {

		final Traits traits = node.getTraits();

		if (!traits.hasKey(key)) {
			return null;
		}

		final Object value = node.getProperty(traits.key(key));

		return value != null ? value.toString() : null;
	}

	/**
	 * Everything on the node a script expression can be in.
	 */
	String text() {

		final List<String> parts = new LinkedList<>();

		addIfPresent(parts, content);
		addIfPresent(parts, functionQuery);
		addIfPresent(parts, node.getShowConditions());
		addIfPresent(parts, node.getHideConditions());

		for (final String key : SCRIPT_ATTRIBUTES) {
			addIfPresent(parts, attribute(key));
		}

		return String.join(" ", parts);
	}

	/**
	 * The node and everything below it, in the order a stack visits them.
	 */
	List<HtmlNode> subtree() {

		final List<HtmlNode> result     = new LinkedList<>();
		final Deque<HtmlNode> stack     = new ArrayDeque<>(List.of(this));

		while (!stack.isEmpty()) {

			final HtmlNode current = stack.pop();

			result.add(current);

			for (final HtmlNode child : current.children) {
				stack.push(child);
			}
		}

		return result;
	}

	/**
	 * The nodes above this one, innermost first.
	 */
	List<HtmlNode> ancestors() {

		final List<HtmlNode> result = new LinkedList<>();
		HtmlNode current            = parent;

		while (current != null) {

			result.add(current);
			current = current.parent;
		}

		return result;
	}

	// ----- private methods -----
	private void addIfPresent(final List<String> parts, final String value) {

		if (StringUtils.isNotEmpty(value)) {
			parts.add(value);
		}
	}

	/**
	 * An element that binds data, embeds a shared component or reacts to events does something;
	 * the rest is structure, also an element that is only shown conditionally.
	 */
	private static boolean isActive(final DOMNode node) {

		if (node.getDataKey() != null || StringUtils.isNotBlank(node.getFunctionQuery()) || StringUtils.isNotBlank(node.getCypherQuery())) {
			return true;
		}

		if (node.getSharedComponent() != null) {
			return true;
		}

		return node.is(StructrTraits.DOM_ELEMENT) && node.as(DOMElement.class).getTriggeredActions().iterator().hasNext();
	}
}
