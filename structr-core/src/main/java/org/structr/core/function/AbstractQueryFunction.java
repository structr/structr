/**
 * Copyright (C) 2010-2018 Structr GmbH
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
package org.structr.core.function;

import org.structr.core.app.Query;
import org.structr.schema.action.Function;

/**
 * Abstract implementation of the basic functions of the Interface QueryFunction.
 */
public abstract class AbstractQueryFunction extends Function<Object, Object> implements QueryFunction {

	private int start = -1;
	private int end   = -1;

	// ----- interface QueryFunction -----
	@Override
	public void setRangeStart(final int start) {
		this.start = start;
	}

	@Override
	public void setRangeEnd(final int end) {
		this.end = end;
	}

	@Override
	public void applyRange(final Query query) {

		// paging applied by surrounding slice() function
		if (start >= 0 && end >= 0) {

			query.getQueryContext().slice(start, end);
		}
	}

	@Override
	public void resetRange() {

		this.start = -1;
		this.end   = -1;
	}
}
