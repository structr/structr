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
package org.structr.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;

import java.util.Map.Entry;

//~--- classes ----------------------------------------------------------------

/**
 * A property group that uses a Map.
 *
 *
 */
public class MapPropertyGroup implements PropertyGroup<PropertyMap> {

	private static final Logger logger   = LoggerFactory.getLogger(MapPropertyGroup.class.getName());
	protected PropertyKey[] propertyKeys = null;

	//~--- constructors ---------------------------------------------------

	public MapPropertyGroup(PropertyKey... propertyKeys) {
		this.propertyKeys = propertyKeys;
	}

	//~--- get methods ----------------------------------------------------

	@Override
	public PropertyMap getGroupedProperties(SecurityContext securityContext, GraphObject source) {

		PropertyMap groupedProperties = new PropertyMap();

		for (PropertyKey key : propertyKeys) {

			PropertyConverter converter = key.inputConverter(securityContext);
			if (converter != null) {
				
				try {
					Object convertedValue = converter.revert(source.getProperty(key));
					groupedProperties.put(key, convertedValue);
					
				} catch(FrameworkException fex) {
					
					logger.warn("Unable to convert grouped property {} on type {}: {}", new Object[] {
						key.dbName(),
						source.getClass().getSimpleName(),
						fex.getMessage()
						
					});
				}
				
				
			} else {
				
				groupedProperties.put(key, source.getProperty(key));
			}

		}

		return groupedProperties;
	}

	//~--- set methods ----------------------------------------------------

	@Override
	public void setGroupedProperties(SecurityContext securityContext, PropertyMap source, GraphObject destination) throws FrameworkException {

		if (source == null) {

			for (PropertyKey key : propertyKeys) {

				destination.setProperty(key, null);

			}

			return;
			
		}

		for (Entry<PropertyKey, Object> entry : source.entrySet()) {
			destination.setProperty(entry.getKey(), entry.getValue());
		}
	}
}
