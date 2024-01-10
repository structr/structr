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

import org.structr.api.search.Occurrence;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.Query;
import org.structr.core.property.PropertyKey;

/**
 */
public class EqualsPredicate extends AbstractPredicate {

	private Object value = null;

	public EqualsPredicate(final Object value) {
		this.value = value;
	}

	@Override
	public void configureQuery(final SecurityContext securityContext, final Class type, final PropertyKey propertyKey, final Query query, final boolean exact) throws FrameworkException {

		if (propertyKey.isCollection()) {

			query.and(propertyKey, value, true, Occurrence.EXACT);

		} else {

			query.and(propertyKey, value, true);
		}
	}
}
