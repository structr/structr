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

/**
 */
public class SearchParameter {

	protected boolean exact = true;
	protected String key    = null;
	protected Object value  = null;

	public SearchParameter(final String key, final Object value, final boolean exact) {

		this.exact = exact;
		this.key   = key;
		this.value = value;
	}

	@Override
	public String toString() {
		return "Exact(" + key + ", " + value + ")";
	}

	public boolean isExact() {
		return exact;
	}

	public String getKey() {
		return key;
	}

	public Object getValue() {
		return value;
	}

	public boolean isEmptyPredicate() {
		return false;
	}
}
