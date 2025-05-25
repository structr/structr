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
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.Traits;
import org.structr.schema.ConfigurationProvider;
import org.structr.schema.action.ActionContext;

import java.util.Map;

public class CreateOrUpdateFunction extends CoreFunction {

	public static final String ERROR_MESSAGE_CREATE_OR_UPDATE  = "Usage: ${create_or_update(type, properties)}. Example: ${create_or_update(\"User\", \"email\", \"tester@test.com\"}";
	public static final String ERROR_MESSAGE_NO_TYPE_SPECIFIED = "Error in create_or_update(): no type specified.";
	public static final String ERROR_MESSAGE_TYPE_NOT_FOUND    = "Error in create_or_update(): type not found: ";

	@Override
	public String getName() {
		return "create_or_update";
	}

	@Override
	public String getSignature() {
		return "type, propertyMap";
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

					logger.warn("Error in create_or_update(): type \"{}\" not found.", typeString);
					return ERROR_MESSAGE_TYPE_NOT_FOUND + typeString;
				}
			}

			// exit gracefully instead of crashing..
			if (type == null) {

				logger.warn("Error in create_or_update(): no type specified. Parameters: {}", getParametersAsString(sources));
				return ERROR_MESSAGE_NO_TYPE_SPECIFIED;
			}

			// extension for native javascript objects
			if (sources.length == 2 && sources[1] instanceof Map) {

				properties.putAll(PropertyMap.inputTypeToJavaType(securityContext, type.getName(), (Map)sources[1]));

			} else {

				final int parameter_count = sources.length;

				if (parameter_count % 2 == 0) {

					throw new FrameworkException(400, "Invalid number of parameters: " + parameter_count + ". Should be uneven: " + ERROR_MESSAGE_CREATE_OR_UPDATE);
				}

				for (int c = 1; c < parameter_count; c += 2) {

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
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_CREATE_OR_UPDATE;
	}

	@Override
	public String shortDescription() {
		return "Creates an object with the given properties or updates an existing object if it could be identified by a unique property.";
	}
}
