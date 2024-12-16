/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.text.model;

import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.View;
import org.structr.common.error.FrameworkException;
import org.structr.core.Export;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.*;
import org.structr.core.property.*;
import org.structr.text.model.relationship.StructuredTextNodeCONTAINSStructuredTextNode;
import org.structr.text.model.relationship.StructuredTextNodeNEXTStructuredTextNode;

import java.util.List;
import java.util.Map;

/**
 *
 */
public class StructuredTextNode extends AbstractNode implements LinkedTreeNode<StructuredTextNode, StructuredTextNode> {

	public static final Property<Iterable<StructuredTextNode>> childrenProperty = new EndNodes<>("children", StructuredTextNodeCONTAINSStructuredTextNode.class);
	public static final Property<StructuredTextNode> parentProperty             = new StartNode<>("parent", StructuredTextNodeCONTAINSStructuredTextNode.class);

	public static final Property<StructuredTextNode> nextSiblingProperty     = new StartNode<>("nextSibling", StructuredTextNodeNEXTStructuredTextNode.class);
	public static final Property<StructuredTextNode> previousSiblingProperty = new EndNode<>("previousSibling", StructuredTextNodeNEXTStructuredTextNode.class);

	public static final Property<String> contentProperty = new StringProperty("content");
	public static final Property<String> kindProperty    = new StringProperty("kind");

	public static final View defaultView = new View(StructuredTextNode.class, PropertyView.Public);
	public static final View uiView      = new View(StructuredTextNode.class, PropertyView.Ui);

	// ----- abstract method implementations -----
	@Override
	public String getChildLinkType() {
		return "StructuredTextNodeCONTAINSStructuredTextNode";
	}

	@Override
	public String getSiblingLinkType() {
		return "StructuredTextNodeNEXTStructuredTextNode";
	}

	@Override
	public Property<Integer> getPositionProperty() {
		return StructuredTextNodeCONTAINSStructuredTextNode.position;
	}

	@Override
	public void onNodeDeletion(SecurityContext securityContext) throws FrameworkException {

		super.onNodeDeletion(securityContext);

		try {
			final org.structr.text.model.StructuredTextNode parent = this.treeGetParent();
			if (parent != null) {

				parent.treeRemoveChild(this);
			}

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}
	}

	// ----- property getters and setters -----
	public String getContent() {
		return getProperty(contentProperty);
	}

	public void setContent(final String content) throws FrameworkException {
		setProperty(contentProperty, content);
	}

	// ----- exported methods -----
	@Export
	public List<StructuredTextNode> getChildren(final SecurityContext securityContext, final Map<String, Object> parameters) throws FrameworkException {
		return treeGetChildren();
	}

	@Export
	public Map<String, Object> combine(final SecurityContext securityContext, final Map<String, Object> parameters) throws FrameworkException {

		final String id = stringOrDefault(parameters.get("id"), null);
		if (id != null) {

			final App app                      = StructrApp.getInstance(securityContext);
			final StructuredTextNode otherNode = app.get(StructuredTextNode.class, id);

			if (otherNode != null) {

				final String separator = stringOrDefault(parameters.get("separator"), " ");
				final boolean delete   = booleanOrDefault(parameters.get("delete"), Boolean.FALSE);

				this.setContent(this.getContent() + separator + otherNode.getContent());

				if (delete) {

					// remove from parent
					final StructuredTextNode parent = this.treeGetParent();
					if (parent != null) {

						parent.treeRemoveChild(otherNode);
					}

					app.delete(otherNode);
				}

			} else {

				throw new FrameworkException(422, "Node with ID " + id + " not found.");
			}

		} else {

			throw new FrameworkException(422, "Invalid parameters for combine() method, missing parameter 'id'. Usage: combine(id, [separator=' ', delete=false]");
		}

		return null;
	}

	@Export
	public Map<String, Object> split(final SecurityContext securityContext, final Map<String, Object> parameters) throws FrameworkException {

		final App app = StructrApp.getInstance(securityContext);
		final Integer pos = integerOrDefault(parameters.get("position"), null);

		if (pos != null) {

			final String content = getContent();
			if (pos <= 0 || pos >= content.length()) {

				throw new FrameworkException(422, "Invalid parameters for split() method, parameter 'position' is out of range.");

			} else {

				String part1 = content.substring(0, pos);
				String part2 = content.substring(pos);

				if (booleanOrDefault(parameters.get("trim"), true)) {

					part1 = part1.trim();
					part2 = part2.trim();
				}

				setContent(part1);

				// create new node
				final StructuredTextNode newNode = app.create(getClass());
				newNode.setContent(part2);

				// link into parent/child structure
				final StructuredTextNode parent = treeGetParent();
				if (parent != null) {

					parent.treeInsertAfter(newNode, this);
				}
			}

		} else {

			throw new FrameworkException(422, "Invalid parameters for split() method, missing parameter 'position'. Usage: split(position, [trim=true])");
		}

		return null;
	}

	// ----- private methods -----
	static String stringOrDefault(final Object value, final String defaultValue) {

		if (value != null && value instanceof String) {

			return (String)value;
		}

		return defaultValue;
	}

	static Integer integerOrDefault(final Object value, final Integer defaultValue) {

		if (value != null) {

			if (value instanceof Integer) {

				return (Integer)value;
			}

			if (value instanceof String) {

				try { return Double.valueOf((String)value).intValue(); } catch (Throwable t) {}
			}
		}

		return defaultValue;
	}

	static Boolean booleanOrDefault(final Object value, final Boolean defaultValue) {

		if (value != null && value instanceof Boolean) {

			return (Boolean)value;
		}

		return defaultValue;
	}
}
