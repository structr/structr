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

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.property.RelationProperty;

/**
 * Serializes a {@link GraphObject} using a set of properties.
 *
 *
 */
public class PropertySetSerializationStrategy implements SerializationStrategy {

	private PropertyKey[] propertyKeys = null;

	public PropertySetSerializationStrategy(PropertyKey... propertyKeys) {

		this.propertyKeys = propertyKeys;

		if (propertyKeys == null || propertyKeys.length == 0) {
			throw new IllegalStateException("PropertySetDeserializationStrategy must contain at least one property.");
		}
	}

	@Override
	public void setRelationProperty(RelationProperty relationProperty) {
	}

	@Override
	public Object serialize(final SecurityContext securityContext, final String type, final GraphObject source) throws FrameworkException {

		if (source != null) {

			PropertyMap propertyMap = new PropertyMap();
			for (PropertyKey key : propertyKeys) {
				propertyMap.put(key, source.getProperty(key));
			}

			return PropertyMap.javaTypeToInputType(securityContext, type, propertyMap);
		}

		return null;
	}
}
