/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.property;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.TypeToken;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.entity.AbstractNode;
import org.structr.schema.SchemaHelper;

/**
 * A container for properties and their values that is used for input/output and database
 * conversion.
 *
 * @author Christian Morgner
 */
public class PropertyMap {

	private static final Logger logger = Logger.getLogger(PropertyMap.class.getName());

	protected Map<PropertyKey, Object> properties = new LinkedHashMap<>();

	public PropertyMap() {
	}

	public PropertyMap(PropertyMap source) {

		for (Entry<PropertyKey, Object> entry : source.entrySet()) {
			properties.put(entry.getKey(), entry.getValue());
		}
	}

	public int size() {
		return properties.size();
	}

	public boolean isEmpty() {
		return properties.isEmpty();
	}

	public <T> boolean containsKey(PropertyKey<T> key) {
		return properties.containsKey(key);
	}

	public boolean containsValue(Object value) {
		return properties.containsValue(value);
	}

	public <T> T get(PropertyKey<T> key) {
		return (T)properties.get(key);
	}

	public <T> T put(PropertyKey<T> key, T value) {
		return (T)properties.put(key, value);
	}

	public <T> T remove(PropertyKey<T> key) {
		return (T)properties.remove(key);
	}

	public void clear() {
		properties.clear();
	}

	public Set<PropertyKey> keySet() {
		return properties.keySet();
	}

	public Collection<Object> values() {
		return properties.values();
	}

	public Set<Entry<PropertyKey, Object>> entrySet() {
		return properties.entrySet();
	}

	public Map<PropertyKey, Object> getRawMap() {
		return properties;
	}

	/**
	 * Calculates a hash code for the contents of this PropertyMap.
	 *
	 * @param comparableKeys the set of property keys to use for hash code calculation, or null to use the whole keySet
	 * @param includeSystemProperties whether to include system properties in the calculatio9n
	 * @return
	 */
	public int contentHashCode(Set<PropertyKey> comparableKeys, boolean includeSystemProperties) {

		Map<PropertyKey, Object> sortedMap = new TreeMap<>(new PropertyKeyComparator());
		int hashCode                       = 42;

		sortedMap.putAll(properties);

		if (comparableKeys == null) {

			// calculate hash code for all properties in this map
			for (Entry<PropertyKey, Object> entry : sortedMap.entrySet()) {

				if (includeSystemProperties || !entry.getKey().isUnvalidated()) {

					hashCode ^= entry.hashCode();
				}
			}

		} else {

			for (Entry<PropertyKey, Object> entry : sortedMap.entrySet()) {

				PropertyKey key = entry.getKey();

				if (comparableKeys.contains(key)) {

					if (includeSystemProperties || !key.isUnvalidated()) {

						hashCode ^= entry.hashCode();
					}
				}
			}
		}


		return hashCode;
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

					PropertyKey propertyKey     = StructrApp.getConfiguration().getPropertyKeyForDatabaseName(entity.getClass(), key);
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

					PropertyKey propertyKey     = StructrApp.getConfiguration().getPropertyKeyForDatabaseName(entityType, key);
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

	public static PropertyMap databaseTypeToJavaType(SecurityContext securityContext, Class<? extends GraphObject> entityType, Map<String, Object> source) throws FrameworkException {

		PropertyMap resultMap = new PropertyMap();

		if (source != null) {

			for (Entry<String, Object> entry : source.entrySet()) {

				String key   = entry.getKey();
				Object value = entry.getValue();

				if (key != null) {

					PropertyKey propertyKey     = StructrApp.getConfiguration().getPropertyKeyForDatabaseName(entityType, key);
					PropertyConverter converter = propertyKey.databaseConverter(securityContext);

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

				Class<? extends GraphObject> type = SchemaHelper.getEntityClassForRawType(typeName.toString());
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

					PropertyKey propertyKey     = StructrApp.getConfiguration().getPropertyKeyForJSONName(entity, key);
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

		Map<String, Object> inputTypedProperties = new LinkedHashMap<>();

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
		//Thread.dumpStack();

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

	private static class PropertyKeyComparator implements Comparator<PropertyKey> {

		@Override
		public int compare(PropertyKey o1, PropertyKey o2) {
			return o1.jsonName().compareTo(o2.jsonName());
		}
	}
}
