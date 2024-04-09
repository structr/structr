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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.structr.api.config.Settings;
import org.structr.common.SecurityContext;
import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.GraphObjectMap;
import org.structr.core.app.StructrApp;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.schema.ConfigurationProvider;
import org.structr.schema.action.ActionContext;

import java.util.Map;

public class SetFunction extends CoreFunction {

	public static final String ERROR_MESSAGE_SET = "Usage: ${set(entity, propertyKey, value, ...)}. Example: ${set(this, \"email\", lower(this.email))}";
	public static final String ERROR_MESSAGE_SET_JS = "Usage: ${{Structr.set(entity, propertyKey, value, ...)}}. Example: ${{Structr.set(this, \"email\", lower(this.email))}}";

	@Override
	public String getName() {
		return "set";
	}

	@Override
	public String getSignature() {
		return "entity, parameterMap";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndAllElementsNotNull(sources, 2);

			final boolean useGenericPropertyForUnknownKeys = Settings.AllowUnknownPropertyKeys.getValue(false);
			final SecurityContext securityContext          = ctx.getSecurityContext();
			final ConfigurationProvider config             = StructrApp.getConfiguration();

			Class type = null;
			PropertyMap propertyMap = null;

			if (sources[0] instanceof GraphObject) {

				final GraphObject source = (GraphObject) sources[0];
				type = source.getEntityType();
			}

			if (type == null) {

				throw new FrameworkException(422, "Can't get type of object '" + sources[0].toString() + "' in set() method!");
			}

			final GraphObject sourceObject = (GraphObject) sources[0];

			if (sources.length == 2 && sources[1] instanceof Map) {

				propertyMap = PropertyMap.inputTypeToJavaType(securityContext, type, (Map) sources[1]);

			} else if (sources.length == 2 && sources[1] instanceof GraphObjectMap) {

				propertyMap = PropertyMap.inputTypeToJavaType(securityContext, type, ((GraphObjectMap)sources[1]).toMap());

			} else if (sources.length == 2 && sources[1] instanceof String) {

				final Gson gson = new GsonBuilder().create();

				final Map<String, Object> values = deserialize(gson, sources[1].toString());
				if (values != null) {

					propertyMap = PropertyMap.inputTypeToJavaType(securityContext, type, values);

				}

			} else if (sources.length > 2) {

				propertyMap               = new PropertyMap();
				final int parameter_count = sources.length;

				if (parameter_count % 2 == 0) {

					throw new FrameworkException(400, "Invalid number of parameters: " + parameter_count + ". Should be uneven: " + (ctx.isJavaScriptContext() ? ERROR_MESSAGE_SET_JS : ERROR_MESSAGE_SET));
				}

				for (int c = 1; c < parameter_count; c += 2) {

					final String keyName  = sources[c].toString();
					final PropertyKey key = config.getPropertyKeyForJSONName(type, keyName, useGenericPropertyForUnknownKeys);

					if (key != null) {

						final PropertyConverter inputConverter = key.inputConverter(securityContext);
						Object value = sources[c + 1];

						if (inputConverter != null) {

							value = inputConverter.convert(value);
						}

						propertyMap.put(key, value);

					} else {

						// key does not exist and generic property is not desired => log warning
						logger.warn("Unknown property {}.{}, value will not be set.", type.getSimpleName(), keyName);
					}
				}

			} else {

				throw new FrameworkException(422, "Invalid use of builtin method set, usage: set(entity, params..)");
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
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_SET;
	}

	@Override
	public String shortDescription() {
		return "Sets a value on an entity";
	}
}
