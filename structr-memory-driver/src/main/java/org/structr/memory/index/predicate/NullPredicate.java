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

import org.apache.commons.lang3.StringUtils;
import org.structr.api.Predicate;
import org.structr.api.graph.PropertyContainer;

/**
 */
public class NullPredicate<T extends PropertyContainer, V> implements Predicate<T> {

	private String key = null;

	public NullPredicate(final String key) {
		this.key = key;
	}

	@Override
	public String toString() {
		return "NULL(" + key + ")";
	}

	@Override
	public boolean accept(final T entity) {

		final Object value = entity.getProperty(key);

		if (value instanceof String) {

			return StringUtils.isEmpty((String)value);
		}

		return value == null;
	}
}
