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

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.Query;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.Traits;

/**
 */
public class NotPredicate extends AbstractPredicate {

	@Override
	public void configureQuery(final SecurityContext securityContext, final Traits type, final PropertyKey propertyKey, final Query query, final boolean exact) throws FrameworkException {

		for (final SearchParameter p : parameters) {

			final PropertyKey key = type.key(p.getKey());
			if (key != null) {

				final Object value = p.getValue();

				// check if value is predicate...
				if (value instanceof SearchFunctionPredicate) {

					query.not();
					((SearchFunctionPredicate)value).configureQuery(securityContext, type, key, query, p.isExact());
					query.parent();

				} else {

					query.not();

					if (p.isEmptyPredicate()) {

						query.blank(key);

					} else {

						query.and(key, value, p.isExact());
					}

					query.parent();
				}
			}
		}

		for (final SearchFunctionPredicate p : predicates) {

			query.and();
			query.not();
			p.configureQuery(securityContext, type, propertyKey, query, exact);
			query.parent();
			query.parent();
		}
	}
}
