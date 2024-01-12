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
package org.structr.memory.index.predicate;

import org.structr.api.Predicate;
import org.structr.api.graph.PropertyContainer;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * A predicate that accepts entities whose array property value
 * contains all of the given values.
 */
public class ArrayPropertyPredicate<T extends PropertyContainer, V> implements Predicate<T> {

	private String key     = null;
	private V expectedValue = null;

	public ArrayPropertyPredicate(final String key, final V expectedValue) {

		this.key          = key;
		this.expectedValue = expectedValue;
	}

	@Override
	public boolean accept(final T entity) {

		final Object value = entity.getProperty(key);

		// support for null values
		if (expectedValue == null) {
			return value == null;
		}

		if (value != null) {

			Set expected = new HashSet<>();
			Set actual   = new HashSet<>(Arrays.asList((Object[])value));

			if (expectedValue.getClass().isArray()) {

				expected.addAll(Arrays.asList((Object[])expectedValue));

			} else {

				// single, non-array value
				expected.add(expectedValue);
			}

			return actual.containsAll(expected);
		}

		return false;
	}
}
