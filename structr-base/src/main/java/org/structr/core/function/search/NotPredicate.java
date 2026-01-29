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

import org.structr.api.search.Operation;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.QueryGroup;
import org.structr.core.graph.search.SearchAttributeGroup;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.Traits;

/**
 */
public class NotPredicate extends AbstractPredicate {

	@Override
	public void configureQuery(final SecurityContext securityContext, final Traits type, final PropertyKey propertyKey, final QueryGroup query, final boolean exact) throws FrameworkException {

		final SearchAttributeGroup notGroup = new SearchAttributeGroup(securityContext, query, Operation.NOT);

		for (final SearchParameter p : parameters) {

			final PropertyKey key = type.key(p.getKey());
			if (key != null) {

				final Object value = p.getValue();

				// check if value is predicate...
				if (value instanceof SearchFunctionPredicate predicate) {

					predicate.configureQuery(securityContext, type, key, notGroup, exact && p.isExact());

				} else {


					if (p.isEmptyPredicate()) {

						notGroup.blank(key);

					} else {

						notGroup.key(key, value, exact && p.isExact());
					}
				}
			}
		}

		for (final SearchFunctionPredicate p : predicates) {

			p.configureQuery(securityContext, type, propertyKey, notGroup, exact);
		}

		// only add group if it is not empty!
		if (!notGroup.isEmpty()) {
			query.add(notGroup);
		}
	}
}
