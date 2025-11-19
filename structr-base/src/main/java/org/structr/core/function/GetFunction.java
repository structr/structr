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


import jakarta.servlet.http.HttpServletRequest;
import org.structr.api.config.Settings;
import org.structr.api.util.Iterables;
import org.structr.common.SecurityContext;
import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.GraphObjectMap;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.Traits;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;

import java.util.List;
import java.util.Map;

public class GetFunction extends CoreFunction {

	public static final String ERROR_MESSAGE_GET_ENTITY = "Cannot evaluate first argument to entity, must be entity or single element list of entities.";

	@Override
	public String getName() {
		return "get";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllLanguages("entity, propertyName");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		final SecurityContext securityContext    = ctx.getSecurityContext();

		try {

			assertArrayHasLengthAndAllElementsNotNull(sources, 2);

			final String keyName                           = sources[1].toString();
			GraphObject dataObject                         = null;

			// handle GraphObject
			if (sources[0] instanceof GraphObject) {
				dataObject = (GraphObject)sources[0];
			}

			// handle first element of a list of graph objects
			if (sources[0] instanceof Iterable) {

				final List list = Iterables.toList((Iterable)sources[0]);
				final int size  = list.size();

				if (size == 1) {

					final Object value = list.get(0);
					if (value != null) {

						if (value instanceof GraphObject) {

							dataObject = (GraphObject)list.get(0);

						} else {

							return "get(): first element of collection is of type " + value.getClass() + " which is not supported.";
						}

					} else {

						return "get(): first element of collection is null.";
					}
				}
			}

			// handle map separately
			if (sources[0] instanceof Map && !(sources[0] instanceof GraphObjectMap)) {

				final Map map = (Map)sources[0];
				return map.get(keyName);
			}

			// handle request object
			if (sources[0] instanceof HttpServletRequest) {

				final HttpServletRequest request = (HttpServletRequest)sources[0];
				return request.getParameter(keyName);
			}

			if (dataObject != null) {

				final Traits traits = dataObject.getTraits();
				final boolean useGenericPropertyForUnknownKeys = Settings.AllowUnknownPropertyKeys.getValue(false) || dataObject instanceof GraphObjectMap;

				final PropertyKey key = (useGenericPropertyForUnknownKeys ? traits.keyOrGenericProperty(keyName) : traits.key(keyName));

				if (key != null) {

					final PropertyConverter inputConverter = key.inputConverter(securityContext, false);
					Object value = dataObject.getProperty(key);

					if (inputConverter != null) {
						return inputConverter.revert(value);
					}

					return dataObject.getProperty(key);

				} else {

					// key does not exist and generic property is not desired => log warning
					logger.warn("get(): Unknown property {}.{}, value will not be returned. [{}]", traits.getName(), keyName, dataObject.getUuid());
				}

				return "";

			} else {

				return ERROR_MESSAGE_GET_ENTITY;
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
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${get(entity, propertyKey)}. Example: ${get(this, 'children')}"),
			Usage.javaScript("Usage: ${{ $.get(entity, propertyKey) }}. Example: ${{ $.get($.this, 'children')}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Returns the value with the given name of the given entity, or an empty string.";
	}

	@Override
	public String getLongDescription() {
		return """
		Returns the value for the given property key from the given entity. 
		This method will print an error message if the first parameter is null / not accessible. 
		See `get_or_null()` for a more tolerant get method.
		""";
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
				Example.javaScript("${get(page, 'name')}"),
				Example.structrScript("$.get(page, 'name')")
		);
	}

	@Override
	public List<String> getNotes() {
		return List.of(
				"The result value of the get() method can differ from the result value of property access using the dot notation (`get(this, 'name')` vs `this.name`) for certain property types (e.g. date properties), because get() converts the property value to its output representation.",
				"That means that a Date object will be formatted into a string when fetched via `get(this, 'date')`, whereas `this.date` will return an actual date object."
		);
	}


	@Override
	public List<Parameter> getParameters() {

		return List.of(
				Parameter.mandatory("entity", "node or object"),
				Parameter.mandatory("propertyKey", "requested property name")
				);
	}

}
