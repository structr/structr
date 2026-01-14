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

/**
 */
public class StartsOrEndsWithPredicate<T extends PropertyContainer, V> implements Predicate<T> {

	private boolean caseInsensitive = false;
	private boolean startsWith      = false;
	private String value            = null;
	private String key              = null;

	public StartsOrEndsWithPredicate(final String key, final String value, final boolean startsWith, final boolean caseInsensitive) {

		this.caseInsensitive = caseInsensitive;
		this.startsWith      = startsWith;
		this.value           = value;
		this.key             = key;
	}

	@Override
	public boolean accept(final T entity) {

		final Object propertyValue = entity.getProperty(key);
		if (propertyValue != null) {

			final String stringValue = propertyValue.toString();

			if (startsWith) {

				// startsWith
				if (caseInsensitive) {

					return stringValue.startsWith(value);

				} else {

					return stringValue.toLowerCase().startsWith(value.toLowerCase());
				}

			} else {

				// endsWith
				if (caseInsensitive) {

					return stringValue.endsWith(value);

				} else {

					return stringValue.toLowerCase().endsWith(value.toLowerCase());
				}
			}
		}

		return false;
	}
}
