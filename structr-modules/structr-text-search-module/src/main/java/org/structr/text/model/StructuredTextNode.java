/**
 * Copyright (C) 2010-2019 Structr GmbH
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

import java.net.URI;
import java.util.Map;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.LinkedTreeNode;
import org.structr.core.entity.Relation.Cardinality;
import org.structr.core.graph.NodeInterface;
import org.structr.schema.SchemaService;
import org.structr.schema.json.JsonObjectType;
import org.structr.schema.json.JsonReferenceType;
import org.structr.schema.json.JsonSchema;

/**
 *
 * @author Christian Morgner
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

		type.addPropertyGetter("content", String.class);
		type.addPropertySetter("content", String.class);

		type.addStringProperty("content");
		type.addStringProperty("kind");

		final JsonReferenceType parent   = type.relate(type, "CONTAINS", Cardinality.OneToMany, "parent",           "children");
		final JsonReferenceType siblings = type.relate(type, "NEXT",     Cardinality.OneToOne,  "previousSibling",  "nextSibling");

		// sort position of children in page
		parent.addIntegerProperty("position");
		parent.setCascadingDelete(JsonSchema.Cascade.sourceToTarget);

		// combine method

		type.addMethod("combine")
			.addParameter("ctx", SecurityContext.class.getName())
			.addParameter("parameters", "java.util.Map<java.lang.String, java.lang.Object>")
			.setReturnType("java.util.Map<java.lang.String, java.lang.Object>")
			.setSource("return " + StructuredTextNode.class.getName() + ".combine(this, parameters);")
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

					app.delete(otherNode);
				}

			} else {

				throw new FrameworkException(422, "Node with ID " + id + " not found.");
			}

		} else {

			throw new FrameworkException(422, "Invalid parameter for combine() method, missing parameter 'id'");
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

	static Boolean booleanOrDefault(final Object value, final Boolean defaultValue) {

		if (value != null && value instanceof Boolean) {

			return (Boolean)value;
		}

		return defaultValue;
	}
}
