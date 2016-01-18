/**
 * Copyright (C) 2010-2016 Structr GmbH
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

import org.structr.core.property.PropertyKey;

//~--- JDK imports ------------------------------------------------------------

import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.property.PropertyMap;
import org.structr.core.converter.PropertyConverter;

//~--- classes ----------------------------------------------------------------

/**
 * A property group that uses a Map.
 *
 *
 */
public class MapPropertyGroup implements PropertyGroup<PropertyMap> {

	private static final Logger logger   = Logger.getLogger(MapPropertyGroup.class.getName());
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
					
					logger.log(Level.WARNING, "Unable to convert grouped property {0} on type {1}: {2}", new Object[] {
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
