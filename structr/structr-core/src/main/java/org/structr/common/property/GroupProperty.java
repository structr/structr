/*
 *  Copyright (C) 2012 Axel Morgner
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
package org.structr.common.property;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.EntityContext;
import org.structr.core.GraphObject;
import org.structr.core.PropertyGroup;
import org.structr.core.converter.PropertyConverter;

/**
 *
 * @author Christian Morgner
 */
public class GroupProperty extends Property<PropertyMap> implements PropertyGroup<PropertyMap> {
	
	private static final Logger logger = Logger.getLogger(GroupProperty.class.getName());
	
	protected Class<? extends GraphObject> entityClass = null;
	protected PropertyKey[] propertyKeys               = null;
	
	public GroupProperty(String name, Class<? extends GraphObject> entityClass, PropertyKey... properties) {
		super(name);
		
		this.propertyKeys = properties;
		this.entityClass  = entityClass;

		// this looks strange
		EntityContext.registerPropertyGroup(entityClass, this, this);	
	}
	
	@Override
	public PropertyConverter<PropertyMap, ?> databaseConverter(SecurityContext securityContext, GraphObject currentObject) {
		return null;
	}

	@Override
	public PropertyConverter<Map<String, Object>, PropertyMap> inputConverter(SecurityContext securityContext) {
		return new InputConverter(securityContext);
	}
	
	
	private class InputConverter extends PropertyConverter<Map<String, Object>, PropertyMap> {

		public InputConverter(SecurityContext securityContext) {
			super(securityContext, null);
		}
		
		@Override
		public Map<String, Object> revert(PropertyMap source) throws FrameworkException {
			return PropertyMap.javaTypeToInputType(securityContext, entityClass, source);
		}

		@Override
		public PropertyMap convert(Map<String, Object> source) throws FrameworkException {
			return PropertyMap.inputTypeToJavaType(securityContext, entityClass, source);
		}
	}
	
	// ----- interface PropertyGroup -----
	@Override
	public PropertyMap getGroupedProperties(SecurityContext securityContext, GraphObject source) {

		PropertyMap groupedProperties = new PropertyMap();

		for (PropertyKey key : propertyKeys) {

			Object value = source.getProperty(key);
			
			PropertyConverter converter = key.inputConverter(securityContext);
			if (converter != null) {
				
				try {
					Object convertedValue = converter.revert(value);
					groupedProperties.put(key, convertedValue);
					
				} catch(FrameworkException fex) {
					
					logger.log(Level.WARNING, "Unable to convert grouped property {0} on type {1}: {2}", new Object[] {
						key.name(),
						source.getClass().getSimpleName(),
						fex.getMessage()
						
					});
				}
				
				
			} else {
				
				groupedProperties.put(key, value);
			}

		}

		return groupedProperties;
	}

	@Override
	public void setGroupedProperties(SecurityContext securityContext, PropertyMap source, GraphObject destination) throws FrameworkException {

		if (source == null) {

			for (PropertyKey key : propertyKeys) {

				destination.setProperty(key, null);

			}

			return;
			
		}

		for (PropertyKey key : propertyKeys) {
			
			Object value = source.get(key);

			PropertyConverter converter = key.inputConverter(securityContext);
			if (converter != null) {
				
				try {
					Object convertedValue = converter.convert(value);
					destination.setProperty(key, convertedValue);
					
				} catch(FrameworkException fex) {
					
					logger.log(Level.WARNING, "Unable to convert grouped property {0} on type {1}: {2}", new Object[] {
						key.name(),
						source.getClass().getSimpleName(),
						fex.getMessage()
						
					});
				}
				
				
			} else {
				
				destination.setProperty(key, value);
			}
			
		}
	}
}
