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
import org.structr.core.converter.PropertyConverter;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.Traits;

/**
 */
public class RangePredicate implements SearchFunctionPredicate {

	private Object rangeStart    = null;
	private Object rangeEnd      = null;
	private boolean includeStart = true;
	private boolean includeEnd   = true;

	public RangePredicate(final Object rangeStart, final Object rangeEnd, final boolean includeStart, final boolean includeEnd) {

		this.rangeStart   = rangeStart;
		this.rangeEnd     = rangeEnd;
		this.includeStart = includeStart;
		this.includeEnd   = includeEnd;
	}

	@Override
	public void configureQuery(final SecurityContext securityContext, final Traits type, final PropertyKey key, final QueryGroup query, final boolean exact) throws FrameworkException {

		final Object effectiveRangeStart = convertIfNecessary(securityContext, key, rangeStart);
		final Object effectiveRangeEnd   = convertIfNecessary(securityContext, key, rangeEnd);

		/*
		if (Operation.OR.equals(query.getOperation())) {

			query.orRange(key, effectiveRangeStart, effectiveRangeEnd, includeStart, includeEnd);

		} else {


		 */
			query.range(key, effectiveRangeStart, effectiveRangeEnd, includeStart, includeEnd);
		//}
	}

	/**
	 * Converts the given range bound into the value type of the given key, in case the
	 * bound does not already have a compatible type. Neither a value type (e.g. generic
	 * and cypher properties) nor an input converter is guaranteed to exist, so the bound
	 * is passed through unchanged whenever we cannot do better.
	 */
	private Object convertIfNecessary(final SecurityContext securityContext, final PropertyKey key, final Object bound) throws FrameworkException {

		if (key == null || bound == null) {

			return bound;
		}

		final Class valueType = key.valueType();
		if (valueType == null || valueType.isAssignableFrom(bound.getClass())) {

			return bound;
		}

		final PropertyConverter converter = key.inputConverter(securityContext, false);
		if (converter == null) {

			return bound;
		}

		final Object converted = converter.convert(bound);
		if (converted == null) {

			return bound;
		}

		return converted;
	}
}
