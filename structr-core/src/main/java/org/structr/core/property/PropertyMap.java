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
package org.structr.core.property;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.TypeToken;
import org.structr.core.EntityContext;
import org.structr.core.GraphObject;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.entity.AbstractNode;

/**
 * A container for properties and their values that is used for input/output and database
 * conversion.
 * 
 * @author Christian Morgner
 */
public class PropertyMap implements Map<PropertyKey, Object> {
	
	private static final Logger logger = Logger.getLogger(PropertyMap.class.getName());
	
	protected Map<PropertyKey, Object> properties = new LinkedHashMap<PropertyKey, Object>();

	public PropertyMap() {}
	
	public PropertyMap(Map<PropertyKey, Object> source) {
		properties.putAll(source);
	}
	
	@Override
	public int size() {
		return properties.size();
	}

	@Override
	public boolean isEmpty() {
		return properties.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return properties.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return properties.containsValue(value);
	}

	@Override
	public Object get(Object key) {
		return properties.get(key);
	}

	public <T> T get(PropertyKey<T> key) {
		return (T)properties.get(key);
	}

	@Override
	public Object put(PropertyKey key, Object value) {
		return properties.put(key, value);
	}

	@Override
	public Object remove(Object key) {
		return properties.remove(key);
	}

	@Override
	public void putAll(Map<? extends PropertyKey, ? extends Object> m) {
		properties.putAll(m);
	}

	@Override
	public void clear() {
		properties.clear();
	}

	@Override
	public Set<PropertyKey> keySet() {
		return properties.keySet();
	}

	@Override
	public Collection<Object> values() {
		return properties.values();
	}

	@Override
	public Set<Entry<PropertyKey, Object>> entrySet() {
		return properties.entrySet();
	}
	
	
	// ----- static methods -----
	public static PropertyMap javaTypeToDatabaseType(SecurityContext securityContext, GraphObject entity, Map<String, Object> source) throws FrameworkException {
		
		PropertyMap resultMap = new PropertyMap();
		Class entityType      = entity.getClass();
		
		if (source != null) {

			for (Entry<String, Object> entry : source.entrySet()) {

				String key   = entry.getKey();
				Object value = entry.getValue();

				if (key != null) {

					PropertyKey propertyKey     = EntityContext.getPropertyKeyForDatabaseName(entity.getClass(), key);
					PropertyConverter converter = propertyKey.databaseConverter(securityContext, entity);

					if (converter != null) {

						try {
							Object propertyValue = converter.convert(value);
							resultMap.put(propertyKey, propertyValue);

						} catch(ClassCastException cce) {

							cce.printStackTrace();

							throw new FrameworkException(entityType.getSimpleName(), new TypeToken(propertyKey, propertyKey.typeName()));
						}

					} else {

						resultMap.put(propertyKey, value);
					}
				}
			}
		}
		
		return resultMap;
	}
	
	public static PropertyMap databaseTypeToJavaType(SecurityContext securityContext, GraphObject entity, Map<String, Object> source) throws FrameworkException {
		
		PropertyMap resultMap = new PropertyMap();
		Class entityType      = entity.getClass();

		if (source != null) {

			for (Entry<String, Object> entry : source.entrySet()) {

				String key   = entry.getKey();
				Object value = entry.getValue();

				if (key != null) {

					PropertyKey propertyKey     = EntityContext.getPropertyKeyForDatabaseName(entityType, key);
					PropertyConverter converter = propertyKey.databaseConverter(securityContext, entity);

					if (converter != null) {

						try {
							Object propertyValue = converter.revert(value);
							resultMap.put(propertyKey, propertyValue);

						} catch(ClassCastException cce) {

							cce.printStackTrace();

							throw new FrameworkException(entityType.getSimpleName(), new TypeToken(propertyKey, propertyKey.typeName()));
						}

					} else {

						resultMap.put(propertyKey, value);
					}
				}
			}
		}
		
		return resultMap;
	}
	
	public static PropertyMap inputTypeToJavaType(SecurityContext securityContext, Map<String, Object> source) throws FrameworkException {

		if (source != null) {

			Object typeName = source.get(AbstractNode.type.jsonName());
			if (typeName != null) {

				Class<? extends GraphObject> type = EntityContext.getEntityClassForRawType(typeName.toString());
				if (type != null) {

					return inputTypeToJavaType(securityContext, type, source);

				} else {

					logger.log(Level.WARNING, "No entity type found for raw type {0}", typeName);
				}

			} else {

				logger.log(Level.WARNING, "No entity type found in source map");
			}
		}
			
		return fallbackPropertyMap(source);
	}
		
	public static PropertyMap inputTypeToJavaType(SecurityContext securityContext, Class<? extends GraphObject> entity, Map<String, Object> source) throws FrameworkException {
		
		PropertyMap resultMap = new PropertyMap();
		if (source != null) {

			// caution, source can be null when an empty nested property group is encountered!
			for (Entry<String, Object> entry : source.entrySet()) {

				String key   = entry.getKey();
				Object value = entry.getValue();

				if (key != null) {

					PropertyKey propertyKey     = EntityContext.getPropertyKeyForJSONName(entity, key);
					PropertyConverter converter = propertyKey.inputConverter(securityContext);

					if (converter != null) {

						try {
							Object propertyValue = converter.convert(value);
							resultMap.put(propertyKey, propertyValue);

						} catch(ClassCastException cce) {

							cce.printStackTrace();

							throw new FrameworkException(entity.getSimpleName(), new TypeToken(propertyKey, propertyKey.typeName()));
						}

					} else {

						resultMap.put(propertyKey, value);
					}
				}
			}
		}
		
		return resultMap;
	}
	
	public static Map<String, Object> javaTypeToInputType(SecurityContext securityContext, Class<? extends GraphObject> entity, PropertyMap properties) throws FrameworkException {
		
		Map<String, Object> inputTypedProperties = new LinkedHashMap<String, Object>();
		
		for(Entry<PropertyKey, Object> entry : properties.entrySet()) {
			
			PropertyKey propertyKey     = entry.getKey();
			PropertyConverter converter = propertyKey.inputConverter(securityContext);

			if (converter != null) {

				try {
					Object propertyValue = converter.revert(entry.getValue());
					inputTypedProperties.put(propertyKey.jsonName(), propertyValue);
					
				} catch(ClassCastException cce) {

					cce.printStackTrace();
					
					throw new FrameworkException(entity.getSimpleName(), new TypeToken(propertyKey, propertyKey.typeName()));
				}

			} else {

				inputTypedProperties.put(propertyKey.jsonName(), entry.getValue());
			}
		}
		
		return inputTypedProperties;
	}
	
	private static PropertyMap fallbackPropertyMap(Map<String, Object> source) {

		PropertyMap map = new PropertyMap();

		logger.log(Level.SEVERE, "Using fallback property set conversion without type safety!");
		Thread.dumpStack();

		if (source != null) {
			

			for (Entry<String, Object> entry : source.entrySet()) {

				String key   = entry.getKey();
				Object value = entry.getValue();

				if (key != null && value != null) {

					map.put(new GenericProperty(key), value);
				}
			}
		}
		
		return map;
	}
}
