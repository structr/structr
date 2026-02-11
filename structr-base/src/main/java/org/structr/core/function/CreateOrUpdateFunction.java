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
package org.structr.core.function;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.Traits;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.docs.ontology.FunctionCategory;
import org.structr.schema.ConfigurationProvider;
import org.structr.schema.action.ActionContext;

import java.util.List;
import java.util.Map;

public class CreateOrUpdateFunction extends CoreFunction {

	private static final String ERROR_MESSAGE_NO_TYPE_SPECIFIED = "Error in createOrUpdate(): no type specified.";
	private static final String ERROR_MESSAGE_TYPE_NOT_FOUND    = "Error in createOrUpdate(): type not found: ";

	@Override
	public String getName() {
		return "createOrUpdate";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("type, propertyMap");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			if (sources == null) {

				throw new IllegalArgumentException();
			}

			final SecurityContext securityContext = ctx.getSecurityContext();
			final ConfigurationProvider config    = StructrApp.getConfiguration();
			final App app                         = StructrApp.getInstance(securityContext);
			final PropertyMap properties          = new PropertyMap();

			// the type to query for
			Traits type = null;

			if (sources.length >= 1 && sources[0] != null) {

				final String typeString = sources[0].toString();
				type = Traits.of(typeString);

				if (type == null) {

					logger.warn("Error in createOrUpdate(): type \"{}\" not found.", typeString);
					return ERROR_MESSAGE_TYPE_NOT_FOUND + typeString;
				}
			}

			// exit gracefully instead of crashing..
			if (type == null) {

				logger.warn("Error in createOrUpdate(): no type specified. Parameters: {}", getParametersAsString(sources));
				return ERROR_MESSAGE_NO_TYPE_SPECIFIED;
			}

			// extension for native javascript objects
			if (sources.length == 2 && sources[1] instanceof Map) {

				properties.putAll(PropertyMap.inputTypeToJavaType(securityContext, type.getName(), (Map)sources[1]));

			} else {

				final int parameterCount = sources.length;

				if (parameterCount % 2 == 0) {

					throw new FrameworkException(400, "Invalid number of parameters: " + parameterCount + ". Should be uneven: " + usage(ctx.isJavaScriptContext()));
				}

				for (int c = 1; c < parameterCount; c += 2) {

					if (sources[c] == null) {
						throw new IllegalArgumentException();
					}

					final PropertyKey key = type.key(sources[c].toString());
					if (key != null) {


						final PropertyConverter inputConverter = key.inputConverter(securityContext, false);
						Object value                           = sources[c + 1];

						if (inputConverter != null) {

							value = inputConverter.convert(value);
						}

						properties.put(key, value);

					}
				}
			}

			NodeInterface obj = null;

			for (final PropertyKey key : properties.keySet()) {

				if (key.isUnique()) {

					// If a key is unique, try to find an existing object
					obj = (NodeInterface) app.nodeQuery(type.getName()).disableSorting().pageSize(1).and().key(key, properties.get(key)).getFirst();

					if (obj != null) {
						break;
					}
				}
			}

			if (obj != null) {

				// update existing object
				obj.setProperties(securityContext, properties);

				return obj;
			}

			// create new object
			return app.create(type.getName(), properties);

		} catch (final IllegalArgumentException e) {

			logParameterError(caller, sources, ctx.isJavaScriptContext());

			return usage(ctx.isJavaScriptContext());

		}
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.javaScript("Usage: ${{$.createOrUpdate(type, properties)}}. Example: ${{$.createOrUpdate('User', 'email', 'tester@test.com')}}"),
			Usage.structrScript("Usage: ${createOrUpdate(type, properties)}. Example: ${createOrUpdate('User', 'email', 'tester@test.com')}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Creates an object with the given properties or updates an existing object if it can be identified by a unique property.";
	}

	@Override
	public String getLongDescription() {
		return "This function is a shortcut for a code sequence with a query that looks up an existing object and a set() operation it if an object was found, or creates a new object with the given properties, if no object was found.";
	}

	@Override
	public List<Parameter> getParameters() {
		return List.of(
			Parameter.mandatory("type", "type to create or update"),
			Parameter.mandatory("properties", "properties")
		);
	}

	@Override
	public List<Example> getExamples() {

		return List.of(
			Example.structrScript("${createOrUpdate('User', 'eMail', 'tester@test.com', 'name', 'New Name')}", "If no object with the given e-mail address exists, a new object is created, because \"eMail\" is unique. Otherwise, the existing object is updated with the new name.")
		);
	}

	@Override
	public List<String> getNotes() {
		return List.of(
			"In a StructrScript environment, parameters are passed as pairs of `'key1', 'value1'`.",
			"In a JavaScript environment, the function can be used just as in a StructrScript environment. Alternatively it can take a map as the second parameter."
		);
	}

	@Override
	public FunctionCategory getCategory() {
		return FunctionCategory.Database;
	}
}
