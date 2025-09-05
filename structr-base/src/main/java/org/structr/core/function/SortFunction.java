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

import org.structr.api.util.Iterables;
import org.structr.common.PathResolvingComparator;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.graph.search.DefaultSortOrder;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.Traits;
import org.structr.schema.action.ActionContext;

import java.util.Collections;
import java.util.List;

public class SortFunction extends CoreFunction {

	public static final String ERROR_MESSAGE_SORT = "Usage: ${sort(list1, [key [, descending=false]])}. Example: ${sort(this.children, \"name\")}";

	@Override
	public String getName() {
		return "sort";
	}

	@Override
	public String getSignature() {
		return "list [, propertyName [, descending=false] ]";
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
					if (firstElement instanceof GraphObject graphObject) {

						final List<GraphObject> sortCollection = (List<GraphObject>)list;
						final int length                       = sources.length;

						if (sources.length <= 3 && sortKey.contains(".")) {

							final boolean descending = length > 2 && sources[2] != null && "true".equals(sources[2].toString());
							final PathResolvingComparator comparator = new PathResolvingComparator(ctx, sortKey, descending);

							// experimental: use path-resolving comparator
							Collections.sort(sortCollection, comparator);

						} else {

							final DefaultSortOrder order           = new DefaultSortOrder();
							final Traits type                      = graphObject.getTraits();

							for (int i=1; i<length; i+=2) {

								final String name        = (String)sources[i];
								final PropertyKey key    = type.key(name);
								final boolean descending = length > i+1 && sources[i+1] != null && "true".equals(sources[i+1].toString());

								order.addElement(key, descending);
							}

							if (!order.isEmpty()) {

								Collections.sort(sortCollection, order);
							}

						}

						return sortCollection;

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
