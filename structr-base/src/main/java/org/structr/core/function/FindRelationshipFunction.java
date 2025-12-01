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

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.QueryGroup;
import org.structr.core.app.StructrApp;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;

import java.util.List;
import java.util.Map;

public class FindRelationshipFunction extends CoreFunction {

	public static final String ERROR_MESSAGE_FIND_RELATIONSHIP_NO_TYPE_SPECIFIED = "Error in findRelationship(): no type specified.";
	public static final String ERROR_MESSAGE_FIND_RELATIONSHIP_TYPE_NOT_FOUND = "Error in findRelationship(): type not found: ";

	@Override
	public String getName() {
		return "findRelationship";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("type [, parameterMap ]");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			if (sources == null) {

				throw new IllegalArgumentException();
			}

			final SecurityContext securityContext = ctx.getSecurityContext();
			final QueryGroup query  = StructrApp.getInstance(securityContext).relationshipQuery().sort(Traits.of(StructrTraits.GRAPH_OBJECT).key(GraphObjectTraitDefinition.CREATED_DATE_PROPERTY)).and();

			// the type to query for
			Traits traits = null;

			if (sources.length >= 1 && sources[0] != null) {

				final String typeString = sources[0].toString();
				traits = Traits.of(typeString);

				if (traits != null) {

					query.types(traits);

				} else {

					logger.warn("Error in findRelationship(): type \"{}\" not found.", typeString);
					return ERROR_MESSAGE_FIND_RELATIONSHIP_TYPE_NOT_FOUND + typeString;
				}
			}

			// exit gracefully instead of crashing..
			if (traits == null) {

				logger.warn("Error in findRelationship(): no type specified. Parameters: {}", getParametersAsString(sources));
				return ERROR_MESSAGE_FIND_RELATIONSHIP_NO_TYPE_SPECIFIED;
			}

			// extension for native javascript objects
			if (sources.length == 2 && sources[1] instanceof Map) {

				query.key(PropertyMap.inputTypeToJavaType(securityContext, traits.getName(), (Map)sources[1]));

			} else if (sources.length == 2) {

				if (sources[1] == null) {

					throw new IllegalArgumentException();
				}

				// special case: second parameter is a UUID
				final PropertyKey key = traits.key(GraphObjectTraitDefinition.ID_PROPERTY);

				query.key(key, sources[1].toString());

				return query.getFirst();

			} else {

				final int parameterCount = sources.length;

				if (parameterCount % 2 == 0) {

					throw new FrameworkException(400, "Invalid number of parameters: " + parameterCount + ". Should be uneven: " + usage(ctx.isJavaScriptContext()));
				}

				for (int c = 1; c < parameterCount; c += 2) {

					if (sources[c] == null) {
						throw new IllegalArgumentException();
					}

					final PropertyKey key = traits.key(sources[c].toString());

					if (key != null) {

						final PropertyConverter inputConverter = key.inputConverter(securityContext, false);
						Object value = sources[c + 1];

						if (inputConverter != null) {

							value = inputConverter.convert(value);
						}

						query.key(key, value);
					}
				}
			}

			return query.getAsList();

		} catch (final IllegalArgumentException e) {

			logParameterError(caller, sources, ctx.isJavaScriptContext());

			return usage(ctx.isJavaScriptContext());
		}
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.javaScript("Usage: ${{ $.findRelationship(type, key, value); }}. Example: ${{ $.findRelationship('PersonRELATED_TOPerson'); }}"),
			Usage.structrScript("Usage: ${findRelationship(type, key, value)}. Example: ${findRelationship('PersonRELATED_TOPerson')}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Returns a collection of relationship entities of the given type from the database, takes optional key/value pairs.";
	}

	@Override
	public String getLongDescription() {
		return "";
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
			Parameter.mandatory("type", "type to return (includes inherited types"),
			Parameter.optional("predicates", "list of predicates"),
			Parameter.optional("uuid", "uuid, makes the function return **a single object**")
		);
	}

	@Override
	public List<Example> getExamples() {
		return super.getExamples();
	}

	@Override
	public List<String> getNotes() {

		return List.of(
			"The relationship type for custom schema relationships is auto-generated as `<source type name><relationship type><target type name>`",
			"In a StructrScript environment parameters are passed as pairs of `'key1', 'value1'`.",
			"In a JavaScript environment, the function can be used just as in a StructrScript environment. Alternatively it can take a map as the second parameter."
		);
	}
}
