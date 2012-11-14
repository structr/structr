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

import org.structr.core.property.PropertyKey;

/**
 *
 * @author Christian Morgner
 */
public class PropertySetNotion extends Notion {

	public PropertySetNotion(PropertyKey... propertyKeys) {
		this(
			new PropertySetSerializationStrategy(propertyKeys),
			new TypeAndPropertySetDeserializationStrategy(propertyKeys)
		);

	}

	public PropertySetNotion(SerializationStrategy serializationStrategy, DeserializationStrategy deserializationStrategy) {
		super(serializationStrategy, deserializationStrategy);
	}

	@Override
	public PropertyKey getPrimaryPropertyKey() {
		return null; // this notion cannot deserialize objects from a single key
	}
}
