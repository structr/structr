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
package org.structr.core.function.search;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.QueryGroup;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.Traits;

/**
 */
public class StartsWithPredicate extends AbstractPredicate {

	private String key    = null;
	private Object value  = null;

	public StartsWithPredicate(final String key, final Object value) {

		this.key   = key;
		this.value = value;
	}

	@Override
	public void configureQuery(final SecurityContext securityContext, final Traits type, final PropertyKey propertyKey, final QueryGroup query, final boolean exact) throws FrameworkException {

		/***
		 * The predicate can be used in two different ways.
		 * 1. $.find('Type', { name: $.predicate.startsWith('abc') }) => key is null but propertyKey should be set
		 * 2. $.find('Type', $.predicate.startsWith('name', 'abc'))   => key is not null but propertyKey should be null
		 */

		if (propertyKey == null) {

			if (key != null) {

				final PropertyKey k = type.key(key);
				if (k != null) {

					query.startsWith(k, value, exact);
				}

			} else {

				// error!
			}

		} else {

			if (key == null) {

				query.startsWith(propertyKey, value, exact);

			} else {

				// error!
			}
		}
	}
}
