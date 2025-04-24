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
import org.structr.core.app.QueryGroup;
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

		Object effectiveRangeStart = rangeStart;

		if (key != null && rangeStart != null && !key.valueType().isAssignableFrom(rangeStart.getClass())) {
			Object converted = key.inputConverter(securityContext, false).convert(rangeStart);
			if (converted != null) {
				effectiveRangeStart = converted;
			};
		}

		Object effectiveRangeEnd = rangeEnd;

		if (key != null && rangeEnd != null && !key.valueType().isAssignableFrom(rangeEnd.getClass())) {
			Object converted = key.inputConverter(securityContext, false).convert(rangeEnd);
			if (converted != null) {
				effectiveRangeEnd = converted;
			}
		}

		/*
		if (Operation.OR.equals(query.getOperation())) {

			query.orRange(key, effectiveRangeStart, effectiveRangeEnd, includeStart, includeEnd);

		} else {


		 */
			query.range(key, effectiveRangeStart, effectiveRangeEnd, includeStart, includeEnd);
		//}
	}
}
