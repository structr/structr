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
package org.structr.core.function.search;

import org.structr.common.SecurityContext;
import org.structr.common.SortKeysComparator;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.QueryGroup;
import org.structr.core.function.SortKeyArguments.SortKey;
import org.structr.core.graph.search.DefaultSortOrder;
import org.structr.core.graph.search.SortKeysSortOrder;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.Traits;
import org.structr.schema.action.ActionContext;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * The sort predicate of find(), with one or more sort keys. If all keys are properties of the
 * results themselves, without natural order and without an explicit position of nulls, the database
 * sorts. Otherwise, for a path through related objects, natural order or nulls first or last, the
 * complete result is sorted in Java before it is paged. Several sort predicates of one query are
 * combined by the query, see SearchCommand.
 */
public class SortKeysPredicate extends AbstractPredicate {

	private final List<SortKey> keys;
	private final Locale locale;

	/**
	 * @param locale the locale of natural order, the one of the script that creates the predicate
	 */
	public SortKeysPredicate(final List<SortKey> keys, final Locale locale) {

		this.keys   = keys;
		this.locale = locale;
	}

	@Override
	public void configureQuery(final SecurityContext securityContext, final Traits type, final PropertyKey propertyKey, final QueryGroup query, final boolean exact) throws FrameworkException {

		final Set<String> types = getTypeAndSubtypes(type.getName());

		// plain keys are checked when they are resolved below
		for (final SortKey key : keys) {

			if (key.key().contains(".")) {

				assertPathIsKnown(types, key.key());
			}
		}

		if (keys.stream().allMatch(SortKey::isSortableByDatabase)) {

			for (final SortKey key : keys) {

				query.sort(resolveKey(type, key.key()), key.descending());
			}

		} else {

			final ActionContext actionContext = new ActionContext(securityContext);

			actionContext.setLocale(locale);

			final SortKeysComparator comparator = new SortKeysComparator(actionContext);

			for (final SortKey key : keys) {

				if (key.key().contains(".")) {

					comparator.addPath(key.key(), key.descending(), key.natural(), key.nulls());

				} else {

					comparator.addKey(resolveKey(type, key.key()), key.descending(), key.natural(), key.nulls());
				}
			}

			query.sort(new SortKeysSortOrder(comparator));
		}
	}

	/**
	 * Rejects a path with an unknown key like an unknown sort key, instead of returning an unsorted
	 * result. The objects on the path can be of any subtype of the declared type, so a key counts as
	 * known if one of them has it. The check ends where the type of the next objects is not known
	 * before the query runs. Also used by the sort() function, which starts with the types of the
	 * objects it sorts.
	 */
	public static void assertPathIsKnown(final Set<String> startTypes, final String sortPath) throws FrameworkException {

		Set<String> types = startTypes;

		for (final String part : sortPath.split("[\\.]+")) {

			final Set<String> nextTypes = new LinkedHashSet<>();
			boolean known               = false;
			boolean nextTypesKnown      = true;

			for (final String name : types) {

				final Traits traits = Traits.of(name);
				if (traits.hasKey(part)) {

					final String relatedType = traits.key(part).relatedType();

					known = true;

					if (relatedType != null && Traits.exists(relatedType)) {

						nextTypes.addAll(getTypeAndSubtypes(relatedType));

					} else {

						nextTypesKnown = false;
					}
				}
			}

			if (!known) {

				throw new FrameworkException(422, "Unknown sort key '" + part + "' in path '" + sortPath + "'");
			}

			if (!nextTypesKnown) {

				return;
			}

			types = nextTypes;
		}
	}

	public static Set<String> getTypeAndSubtypes(final String name) {

		final Set<String> types = new LinkedHashSet<>();

		types.add(name);
		types.addAll(Traits.getAllTypes(traits -> traits.contains(name)));

		return types;
	}

	// ----- private methods -----
	// a key of a subtype is known too, because the result can contain objects of that subtype
	private PropertyKey resolveKey(final Traits type, final String name) throws FrameworkException {

		final PropertyKey key = DefaultSortOrder.resolveSortKey(type.getName(), name);
		if (key == null) {

			throw new FrameworkException(422, "Unknown sort key '" + name + "' for type " + type.getName());
		}

		return key;
	}
}
