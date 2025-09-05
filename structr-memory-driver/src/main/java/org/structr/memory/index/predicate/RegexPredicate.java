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
package org.structr.memory.index.predicate;

import org.structr.api.graph.PropertyContainer;

import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 */
public class RegexPredicate<T extends PropertyContainer> implements org.structr.api.Predicate<T> {

	private String key      = null;
	private Pattern pattern = null;

	public RegexPredicate(final String key, final String regex) {

		this.key   = key;
		this.pattern = Pattern.compile(regex);
	}

	@Override
	public String toString() {
		return "REGEX(" + key + ", " + pattern.pattern() + ")";
	}

	@Override
	public boolean accept(final T entity) {

		final Object value = entity.getProperty(key);
		if (value != null) {

			final Predicate<String> predicate = pattern.asMatchPredicate();

			return predicate.test(value.toString());
		}

		return false;
	}
}
