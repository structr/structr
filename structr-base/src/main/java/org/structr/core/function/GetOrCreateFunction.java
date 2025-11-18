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
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.property.GenericProperty;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.Traits;
import org.structr.docs.Example;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.docs.Parameter;
import org.structr.schema.action.ActionContext;

import java.util.List;
import java.util.Map;

public class GetOrCreateFunction extends CoreFunction {

	private static final String ERROR_MESSAGE_NO_TYPE_SPECIFIED = "Error in get_or_create(): no type specified.";

	@Override
	public String getName() {
		return "get_or_create";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllLanguages("type, propertyMap");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			if (sources == null) {

				throw new IllegalArgumentException();
			}

			final SecurityContext securityContext = ctx.getSecurityContext();
			final App app                         = StructrApp.getInstance(securityContext);
			final PropertyMap properties          = new PropertyMap();

			// the type to query for
			Traits type = null;

			if (sources.length >= 1 && sources[0] != null) {

				final String typeString = sources[0].toString();
				type = Traits.of(typeString);
			}

			// exit gracefully instead of crashing..
			if (type == null) {

				logger.warn("Error in get_or_create(): no type specified. Parameters: {}", getParametersAsString(sources));
				return ERROR_MESSAGE_NO_TYPE_SPECIFIED;
			}

			// extension for native javascript objects
			if (sources.length == 2 && sources[1] instanceof Map) {

				final PropertyMap convertedProperties = PropertyMap.inputTypeToJavaType(securityContext, type.getName(), (Map)sources[1]);

				// check property keys manually (not allowed to use generic properties here)
				for (final PropertyKey key : convertedProperties.keySet()) {

					if (key instanceof GenericProperty) {

						throw new FrameworkException(422, "Unknown key `" + key.jsonName() + "`");
					}
				}

				properties.putAll(convertedProperties);

			} else {

				final int parameter_count = sources.length;

				if (parameter_count % 2 == 0) {

					throw new FrameworkException(400, "Invalid number of parameters: " + parameter_count + ". Should be uneven: " + usage(ctx.isJavaScriptContext()));
				}

				for (int c = 1; c < parameter_count; c += 2) {

					if (sources[c] == null) {
						throw new IllegalArgumentException();
					}

					final String keyName  = sources[c].toString();
					final PropertyKey key = type.key(keyName);
					if (key != null) {

						final PropertyConverter inputConverter = key.inputConverter(securityContext, false);
						Object value                           = sources[c + 1];

						if (inputConverter != null) {

							value = inputConverter.convert(value);
						}

						properties.put(key, value);

					} else {

						throw new FrameworkException(422, "Unknown key `" + keyName + "`");
					}
				}
			}

			final GraphObject obj = app.nodeQuery(type.getName()).disableSorting().pageSize(1).and().key(properties).getFirst();
			if (obj != null) {

				// return existing object
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
			Usage.javaScript("Usage: ${{ $.getOrCreate(type, properties)}}. Example: ${{ $.getOrCreate(\"User\", { eMail: 'tester@test.com' }); }}"),
			Usage.structrScript("Usage: ${get_or_create(type, properties)}. Example: ${get_or_create(\"User\", \"email\", \"tester@test.com\"}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Returns an entity with the given properties, creating one if it doesn't exist.";
	}

	@Override
	public String getLongDescription() {
		return """
				`get_or_create()` finds and returns a single object with the given properties 
				(key/value pairs or a map of properties) and **creates** that object if it does not exist yet.
				The function accepts three different parameter combinations, where the first parameter is always the 
				name of the type to retrieve from the database. The second parameter can either 
				be a map (e.g. a result from nested function calls) or a list of (key, value) pairs.
				""";
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
				Example.javaScript("""
				${get_or_create('User', 'name', 'admin')}
				> 7379af469cd645aebe1a3f8d52b105bd
				${get_or_create('User', 'name', 'admin')}
				> 7379af469cd645aebe1a3f8d52b105bd
				${get_or_create('User', 'name', 'admin')}
				> 7379af469cd645aebe1a3f8d52b105bd
				""", "The example shows that repeated calls to `get_or_create()` with the same parameters will always return the same object.")
		);
	}

	@Override
	public List<String> getNotes() {
		return List.of(
				"The `get_or_create()` method will always use **exact** search, if you are interested in inexact / case-insensitive search, use `search()`.",
				"In a StructrScript environment parameters are passed as pairs of `'key1', 'value1'`.",
				"In a JavaScript environment, the function can be used just as in a StructrScript environment. Alternatively it can take a map as the second parameter."
		);
	}


	@Override
	public List<Parameter> getParameters() {

		return List.of(
				Parameter.mandatory("type", "type of node"),
				Parameter.optional("map", "values map (only for javascript)"),
				Parameter.optional("key1", "key for key-value-pair 1 (only for structrScript)"),
				Parameter.optional("value1", "value for key-value-pair 1 (only for structrScript)"),
				Parameter.optional("key2", "key for key-value-pair 2 (only for structrScript)"),
				Parameter.optional("value2", "value for key-value-pair 2 (only for structrScript)"),
				Parameter.optional("keyN", "key for key-value-pair N (only for structrScript)"),
				Parameter.optional("valueN", "value for key-value-pair N (only for structrScript)")
		);
	}
}
