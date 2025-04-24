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
package org.structr.core.graph.search;

import org.structr.api.search.NotEmptyQuery;
import org.structr.core.GraphObject;
import org.structr.core.property.PropertyKey;

/**
 *
 *
 */
public class NotBlankSearchAttribute<T> extends EmptySearchAttribute<T> {

	public NotBlankSearchAttribute(PropertyKey<T> key) {
		super(key, null);
	}

	@Override
	public String toString() {
		return "NotBlankSearchAttribute()";
	}

	@Override
	public Class getQueryType() {
		return NotEmptyQuery.class;
	}

	@Override
	public boolean includeInResult(final GraphObject entity) {

		throw new RuntimeException("Not implemented");

		/*
		final Operation operation = getOperation();
		final T nodeValue         = entity.getProperty(getKey());

		if (operation.equals(Operation.NOT)) {

			// reverse
			return nodeValue == null;

		} else {

			return nodeValue != null;
		}

		 */
	}
}
