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

import java.util.Collections;
import java.util.List;
import org.structr.api.util.Iterables;
import org.structr.common.GraphObjectComparator;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.property.PropertyKey;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class SortFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_SORT = "Usage: ${sort(list1, [key [, descending=false]])}. Example: ${sort(this.children, \"name\")}";

	@Override
	public String getName() {
		return "sort";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		if (sources == null || sources.length == 0) {

			return null;
		}

		// Default sort key
		String sortKey = "name";

		if (sources.length > 1 && sources[1] instanceof String) {

			sortKey = (String) sources[1];
		}

		if (sources.length >= 1) {

			if (sources[0] instanceof Iterable) {

				final List list = Iterables.toList((Iterable)sources[0]);
				if (!list.isEmpty()) {

					final Object firstElement = list.get(0);
					if (firstElement instanceof GraphObject) {

						final Class type         = firstElement.getClass();
						final PropertyKey key    = StructrApp.getConfiguration().getPropertyKeyForJSONName(type, sortKey);
						final boolean descending = sources.length == 3 && sources[2] != null && "true".equals(sources[2].toString());

						if (key != null) {

							List<GraphObject> sortCollection = (List<GraphObject>)list;
							Collections.sort(sortCollection, new GraphObjectComparator(key, descending));

							return sortCollection;
						}

					} else if (firstElement instanceof String) {

						Collections.sort(list);
					}
				}
			}

		} else {

			logParameterError(caller, sources, ctx.isJavaScriptContext());
		}

		return sources[0];
	}


	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_SORT;
	}

	@Override
	public String shortDescription() {
		return "Sorts the given collection or array according to the given property key. Default sort key is 'name'.";
	}

}
