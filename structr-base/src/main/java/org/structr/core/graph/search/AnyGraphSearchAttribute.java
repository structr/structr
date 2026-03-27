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
package org.structr.core.graph.search;

import org.structr.api.search.AnyGraphQuery;
import org.structr.core.GraphObject;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;

import java.util.*;

public class AnyGraphSearchAttribute<T> extends GraphSearchAttribute<T> implements AnyGraphQuery {

	public AnyGraphSearchAttribute(final PropertyKey<T> key, final T value, final boolean exact) {

		super(Traits.of(StructrTraits.GRAPH_OBJECT).key(GraphObjectTraitDefinition.ID_PROPERTY), key, value, exact);
	}

	@Override
	public boolean includeInResult(final GraphObject entity) {

		final LinkedHashSet<Object> entityValues = new LinkedHashSet<>();
		final Set<Object> searchValues = new LinkedHashSet<>();

		collect(entityValues, entity.getProperty(getKey()));
		collectNoFlatten(searchValues, getValue(), true);

		// System.out.println("Comparing entity values " + entityValues + " to search values " + searchValues + " , (isExactMatch ? "exactly" : "INexactly"))");

		if (searchValues.isEmpty() && entityValues.isEmpty()) {
			return true;
		}

		if (isExactMatch) {

			// ANY of the list of searchValues must equal the entityValue(s)
			if (getKey().isCollection()) {

				// one of the search values must be equal to the entity value
				for (final Object searchVal : searchValues) {

					// for collection properties, every search parameter should be a collection - if not, it can not be equal
					if (searchVal instanceof Collection<?> searchValCollection) {

						// sizes must be equal and the elements must be the same
						if (searchValCollection.size() == entityValues.size() && entityValues.containsAll(searchValCollection)) {
							return true;
						}
					}
				}

				return false;

			} else {

				// entityValues is just one element and searchValues should also only be single elements, not collections
				return searchValues.contains(entityValues.getFirst());
			}

		} else {

			if (getKey().isCollection()) {

				// one of the search values must be equal to the entity value
				for (final Object searchVal : searchValues) {

					// for collection properties, every search parameter should be a collection - if not, it can not be equal
					if (searchVal instanceof Collection<?> searchValCollection) {

						// all searchValues must be contained in the entityCollection
						if (entityValues.containsAll(searchValCollection)) {
							return true;
						}
					}
				}

				return false;

			} else {

				return searchValues.contains(entityValues.getFirst());
			}
		}
	}

	// ----- private methods -----
	private void collectNoFlatten(final Set<Object> values, final Object value, final boolean isFirstLevel) {

		if (value instanceof Iterable) {

			if (isFirstLevel) {

				for (final Object o : (Iterable)value) {

					collectNoFlatten(values, o, false);
				}

			} else {

				final Set<Object> subSet = new LinkedHashSet<>();
				values.add(subSet);

				for (final Object o : (Iterable)value) {

					collectNoFlatten(subSet, o, false);
				}
			}

		} else if (value instanceof GraphObject) {

			values.add(((GraphObject)value).getProperty(notionKey));

		} else if (value != null) {

			values.add(value);
		}
	}
}
