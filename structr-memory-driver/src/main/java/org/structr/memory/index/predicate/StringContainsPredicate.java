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

/**
 */
public class StringContainsPredicate<T extends PropertyContainer> implements Predicate<T> {

	private String key          = null;
	private String desiredValue = null;

	public StringContainsPredicate(final String key, final String desiredValue) {

		this.key          = key;
		this.desiredValue = desiredValue;
	}

	@Override
	public boolean accept(final T entity) {

		final Object value = entity.getProperty(key);
		if (value != null) {

			final String string = value.toString().toLowerCase();

			return value != null && string.contains(desiredValue.toLowerCase());
		}

		return false;
	}
}
