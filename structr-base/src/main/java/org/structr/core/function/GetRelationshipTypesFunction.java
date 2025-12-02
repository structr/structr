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
package org.structr.core.function;

import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObjectMap;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.StringProperty;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.SchemaHelper;
import org.structr.schema.action.ActionContext;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class GetRelationshipTypesFunction extends AdvancedScriptingFunction {

	@Override
	public String getName() {
		return "getRelationshipTypes";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("node, lookupType [, direction ]");
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
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${getRelationshipTypes(node, lookupType [, direction])}. Example: ${getRelationshipTypes(me, 'existing', 'both')}"),
			Usage.javaScript("Usage: ${{$.getRelationshipTypes(node, lookupType [, direction ])}}. Example: ${{$.getRelationshipTypes(me, 'existing', 'both')}}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Returns the list of available relationship types form and/or to this node. Either potentially available (schema) or actually available (database).";
	}

	@Override
	public String getLongDescription() {
		return "";
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
				Example.structrScript("""
						${getRelationshipTypes(me, 'schema')}
						${getRelationshipTypes(me, 'schema', 'incoming')}
						${getRelationshipTypes(me, 'schema', 'outgoing')}
						${getRelationshipTypes(me, 'existing')}
						${getRelationshipTypes(me, 'existing', 'incoming')}
						${getRelationshipTypes(me, 'existing', 'outgoing')}
						"""),
				Example.javaScript("""
						${{ $.getRelationshipTypes($.me, 'schema') }}
						${{ $.getRelationshipTypes($.me, 'schema', 'incoming') }}
						${{ $.getRelationshipTypes($.me, 'schema', 'outgoing') }}
						${{ $.getRelationshipTypes($.me, 'existing') }}
						${{ $.getRelationshipTypes($.me, 'existing', 'incoming') }}
						${{ $.getRelationshipTypes($.me, 'existing', 'outgoing') }}
						""")
		);
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
				Parameter.mandatory("node", "node for which possible relationship types should be checked"),
				Parameter.optional("lookupType", "`existing` or `schema` - default: `existing`"),
				Parameter.optional("direction", "`incoming`, `outgoing` or `both` - default: `both`")
		);
	}
}
