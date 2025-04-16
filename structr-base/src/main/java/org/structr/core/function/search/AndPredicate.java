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
public class AndPredicate extends AbstractPredicate {

	@Override
	public void configureQuery(final SecurityContext securityContext, final Traits type, final PropertyKey propertyKey, final QueryGroup query, final boolean exact) throws FrameworkException {

		final SearchAttributeGroup andGroup = new SearchAttributeGroup(securityContext, query, Operation.AND);

		for (final SearchParameter parameter : parameters) {

			final PropertyKey key = type.key(parameter.getKey());
			if (key != null) {

				final Object value = parameter.getValue();

				// check if value is predicate...
				if (value instanceof SearchFunctionPredicate predicate) {

					predicate.configureQuery(securityContext, type, key, query.and(), parameter.isExact());

				} else {


					if (parameter.isEmptyPredicate()) {

						andGroup.blank(key);

					} else {

						andGroup.key(key, value, parameter.isExact());
					}
				}
			}
		}

		for (final SearchFunctionPredicate p : predicates) {

			p.configureQuery(securityContext, type, propertyKey, andGroup, exact);
		}

		// only add group if it is not empty!
		if (!andGroup.isEmpty()) {
			query.add(andGroup);
		}
	}
}
