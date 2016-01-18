/**
 * Copyright (C) 2010-2016 Structr GmbH
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
package org.structr.core.parser.function;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.LinkedHashMap;
import java.util.Map;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class SetFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_SET = "Usage: ${set(entity, propertyKey, value)}. Example: ${set(this, \"email\", lower(this.email))}";

	@Override
	public String getName() {
		return "set()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		if (arrayHasMinLengthAndAllElementsNotNull(sources, 2)) {

			if (sources[0] instanceof GraphObject) {

				final GraphObject source = (GraphObject)sources[0];
				final Map<String, Object> properties = new LinkedHashMap<>();
				final SecurityContext securityContext = source.getSecurityContext();
				final Gson gson = new GsonBuilder().create();
				final Class type = source.getClass();
				final int sourceCount = sources.length;

				if (sources.length == 3 && sources[2] != null && sources[1].toString().matches("[a-zA-Z0-9_]+")) {

					properties.put(sources[1].toString(), sources[2]);

				} else {

					// we either have and odd number of items, or two multi-value items.
					for (int i = 1; i < sourceCount; i++) {

						final Map<String, Object> values = deserialize(gson, sources[i].toString());
						if (values != null) {

							properties.putAll(values);
						}
					}
				}

				// store values in entity
				final PropertyMap map = PropertyMap.inputTypeToJavaType(securityContext, type, properties);
				for (final Map.Entry<PropertyKey, Object> entry : map.entrySet()) {

					source.setProperty(entry.getKey(), entry.getValue());
				}

			} else {

				throw new FrameworkException(422, "Invalid use of builtin method set, usage: set(entity, params..)");
			}

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
