/*
 *  Copyright (C) 2010-2012 Axel Morgner
 * 
 *  This file is part of structr <http://structr.org>.
 * 
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.structr.core.notion;

import org.structr.common.property.PropertyKey;

/**
 * A notion that returns only a single property of the source object.
 *
 * @author Christian Morgner
 */
public class PropertyNotion extends Notion {

	private PropertyKey propertyKey = null;
	
	public PropertyNotion(PropertyKey propertyKey) {
		this(propertyKey, false);
	}
	
	public PropertyNotion(PropertyKey propertyKey, boolean createIfNotExisting) {

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
