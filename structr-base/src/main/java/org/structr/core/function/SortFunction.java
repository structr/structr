/*
 * Copyright (C) 2010-2026 Structr GmbH
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
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.docs.ontology.FunctionCategory;
import org.structr.schema.action.ActionContext;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SortFunction extends CoreFunction {

	@Override
	public String getName() {
		return "sort";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("collection [, sortKey = 'name' [, descending = false ]]");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		if (sources == null || sources.length == 0) {

			return null;
		}

		// default sort key and order
		final String sortKey     = (sources.length > 1 && sources[1] instanceof String) ? (String) sources[1] : "name";
		final boolean descending = (sources.length > 2 && sources[2] != null && "true".equals(sources[2].toString()));

		if (sources.length >= 1) {

			if (sources[0] instanceof Iterable) {

				final List list = Iterables.toList((Iterable)sources[0]);
				if (!list.isEmpty()) {

					final Object firstElement = list.get(0);
					if (firstElement instanceof GraphObject graphObject) {

						final List<GraphObject> sortCollection = (List<GraphObject>)list;

						if (sortKey.contains(".")) {

							final PathResolvingComparator comparator = new PathResolvingComparator(ctx, sortKey, descending);

							// experimental: use path-resolving comparator
							Collections.sort(sortCollection, comparator);

						} else {

							if (sources.length <= 3) {
								// as-documented code path with 1 set of sortKey/descending (supports sortKey = null so we can use the default key descending)

								final Traits type            = graphObject.getTraits();
								final PropertyKey key        = type.key(sortKey);
								final DefaultSortOrder order = new DefaultSortOrder(key, descending);

								Collections.sort(sortCollection, order);

							} else {

								// hidden functionality: code path with multiple set of sortKey/descending (all sortKeys must exist, otherwise we get errors)

								final DefaultSortOrder order           = new DefaultSortOrder();
								final Traits type                      = graphObject.getTraits();

								for (int i = 1; i < sources.length; i += 2) {

									final String name        = (String)sources[i];
									final PropertyKey key    = type.key(name);
									final boolean keyDescending = sources.length > i+1 && sources[i+1] != null && "true".equals(sources[i+1].toString());

									order.addElement(key, keyDescending);
								}

								if (!order.isEmpty()) {

									Collections.sort(sortCollection, order);
								}
							}
						}

						return sortCollection;

					} else if (firstElement instanceof String) {

						final Comparator<String> order = descending ? Comparator.reverseOrder() : Comparator.naturalOrder();

						list.sort(order);

						return list;

					} else {

						logger.error("{}(): Only collections of nodes or strings are supported. Returning input as-is. Caller: {}, class: {}", getName(), caller, sources[0].getClass().getSimpleName());
						logger.debug("{}(): Parameters: {}", getName(), getParametersAsString(sources));
					}
				}
			}

		} else {

			logParameterError(caller, sources, ctx.isJavaScriptContext());
		}

		return sources[0];
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
				Usage.structrScript("Usage: ${sort(collection, [ sortKey = 'name' [, descending = false ]])}."),
				Usage.javaScript("Usage: ${{ $.sort(collection, [ sortKey = 'name' [, descending = false ]]) }}.")
		);
	}

	@Override
	public String getShortDescription() {
		return "Sorts a collection and returns a new sorted collection.";
	}

	@Override
	public String getLongDescription() {
		return """
			Supported collection types:

			* **Collection of nodes**: Sorted by `sortKey`. The `descending` flag controls the order.
			* **Collection of strings**: Sorted lexicographically. The `sortKey` is ignored. The `descending` flag controls the order.
			""";
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
				Example.structrScript("${extract(sort(find('User'), 'name'), 'name')}"),
				Example.structrScript("${extract(sort(find('User'), 'name', true), 'name')}"),
				Example.javaScript("${{ $.sort($.find('User'), 'name') }}")
		);
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
				Parameter.mandatory("collection", "collection to be sorted"),
				Parameter.optional("sortKey", "property name used for sorting (applies only to collections of nodes). Default: `name`"),
				Parameter.optional("descending", "if `true`, sorts in descending order; otherwise ascending. Default: `false`")
		);
	}

	@Override
	public List<String> getNotes() {
		return List.of(
				"This function is often used in conjunction with `find()`",
				"The `sort()` and `find()` functions are often used in repeater elements in a function query, see Repeater Elements.",
				"Do not use JavaScript built-in function `sort` on remote collections for a node because it mutates the collection which leads to database writes and can lead to undesired effects."
		);
	}

	@Override
	public FunctionCategory getCategory() {
		return FunctionCategory.Collection;
	}
}
