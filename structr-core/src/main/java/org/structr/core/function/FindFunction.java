/**
 * Copyright (C) 2010-2019 Structr GmbH
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

import java.util.Map;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.Query;
import org.structr.core.app.StructrApp;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.schema.ConfigurationProvider;
import org.structr.schema.action.ActionContext;

/**
 *
 */
public class FindFunction extends AbstractQueryFunction {

	public static final String ERROR_MESSAGE_FIND = "Usage: ${find(type, key, value)}. Example: ${find(\"User\", \"email\", \"tester@test.com\"}";
	public static final String ERROR_MESSAGE_FIND_NO_TYPE_SPECIFIED = "Error in find(): no type specified.";
	public static final String ERROR_MESSAGE_FIND_TYPE_NOT_FOUND = "Error in find(): type not found: ";

	@Override
	public String getName() {
		return "find";
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
			final Query query                     = app.nodeQuery().sort(GraphObject.createdDate).order(false);

			// paging applied by surrounding slice() function
			applyRange(securityContext, query);

			// the type to query for
			Class type = null;

			if (sources.length >= 1 && sources[0] != null) {

				final String typeString = sources[0].toString();
				type = config.getNodeEntityClass(typeString);

				if (type != null) {

					query.andTypes(type);

				} else {

					logger.warn("Error in find(): type \"{}\" not found.", typeString);
					return ERROR_MESSAGE_FIND_TYPE_NOT_FOUND + typeString;

				}
			}

			// exit gracefully instead of crashing..
			if (type == null) {
				logger.warn("Error in find(): no type specified. Parameters: {}", getParametersAsString(sources));
				return ERROR_MESSAGE_FIND_NO_TYPE_SPECIFIED;
			}

			// extension for native javascript objects
			if (sources.length == 2 && sources[1] instanceof Map) {

				query.and(PropertyMap.inputTypeToJavaType(securityContext, type, (Map)sources[1]));

			} else if (sources.length == 2) {

				if (sources[1] == null) {

					throw new IllegalArgumentException();
				}

				// special case: second parameter is a UUID
				final PropertyKey key = StructrApp.key(type, "id");

				query.and(key, sources[1].toString());

				return query.getFirst();

			} else {

				final int parameter_count = sources.length;

				if (parameter_count % 2 == 0) {

					throw new FrameworkException(400, "Invalid number of parameters: " + parameter_count + ". Should be uneven: " + ERROR_MESSAGE_FIND);
				}

				for (int c = 1; c < parameter_count; c += 2) {

					if (sources[c] == null) {
						throw new IllegalArgumentException();
					}

					final PropertyKey key = StructrApp.key(type, sources[c].toString());

					if (key != null) {

						final PropertyConverter inputConverter = key.inputConverter(securityContext);
						Object value = sources[c + 1];

						if (inputConverter != null) {

							value = inputConverter.convert(value);
						}

						query.and(key, value);
					}
				}
			}

			return query.getAsList();

		} catch (final IllegalArgumentException e) {

			logParameterError(caller, sources, ctx.isJavaScriptContext());

			return usage(ctx.isJavaScriptContext());

		} finally {
			resetRange();
		}
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_FIND;
	}

	@Override
	public String shortDescription() {
		return "Returns a collection of entities of the given type from the database, takes optional key/value pairs";
	}
}
