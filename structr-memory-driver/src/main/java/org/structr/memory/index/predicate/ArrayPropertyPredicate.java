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
package org.structr.memory.index.predicate;

import org.structr.api.Predicate;
import org.structr.api.graph.PropertyContainer;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * A predicate that accepts entities whose array property value
 * contains all of the given values.
 */
public class ArrayPropertyPredicate<T extends PropertyContainer, V> implements Predicate<T> {

	private final String key;
	private final V expectedValue;
	private final boolean exactMatch;

	public ArrayPropertyPredicate(final String key, final V expectedValue, final boolean exactMatch) {

		this.key           = key;
		this.expectedValue = expectedValue;
		this.exactMatch    = exactMatch;
	}

	@Override
	public String toString() {
		return "Array(" + key + "=" + Arrays.toString((Object[]) expectedValue) + ")";
	}

	@Override
	public boolean accept(final T entity) {

		final Object value = entity.getProperty(key);

		// support for null values
		if (expectedValue == null) {
			return value == null;
		}

		if (value != null) {

			List expected = new LinkedList<>();
			List actual   = Arrays.asList((Object[])value);

			if (expectedValue.getClass().isArray()) {

				expected.addAll(Arrays.asList((Object[])expectedValue));

			} else {

				// single, non-array value
				expected.add(expectedValue);
			}

			final boolean result;

			if (exactMatch) {

				result = Arrays.deepEquals(expected.toArray(), actual.toArray());

			} else {

				result = actual.containsAll(expected);
			}

			return result;
		}

		return false;
	}
}
