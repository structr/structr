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

import org.structr.common.SecurityContext;
import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObjectMap;
import org.structr.core.app.StructrApp;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.Traits;
import org.structr.schema.action.ActionContext;

import java.util.Map;

public class CreateRelationshipFunction extends CoreFunction {

	public static final String ERROR_MESSAGE_CREATE_RELATIONSHIP    = "Usage: ${create_relationship(from, to, relType)}. Example: ${create_relationship(me, user, 'FOLLOWS')} (Relationshiptype has to exist)";
	public static final String ERROR_MESSAGE_CREATE_RELATIONSHIP_JS = "Usage: ${{Structr.create_relationship(from, to, relType)}}. Example: ${{Structr.create_relationship(Structr.get('me'), user, 'FOLLOWS')}} (Relationshiptype has to exist)";

	@Override
	public String getName() {
		return "create_relationship";
	}

	@Override
	public String getSignature() {
		return "from, to, relType [, parameterMap ]";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndAllElementsNotNull(sources, 3);

			final Object source = sources[0];
			final Object target = sources[1];
			final String relType = (String)sources[2];

			NodeInterface sourceNode = null;
			NodeInterface targetNode = null;

			if (source instanceof NodeInterface && target instanceof NodeInterface) {

				sourceNode = (NodeInterface)source;
				targetNode = (NodeInterface)target;

			} else {

				logger.warn("Error: entities are not nodes. Parameters: {}", getParametersAsString(sources));
				return "Error: entities are not nodes.";
			}

			final Traits traits = Traits.ofRelationship(sourceNode.getType(), relType, targetNode.getType());
			if (traits != null) {

				final String relationshipTypeName     = traits.getName();
				final SecurityContext securityContext = ctx.getSecurityContext();
				PropertyMap propertyMap;

				// extension for native javascript objects
				if (sources.length == 4 && sources[3] instanceof Map) {

					propertyMap = PropertyMap.inputTypeToJavaType(securityContext, relationshipTypeName, (Map)sources[3]);

				} else if (sources.length == 4 && sources[3] instanceof GraphObjectMap) {

					propertyMap = PropertyMap.inputTypeToJavaType(securityContext, relationshipTypeName, ((GraphObjectMap)sources[3]).toMap());

				} else {

					propertyMap               = new PropertyMap();
					final int parameter_count = sources.length;

					if (parameter_count % 2 == 0) {

						throw new FrameworkException(400, "Invalid number of parameters: " + parameter_count + ". Should be uneven: " + usage(ctx.isJavaScriptContext()));
					}

					for (int c = 3; c < parameter_count; c += 2) {

						final PropertyKey key = traits.key(sources[c].toString());

						if (key != null) {

							final PropertyConverter inputConverter = key.inputConverter(securityContext, false);
							Object value = sources[c + 1];

							if (inputConverter != null) {

								value = inputConverter.convert(value);
							}

							propertyMap.put(key, value);
						}
					}
				}

				return StructrApp.getInstance(sourceNode.getSecurityContext()).create(sourceNode, targetNode, relationshipTypeName, propertyMap);

			} else {

				logger.warn("Error: Unknown relationship type. Parameters: {}", getParametersAsString(sources));
				return "Error: Unknown relationship type";
			}

		} catch (ArgumentNullException pe) {

			// silently ignore null arguments
			return "";

		} catch (ArgumentCountException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_CREATE_RELATIONSHIP_JS : ERROR_MESSAGE_CREATE_RELATIONSHIP);
	}

	@Override
	public String shortDescription() {
		return "Creates a relationship of the given type between two entities";
	}
}
