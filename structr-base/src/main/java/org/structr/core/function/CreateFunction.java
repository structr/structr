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
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObjectMap;
import org.structr.core.app.StructrApp;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.Traits;
import org.structr.schema.ConfigurationProvider;
import org.structr.schema.action.ActionContext;

import java.util.Map;

public class CreateFunction extends CoreFunction {

	public static final String ERROR_MESSAGE_CREATE    = "Usage: ${create(type, key, value)}. Example: ${create(\"Feedback\", \"text\", this.text)}";
	public static final String ERROR_MESSAGE_CREATE_JS = "Usage: ${{Structr.create(type, {key: value})}}. Example: ${{Structr.create(\"Feedback\", {text: \"Structr is awesome.\"})}}";

	@Override
	public String getName() {
		return "create";
	}

	@Override
	public String getSignature() {
		return "type [, parameterMap ]";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		if (sources != null && sources.length > 0) {

			final SecurityContext securityContext = ctx.getSecurityContext();
			PropertyMap propertyMap;
			Traits type = null;

			if (sources.length >= 1 && sources[0] != null) {

				type = Traits.of(sources[0].toString());
			}

			if (type == null) {

				throw new FrameworkException(422, "Unknown type '" + sources[0].toString() + "' in create() method!");
			}

			// extension for native javascript objects
			if (sources.length == 2 && sources[1] instanceof Map) {

				propertyMap = PropertyMap.inputTypeToJavaType(securityContext, type.getName(), (Map)sources[1]);

			} else if (sources.length == 2 && sources[1] instanceof GraphObjectMap) {

				propertyMap = PropertyMap.inputTypeToJavaType(securityContext, type.getName(), ((GraphObjectMap)sources[1]).toMap());

			} else {

				propertyMap               = new PropertyMap();
				final int parameter_count = sources.length;

				if (parameter_count % 2 == 0) {

					throw new FrameworkException(400, "Invalid number of parameters: " + parameter_count + ". Should be uneven: " + usage(ctx.isJavaScriptContext()));
				}

				for (int c = 1; c < parameter_count; c += 2) {

					final PropertyKey key = type.key(sources[c].toString());

					if (key != null) {

						final PropertyConverter inputConverter = key.inputConverter(securityContext);
						Object value = sources[c + 1];

						if (inputConverter != null) {

							value = inputConverter.convert(value);
						}

						propertyMap.put(key, value);
					}

				}
			}

			return StructrApp.getInstance(securityContext).create(type.getName(), propertyMap);

		} else {

			logParameterError(caller, sources, ctx.isJavaScriptContext());

			return usage(ctx.isJavaScriptContext());
		}
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_CREATE_JS : ERROR_MESSAGE_CREATE);
	}

	@Override
	public String shortDescription() {
		return "Creates a new entity with the given key/value pairs in the database";
	}

}
