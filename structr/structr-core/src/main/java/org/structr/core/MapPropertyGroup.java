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



package org.structr.core;

import org.structr.common.PropertyKey;

//~--- JDK imports ------------------------------------------------------------

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.error.FrameworkException;

//~--- classes ----------------------------------------------------------------

/**
 * A property group that uses a Map.
 *
 * @author Christian Morgner
 */
public class MapPropertyGroup implements PropertyGroup {

	private static final Logger logger = Logger.getLogger(MapPropertyGroup.class.getName());
	private PropertyKey[] propertyKeys = null;

	//~--- constructors ---------------------------------------------------

	public MapPropertyGroup(PropertyKey... propertyKeys) {
		this.propertyKeys = propertyKeys;
	}

	//~--- get methods ----------------------------------------------------

	@Override
	public Object getGroupedProperties(GraphObject source) {

		Map<String, Object> groupedProperties = new LinkedHashMap<String, Object>();

		for (PropertyKey key : propertyKeys) {

			groupedProperties.put(key.name(), source.getProperty(key));

		}

		return groupedProperties;
	}

	//~--- set methods ----------------------------------------------------

	@Override
	public void setGroupedProperties(Object source, GraphObject destination) throws FrameworkException {

		if (source == null) {

			for (PropertyKey key : propertyKeys) {

				destination.setProperty(key, null);

			}

		} else if (source instanceof Map) {

			logger.log(Level.INFO, "Parameter is a Map");
			
			for (Entry<String, Object> attr : ((Map<String, Object>) source).entrySet()) {

				String key   = attr.getKey();
				Object value = attr.getValue();

				if (value instanceof JsonInput) {

					setGroupedProperties(value, destination);

				} else {
					
					PropertyKey propertyKey = destination.getPropertyKeyForName(key);
					destination.setProperty(propertyKey, value);

				}

			}

		} else if (source instanceof JsonInput) {

			logger.log(Level.INFO, "Parameter is a PropertySet");
			
			for (Entry<String, Object> entry : ((JsonInput)source).getAttributes().entrySet()) {

				String key   = entry.getKey();
				Object value = entry.getValue();

				if (value instanceof JsonInput) {

					// recursive put/post
					setGroupedProperties(value, destination);
					
				} else {

					PropertyKey propertyKey = destination.getPropertyKeyForName(key);
					destination.setProperty(propertyKey, value);

				}

			}

		}
	}
}
