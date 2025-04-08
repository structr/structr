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

import org.structr.api.search.ArrayQuery;
import org.structr.api.search.Operation;
import org.structr.core.property.PropertyKey;

/**
 *
 */
public class ArraySearchAttribute<T> extends PropertySearchAttribute<T> {

	public ArraySearchAttribute(final PropertyKey<T> key, final T value, final Operation operation, final boolean isExactMatch) {
		super(key, value, operation, isExactMatch);
	}

	@Override
	public Class getQueryType() {
		return ArrayQuery.class;
	}
}
