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
package org.structr.common.error;

import org.structr.core.property.PropertyKey;

/**
 * Indicates that a property value has the wrong type.
 *
 *
 */
public class TypeToken extends SemanticErrorToken {

	public TypeToken(final Class type, final PropertyKey propertyKey, final String desiredType) {
		this(type.getSimpleName(), propertyKey, desiredType);
	}

	public TypeToken(final String type, final PropertyKey propertyKey, final String desiredType) {
		super(type, propertyKey, "must_be_of_type", desiredType);
	}
}
