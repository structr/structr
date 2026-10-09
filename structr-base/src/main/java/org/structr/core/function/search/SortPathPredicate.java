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
import org.structr.common.error.FrameworkException;
import org.structr.core.app.QueryGroup;
import org.structr.core.graph.search.PathPropertySortOrder;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.Traits;
import org.structr.schema.action.ActionContext;

import java.util.LinkedHashSet;
import java.util.Set;

public class SortPathPredicate extends AbstractPredicate {

	private boolean sortDescending = false;
	private String sortPath     = null;

	public SortPathPredicate(final String sortPath, final boolean sortDescending) {

		this.sortPath    = sortPath;
		this.sortDescending = sortDescending;
	}

	@Override
	public void configureQuery(final SecurityContext securityContext, final Traits type, final PropertyKey propertyKey, final QueryGroup query, final boolean exact) throws FrameworkException {

		assertPathIsKnown(type);

		query.sort(new PathPropertySortOrder(new ActionContext(securityContext), sortPath, sortDescending));
	}

	/**
	 * Rejects a path with an unknown key like SortPredicate rejects an unknown sort key, instead of returning an
	 * unsorted result. The objects on the path can be of any subtype of the declared type, so a key counts as known
	 * if one of them has it. The check ends where the type of the next objects is not known before the query runs.
	 */
	private void assertPathIsKnown(final Traits type) throws FrameworkException {

		Set<String> types = getTypeAndSubtypes(type.getName());

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

	private Set<String> getTypeAndSubtypes(final String name) {

		final Set<String> types = new LinkedHashSet<>();

		types.add(name);
		types.addAll(Traits.getAllTypes(traits -> traits.contains(name)));

		return types;
	}
}
