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

import org.structr.core.property.PropertyKey;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.ReadOnlyPropertyToken;
import org.structr.core.EntityContext;
import org.structr.core.GraphObject;
import org.structr.core.PropertyGroup;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.node.search.*;

/**
 *
 * @author Christian Morgner
 */
public class GroupProperty extends Property<PropertyMap> implements PropertyGroup<PropertyMap> {
	
	private static final Logger logger = Logger.getLogger(GroupProperty.class.getName());
	
	// indicates whether this group property is 
	protected Map<String, PropertyKey> propertyKeys    = new LinkedHashMap<String, PropertyKey>();
	protected Class<? extends GraphObject> entityClass = null;
	protected Property<Boolean> nullValuesOnlyProperty = null;
	
	public GroupProperty(String name, Class<? extends GraphObject> entityClass, PropertyKey... properties) {
		
		super(name);
		
		for (PropertyKey key : properties) {
			propertyKeys.put(key.jsonName(), key);
		}
		
		this.nullValuesOnlyProperty = new BooleanProperty(name.concat(".").concat("nullValuesOnly"));
		this.entityClass            = entityClass;

		
		// this looks strange
		EntityContext.registerPropertyGroup(entityClass, this, this);	
	}
	
	@Override
	public String typeName() {
		return "Object";
	}
	
	@Override
	public PropertyConverter<PropertyMap, ?> databaseConverter(SecurityContext securityContext, GraphObject currentObject) {
		return null;
	}

	@Override
	public PropertyConverter<Map<String, Object>, PropertyMap> inputConverter(SecurityContext securityContext) {
		return new InputConverter(securityContext);
	}

	@Override
	public SearchAttribute getSearchAttribute(SearchOperator op, PropertyMap searchValues, boolean exactMatch) {
		
		SearchAttributeGroup group = new SearchAttributeGroup(op);
		
		for (PropertyKey key : propertyKeys.values()) {
			
			Object value = searchValues.get(key);
			if (value != null) {
				
				group.add( new TextualSearchAttribute(new GroupPropertyWrapper(key), value.toString(), SearchOperator.AND) );
			}
		}
		
		return group;
	}

	@Override
	public void registerSearchableProperties(Set<PropertyKey> searchableProperties) {

		searchableProperties.add(this);
		
		// add grouped properties as well
		for (PropertyKey groupKey : propertyKeys.values()) {
			
			searchableProperties.add(new GroupPropertyWrapper(groupKey));
		}
	}
	
	public <T> PropertyKey<T> getGroupProperty(String name, Class<T> type) {
		return new GroupPropertyWrapper((propertyKeys.get(name)));
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
		Boolean nullValuesOnly        = source.getProperty(nullValuesOnlyProperty);
		
		// return immediately, as there are no properties in this group
		if (nullValuesOnly != null && nullValuesOnly.booleanValue()) {
			return null;
		}

		for (PropertyKey key : propertyKeys.values()) {

			Object value = source.getProperty(new GroupPropertyWrapper(key));
			
			PropertyConverter converter = key.inputConverter(securityContext);
			if (converter != null) {
				
				try {
					Object convertedValue = converter.revert(value);
					groupedProperties.put(key, convertedValue);

					if (convertedValue != null) {
						nullValuesOnly = false;
					}
					
					
				} catch(Throwable t) {
					
					t.printStackTrace();
					
					logger.log(Level.WARNING, "Unable to convert grouped property {0} on type {1}: {2}", new Object[] {
						key.dbName(),
						source.getClass().getSimpleName(),
						t.getMessage()
						
					});
				}
				
				
			} else {
				
				groupedProperties.put(key, value);

				if (value != null) {
					nullValuesOnly = false;
				}
			}

		}
		
		return groupedProperties;
	}

	@Override
	public void setGroupedProperties(SecurityContext securityContext, PropertyMap source, GraphObject destination) throws FrameworkException {

		if (source.containsKey(nullValuesOnlyProperty)) {
			throw new FrameworkException("base", new ReadOnlyPropertyToken(nullValuesOnlyProperty));
		}
		
		if (source.isEmpty()) {

			destination.setProperty(nullValuesOnlyProperty, true);
			
			return;
			
		}
		
		// indicate that this group actually contains values
		destination.setProperty(nullValuesOnlyProperty, false);
		
		// set properties
		for (PropertyKey key : propertyKeys.values()) {
			
			PropertyKey groupPropertyKey = new GroupPropertyWrapper(key);
			Object value = source.get(key);

			PropertyConverter converter = groupPropertyKey.inputConverter(securityContext);
			if (converter != null) {
				
				try {
					Object convertedValue = converter.convert(value);
					destination.setProperty(groupPropertyKey, convertedValue);
					
				} catch(FrameworkException fex) {
					
					logger.log(Level.WARNING, "Unable to convert grouped property {0} on type {1}: {2}", new Object[] {
						groupPropertyKey.dbName(),
						source.getClass().getSimpleName(),
						fex.getMessage()
						
					});
				}
				
				
			} else {
				
				destination.setProperty(groupPropertyKey, value);
			}
			
		}
	}
	
	/**
	 * Acts as a wrapper for property keys to prefix their name with
	 * the name of the surrounding property group.
	 */
	private class GroupPropertyWrapper implements PropertyKey {

		private PropertyKey wrappedKey = null;
		
		public GroupPropertyWrapper(PropertyKey keyToWrap) {
			this.wrappedKey = keyToWrap;
		}
		
		@Override
		public String toString() {
			return "(".concat(jsonName()).concat("|").concat(dbName()).concat(")");
		}
	
		@Override
		public int hashCode() {

			// make hashCode funtion work for subtypes that override jsonName() etc. as well
			if (dbName() != null && jsonName() != null) {
				return (dbName().hashCode() * 31) + jsonName().hashCode();
			}

			if (dbName() != null) {
				return dbName().hashCode();
			}

			if (jsonName() != null) {
				return jsonName().hashCode();
			}

			// TODO: check if it's ok if null key is not unique
			return super.hashCode();
		}

		@Override
		public boolean equals(Object o) {

			if (o instanceof PropertyKey) {

				return o.hashCode() == hashCode();
			}

			return false;
		}
		
		@Override
		public String jsonName() {
			return GroupProperty.this.jsonName.concat(".").concat(wrappedKey.jsonName());
		}

		@Override
		public String dbName() {
			return GroupProperty.this.dbName.concat(".").concat(wrappedKey.dbName());
		}
		
		@Override
		public String typeName() {
			return wrappedKey.typeName();
		}

		@Override
		public Object defaultValue() {
			return wrappedKey.defaultValue();
		}

		@Override
		public PropertyConverter databaseConverter(SecurityContext securityContext, GraphObject entitiy) {
			return wrappedKey.databaseConverter(securityContext, entitiy);
		}

		@Override
		public PropertyConverter inputConverter(SecurityContext securityContext) {
			return wrappedKey.inputConverter(securityContext);
		}

		@Override
		public void setDeclaringClassName(String declaringClassName) {
			wrappedKey.setDeclaringClassName(declaringClassName);
		}

		@Override
		public SearchAttribute getSearchAttribute(SearchOperator op, Object searchValue, boolean exactMatch) {
		
			// return empty string on null value here to enable searching for empty values
			String searchString = searchValue != null ? searchValue.toString() : "";
			String search       = exactMatch ? Search.exactMatch(searchString) : searchString;

			return new TextualSearchAttribute(this, search, op);
		}

		@Override
		public void registerSearchableProperties(Set searchableProperties) {
			wrappedKey.registerSearchableProperties(searchableProperties);
		}

		@Override
		public boolean isSystemProperty() {
			return wrappedKey.isSystemProperty();
		}

		@Override
		public boolean isReadOnlyProperty() {
			return wrappedKey.isReadOnlyProperty();
		}

		@Override
		public boolean isWriteOnceProperty() {
			return wrappedKey.isWriteOnceProperty();
		}
	}
}
