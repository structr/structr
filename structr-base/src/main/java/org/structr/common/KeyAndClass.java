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
package org.structr.common;

import org.structr.core.property.PropertyKey;

/**
 * Simple class to hold a {@link PropertyKey} and a {@link String}
 *
 */
public class KeyAndClass<T> {

	private final String type;
	private final PropertyKey<T> key;

	public KeyAndClass(final PropertyKey<T> key, final String type) {

		this.key = key;
		this.type = type;
	}

	public PropertyKey<T> getPropertyKey() {
		return key;
	}

	public String getType() {
		return type;
	}
}
