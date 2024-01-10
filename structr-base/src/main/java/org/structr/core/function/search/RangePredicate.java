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
	public void configureQuery(final SecurityContext securityContext, final Class type, final PropertyKey key, final Query query, final boolean exact) throws FrameworkException {

		if (Occurrence.OPTIONAL.equals(query.getCurrentOccurrence())) {

			query.orRange(key, rangeStart, rangeEnd, includeStart, includeEnd);

		} else {

			query.andRange(key, rangeStart, rangeEnd, includeStart, includeEnd);
		}
	}
}
