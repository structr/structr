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
package org.structr.api;

import java.util.Comparator;

/**
 *
 */
public interface Predicate<T> {

	boolean accept(final T value);

	/**
	 * Returns an optional Comparator to allow ordering
	 * of elements according to this predicate.
	 *
	 * @return a comparator or null
	 */
	default Comparator<T> comparator() {
		return null;
	}

	static <T> Predicate<T> all() {
		return value -> true;
	}

	static <T> Predicate<T> allExcept(final T given) {

		return value -> {

			if (value != null && given != null) {
				return !value.equals(given);
			}

			return true;
		};
	}

	static <T> Predicate<T> only(final T given) {

		return value -> {

			if (value != null && given != null) {
				return value.equals(given);
			}

			return false;
		};
	}
}
