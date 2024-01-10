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

import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.GraphObjectMap;
import org.structr.core.property.PropertyKey;
import org.structr.schema.action.ActionContext;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

public class KeysFunction extends CoreFunction {

	public static final String ERROR_MESSAGE_KEYS = "Usage: ${keys(entity [, viewName])}. Example: ${keys(this, \"ui\")}";
	public static final String ERROR_MESSAGE_KEYS_JS = "Usage: ${{Structr.keys(entity [, viewName])}}. Example: ${{Structr.keys(this, \"ui\")}}";

	@Override
	public String getName() {
		return "keys";
	}

	@Override
	public String getSignature() {
		return "entity [, viewName ]";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			if (sources == null) {
				throw new IllegalArgumentException();
			}

			if (sources.length == 2 && sources[0] != null && sources[0] instanceof GraphObject && sources[1] != null) {

				final Set<String> keys = new LinkedHashSet<>();
				final GraphObject source = (GraphObject) sources[0];

				for (final PropertyKey key : source.getPropertyKeys(sources[1].toString())) {
					keys.add(key.jsonName());
				}

				return new LinkedList<>(keys);

			} else if (sources.length == 1 && sources[0] != null && sources[0] instanceof GraphObjectMap) {

				return new LinkedList<>(((GraphObjectMap) sources[0]).keySet());

			} else if (sources.length == 1 && sources[0] != null && sources[0] instanceof Map) {

				return new LinkedList<>(((Map)sources[0]).keySet());
			} else {

				return null;
			}

		} catch (final IllegalArgumentException e) {

			logParameterError(caller, sources, ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());

		}
	}


	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_KEYS_JS : ERROR_MESSAGE_KEYS);
	}

	@Override
	public String shortDescription() {
		return "Returns the property keys of the given entity";
	}

}
