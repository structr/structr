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
package org.structr.core.notion;

import org.structr.core.property.PropertyKey;

/**
 * Combines a {@link PropertySerializationStrategy} and a {@link TypeAndValueDeserializationStrategy}
 * to read/write a {@link org.structr.core.GraphObject}.
 *
 *
 */
public class PropertyNotion extends Notion {

	private String propertyKey = null;
	
	public PropertyNotion(final String propertyKey) {
		this(propertyKey, false);
	}
	
	public PropertyNotion(final String propertyKey, final boolean createIfNotExisting) {

		super(
			new PropertySerializationStrategy(propertyKey),
			new TypeAndValueDeserializationStrategy(propertyKey, createIfNotExisting)
		);
		
		this.propertyKey = propertyKey;
	}

	@Override
	public PropertyKey getPrimaryPropertyKey() {
		return propertyKey;
	}
}
