/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.core.function;

import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObjectMap;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.StringProperty;
import org.structr.schema.SchemaHelper;
import org.structr.schema.action.ActionContext;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class GetRelationshipTypesFunction extends AdvancedScriptingFunction {

	public static final String ERROR_MESSAGE_GET_RELATIONSHIP_TYPES    = "Usage: ${get_relationship_types(node, lookupType [, direction])}. Example: ${get_relationship_types(me, 'existing', 'both')}";
	public static final String ERROR_MESSAGE_GET_RELATIONSHIP_TYPES_JS = "Usage: ${{Structr.get_relationship_types(node, lookupType [, direction ])}}. Example: ${{Structr.get_relationship_types(me, 'existing', 'both')}}";

	@Override
	public String getName() {
		return "get_relationship_types";
	}

	@Override
	public String getSignature() {
		return "node, lookupType [, direction ]";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		final Set<String> resultSet = new HashSet();

		try {

			assertArrayHasMinLengthAndMaxLengthAndAllElementsNotNull(sources, 1, 3);

			final Object param1 = sources[0];
			NodeInterface node = null;

			if (param1 instanceof NodeInterface) {

				node = (NodeInterface)param1;

			} else {

				logger.warn("Error: entity is not a node. Parameters: {}", getParametersAsString(sources));
				return "Error: entity is not a node.";
			}

			final String lookupType = (sources.length >= 2) ? sources[1].toString() : "existing";
			final String direction  = (sources.length >= 3) ? sources[2].toString() : "both";

			switch(lookupType) {

				case "schema":

					final PropertyKey<String> relatedTypeKey      = new StringProperty("relatedType");
					final PropertyKey<String> classNameKey        = new StringProperty("className");
					final PropertyKey<String> relationshipTypeKey = new StringProperty("relationshipType");

					final List<GraphObjectMap> propertyList = SchemaHelper.getSchemaTypeInfo(ctx.getSecurityContext(), node.getType(), "all");

					switch(direction) {
						case "incoming":
							for (final GraphObjectMap gom : propertyList) {

								if (gom.containsKey(relatedTypeKey) && gom.get(classNameKey).startsWith("org.structr.core.property.StartNode")) {
									resultSet.add(gom.get(relationshipTypeKey));
								}
							}
							break;

						case "outgoing":
							for (final GraphObjectMap gom : propertyList) {

								if (gom.containsKey(relatedTypeKey) && gom.get(classNameKey).startsWith("org.structr.core.property.EndNode")) {
									resultSet.add(gom.get(relationshipTypeKey));
								}
							}
							break;

						default:
						case "both":
							for (final GraphObjectMap gom : propertyList) {

								if (gom.containsKey(relatedTypeKey) && (gom.get(classNameKey).startsWith("org.structr.core.property.StartNode") || gom.get(classNameKey).startsWith("org.structr.core.property.EndNode"))) {
									resultSet.add(gom.get(relationshipTypeKey));
								}
							}
							break;
					}
					break;

				default:
				case "existing":

					switch(direction) {
						case "incoming":
							for (final RelationshipInterface rel : node.getIncomingRelationships()) {

								resultSet.add(rel.getRelType().name());
							}
							break;

						case "outgoing":
							for (final RelationshipInterface rel : node.getOutgoingRelationships()) {

								resultSet.add(rel.getRelType().name());
							}
							break;

						default:
						case "both":
							for (final RelationshipInterface rel : node.getRelationships()) {

								resultSet.add(rel.getRelType().name());
							}
							break;
					}
			}

		} catch (ArgumentNullException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());

		} catch (ArgumentCountException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}

		return resultSet;
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_GET_RELATIONSHIP_TYPES_JS : ERROR_MESSAGE_GET_RELATIONSHIP_TYPES);
	}

	@Override
	public String shortDescription() {
		return "Returns the list of available relationship types form and/or to this node. Either potentially available (schema) or actually available (database).";
	}
}
