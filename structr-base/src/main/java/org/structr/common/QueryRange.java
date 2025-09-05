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
package org.structr.common;

import org.structr.api.Predicate;
/**
 *
 *
 */
public class QueryRange implements Predicate {

	private int start = 0;
	private int end   = 0;
	private int count = 0;

	public QueryRange(final int start, final int end) {

		this.start = start;
		this.end   = end;
	}

	// ----- interface Predicate -----
	@Override
	public boolean accept(final Object t) {

		final boolean result = count >= start && count < end;

		count++;

		return result;
	}

	public void resetCount() {
		count = 0;
	}
}
