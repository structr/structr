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

import org.structr.api.config.Settings;
import org.structr.common.SecurityContext;
import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.GraphObjectMap;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.Traits;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.schema.action.ActionContext;

import java.util.List;
import java.util.Map;

public class SetFunction extends CoreFunction {

	@Override
	public String getName() {
		return "set";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("entity, parameterMap");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndAllElementsNotNull(sources, 2);

			final boolean useGenericPropertyForUnknownKeys = Settings.AllowUnknownPropertyKeys.getValue(false) || (sources[0] instanceof GraphObjectMap);
			final SecurityContext securityContext          = ctx.getSecurityContext();

			Traits type = null;
			PropertyMap propertyMap = null;

			if (sources[0] instanceof GraphObject) {

				final GraphObject source = (GraphObject) sources[0];
				type = source.getTraits();
			}

			if (type == null) {

				throw new FrameworkException(422, "Can't get type of object '" + sources[0].toString() + "' in set() method!");
			}

			final GraphObject sourceObject = (GraphObject) sources[0];

			if (sources.length == 2 && sources[1] instanceof Map) {

				propertyMap = PropertyMap.inputTypeToJavaType(securityContext, type.getName(), (Map) sources[1]);

			} else if (sources.length == 2 && sources[1] instanceof GraphObjectMap) {

				propertyMap = PropertyMap.inputTypeToJavaType(securityContext, type.getName(), ((GraphObjectMap)sources[1]).toMap());

			} else if (sources.length > 2) {

				propertyMap               = new PropertyMap();
				final int parameter_count = sources.length;

				if (parameter_count % 2 == 0) {

					throw new FrameworkException(400, "Invalid number of parameters: " + parameter_count + ". Should be uneven: " + usage(ctx.isJavaScriptContext()));
				}

				for (int c = 1; c < parameter_count; c += 2) {

					final String keyName  = sources[c].toString();
					final PropertyKey key = (useGenericPropertyForUnknownKeys ? type.keyOrGenericProperty(keyName) : type.key(keyName));

					if (key != null) {

						final PropertyConverter inputConverter = key.inputConverter(securityContext, false);
						Object value = sources[c + 1];

						if (inputConverter != null) {

							value = inputConverter.convert(value);
						}

						propertyMap.put(key, value);

					} else {

						// key does not exist and generic property is not desired => log warning
						logger.warn("set(): Unknown property {}.{}, value will not be set.", type.getName(), keyName);
					}
				}

			} else {

				throw new FrameworkException(422, "Invalid use of builtin method set, usage: set(entity, params..) or set(entity, map)");
			}

			if (propertyMap != null) {
				sourceObject.setProperties(securityContext, propertyMap);
			}

		} catch (ArgumentNullException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());

		} catch (ArgumentCountException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}

		return "";
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${set(entity, propertyKey1, value1, ...)} or ${set(entity, propertyMap)}."),
			Usage.javaScript("Usage: ${{ $.set(entity, propertyMap) }} or ${{ $.set(entity, propertyKey1, value1, ...)}}.")
		);
	}

	@Override
	public String getShortDescription() {
		return "Sets a value or multiple values on an entity. The values can be provided as a map or as a list of alternating keys and values.";
	}

	@Override
	public String getLongDescription() {
		return """
		Sets the passed values for the given property keys on the specified entity, using the security context of the current user.
		`set()` accepts several different parameter combinations, where the first parameter is always a graph object. 
		The second parameter can either be a list of (key, value) pairs, a JSON-coded string (to accept return values of the 
		`geocode()` function) or a map (e.g. a result from nested function calls or a simple map built in serverside JavaScript).
		""";
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
				Example.structrScript("${set(user, 'name', 'new-user-name', 'eMail', 'new@email.com')}"),
				Example.structrScript("${set(page, 'name', 'my-page-name')}"),
				Example.javaScript("""
						${{
						    let me = $.me;
						    $.set(me, {name: 'my-new-name', eMail: 'new@email.com'});
						}}
						""")
		);
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
				Parameter.mandatory("entity", "node to be updated"),
				Parameter.mandatory("map", "parameterMap (only JavaScript)"),
				Parameter.mandatory("key1", "key1 (only StructrScript)"),
				Parameter.mandatory("value1", "value for key1 (only StructrScript)"),
				Parameter.mandatory("key2", "key2 (only JavaScript)"),
				Parameter.mandatory("value2", "value for key1 (only StructrScript)"),
				Parameter.mandatory("keyN", "keyN (only JavaScript)"),
				Parameter.mandatory("valueN", "value for keyN (only StructrScript)")
				);
	}

	@Override
	public List<String> getNotes() {
		return List.of(
				"In a StructrScript environment parameters are passed as pairs of `'key1', 'value1'`.",
				"In a JavaScript environment, the function can be used just as in a StructrScript environment. Alternatively it can take a map as the second parameter.",
				"When using the `set()` method on an entity that is not writable by the current user, a 403 Forbidden HTTP error will be thrown. In this case, use `set_privileged()` which will execute the `set()` operation with elevated privileges."
		);
	}
}
