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

import org.structr.api.graph.Cardinality;
import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonReferenceType;
import org.structr.api.schema.JsonSchema;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.LinkedTreeNode;
import org.structr.core.graph.NodeInterface;
import org.structr.schema.SchemaService;

import java.net.URI;
import java.util.Map;

/**
 *
 */
public interface StructuredTextNode extends NodeInterface, LinkedTreeNode<StructuredTextNode> {

	static class Impl { static {

		final JsonSchema schema    = SchemaService.getDynamicSchema();
		final JsonObjectType type = schema.addType("StructuredTextNode");

		type.setImplements(URI.create("https://structr.org/v1.1/definitions/StructuredTextNode"));
		type.setExtends(URI.create("https://structr.org/v1.1/definitions/LinkedTreeNodeImpl?typeParameters=org.structr.text.model.StructuredTextNode"));

		type.overrideMethod("getSiblingLinkType",          false, "return StructuredTextNodeNEXTStructuredTextNode.class;");
		type.overrideMethod("getChildLinkType",            false, "return StructuredTextNodeCONTAINSStructuredTextNode.class;");
		type.overrideMethod("getPositionProperty",         false, "return StructuredTextNodeCONTAINSStructuredTextNode.positionProperty;");
		type.overrideMethod("onNodeDeletion",              true,  "try { final org.structr.text.model.StructuredTextNode parent = this.treeGetParent(); if (parent != null) { parent.treeRemoveChild(this); } } catch (FrameworkException fex) { fex.printStackTrace(); }");

		type.addPropertyGetter("content", String.class);
		type.addPropertySetter("content", String.class);

		type.addStringProperty("content");
		type.addStringProperty("kind");

		final JsonReferenceType parent   = type.relate(type, "CONTAINS", Cardinality.OneToMany, "parent",           "children");
		final JsonReferenceType siblings = type.relate(type, "NEXT",     Cardinality.OneToOne,  "previousSibling",  "nextSibling");

		// sort position of children in page
		parent.addIntegerProperty("position");
		parent.setCascadingDelete(JsonSchema.Cascade.sourceToTarget);

		type.addMethod("getChildren")
			.addParameter("ctx", SecurityContext.class.getName())
			.addParameter("parameters", "java.util.Map<java.lang.String, java.lang.Object>")
			.setReturnType("java.util.List<org.structr.text.model.StructuredTextNode>")
			.setSource("return this.treeGetChildren();")
			.addException(FrameworkException.class.getName())
			.setDoExport(true);

		// combine method
		type.addMethod("combine")
			.addParameter("ctx", SecurityContext.class.getName())
			.addParameter("parameters", "java.util.Map<java.lang.String, java.lang.Object>")
			.setReturnType("java.util.Map<java.lang.String, java.lang.Object>")
			.setSource("return " + StructuredTextNode.class.getName() + ".combine(this, parameters);")
			.addException(FrameworkException.class.getName())
			.setDoExport(true);

		// split method
		type.addMethod("split")
			.addParameter("ctx", SecurityContext.class.getName())
			.addParameter("parameters", "java.util.Map<java.lang.String, java.lang.Object>")
			.setReturnType("java.util.Map<java.lang.String, java.lang.Object>")
			.setSource("return " + StructuredTextNode.class.getName() + ".split(this, parameters);")
			.addException(FrameworkException.class.getName())
			.setDoExport(true);
	}}

	void setContent(final String content) throws FrameworkException;
	String getContent();

	static Map<String, Object> combine(final StructuredTextNode thisNode, final Map<String, Object> parameters) throws FrameworkException {

		final String id = stringOrDefault(parameters.get("id"), null);
		if (id != null) {

			final App app                      = StructrApp.getInstance(thisNode.getSecurityContext());
			final StructuredTextNode otherNode = app.get(StructuredTextNode.class, id);

			if (otherNode != null) {

				final String separator = stringOrDefault(parameters.get("separator"), " ");
				final boolean delete   = booleanOrDefault(parameters.get("delete"), Boolean.FALSE);

				thisNode.setContent(thisNode.getContent() + separator + otherNode.getContent());

				if (delete) {

					// remove from parent
					final StructuredTextNode parent = thisNode.treeGetParent();
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

	static Map<String, Object> split(final StructuredTextNode thisNode, final Map<String, Object> parameters) throws FrameworkException {

		final App app = StructrApp.getInstance(thisNode.getSecurityContext());
		final Integer pos = integerOrDefault(parameters.get("position"), null);

		if (pos != null) {

			final String content = thisNode.getContent();
			if (pos <= 0 || pos >= content.length()) {

				throw new FrameworkException(422, "Invalid parameters for split() method, parameter 'position' is out of range.");

			} else {

				String part1 = content.substring(0, pos);
				String part2 = content.substring(pos);

				if (booleanOrDefault(parameters.get("trim"), true)) {

					part1 = part1.trim();
					part2 = part2.trim();
				}

				thisNode.setContent(part1);

				// create new node
				final StructuredTextNode newNode = app.create(thisNode.getClass());
				newNode.setContent(part2);

				// link into parent/child structure
				final StructuredTextNode parent = thisNode.treeGetParent();
				if (parent != null) {

					parent.treeInsertAfter(newNode, thisNode);
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
