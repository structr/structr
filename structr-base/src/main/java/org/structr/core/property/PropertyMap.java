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
package org.structr.core.property;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.CreationContainer;
import org.structr.core.graph.Tx;
import org.structr.core.traits.Traits;
import org.structr.schema.SchemaHelper;

import java.util.*;
import java.util.Map.Entry;

/**
 * A container for properties and their values that is used for input/output and database
 * conversion.
 *
 *
 */
public class PropertyMap {

	private static final Logger logger       = LoggerFactory.getLogger(PropertyMap.class.getName());
	protected Map<String, Object> properties = new LinkedHashMap<>();

	public PropertyMap() {
	}

	public PropertyMap(final PropertyMap source) {
		putAll(source);
	}

	public <T> PropertyMap(final String key, final T value) {
		properties.put(key, value);
	}

	@Override
	public String toString() {
		return properties.toString();
	}

	public int size() {
		return properties.size();
	}

	public boolean isEmpty() {
		return properties.isEmpty();
	}

	public <T> boolean containsKey(final String key) {
		return properties.containsKey(key);
	}

	public boolean containsValue(final Object value) {
		return properties.containsValue(value);
	}

	public <T> T get(final String key) {
		return (T)properties.get(key);
	}

	public <T> T put(final String key, final T value) {
		return (T)properties.put(key, value);
	}

	public <T> T putIfAbsent(final String key, final T value) {
		return (T)properties.putIfAbsent(key, value);
	}

	public final void putAll(final PropertyMap source) {

		if (source != null) {

			for (Entry<String, Object> entry : source.entrySet()) {
				properties.put(entry.getKey(), entry.getValue());
			}

		}

	}

	public <T> T remove(final String key) {
		return (T)properties.remove(key);
	}

	public void clear() {
		properties.clear();
	}

	public Set<String> keySet() {
		return properties.keySet();
	}

	public Collection<Object> values() {
		return properties.values();
	}

	public Set<Entry<String, Object>> entrySet() {
		return properties.entrySet();
	}

	public Map<String, Object> getRawMap() {
		return properties;
	}

	public Map<String, Object> getAsMap() {

		final Map<String, Object> result = new LinkedHashMap<>();

		for (final Entry<String, Object> entry : properties.entrySet()) {

			result.put(entry.getKey(), entry.getValue());
		}

		return result;
	}

	// ----- static methods -----
	public static PropertyMap javaTypeToDatabaseType(final SecurityContext securityContext, final GraphObject wrapped, final Map<String, Object> source) throws FrameworkException {

		final PropertyMap resultMap = new PropertyMap();
		final GraphObject entity    = unwrap(wrapped);

		if (source != null) {

			for (Entry<String, Object> entry : source.entrySet()) {

				String key   = entry.getKey();
				Object value = entry.getValue();

				if (key != null) {

					final PropertyKey propertyKey     = entity.getTraits().key(key);
					final PropertyConverter converter = propertyKey.databaseConverter(securityContext, entity);

					if (converter != null) {

						try {
							final Object propertyValue = converter.convert(value);

							resultMap.put(key, propertyValue);

						} catch(ClassCastException cce) {

							throw new FrameworkException(422, "Invalid JSON input for key " + propertyKey.jsonName() + ", expected a JSON " + propertyKey.typeName() + ".");
						}

					} else {

						resultMap.put(key, value);
					}
				}
			}
		}

		return resultMap;
	}

	public static PropertyMap databaseTypeToJavaType(final SecurityContext securityContext, final GraphObject wrapped, final Map<String, Object> source) throws FrameworkException {

		final PropertyMap resultMap = new PropertyMap();
		final GraphObject entity    = unwrap(wrapped);

		if (source != null) {

			for (Entry<String, Object> entry : source.entrySet()) {

				String key   = entry.getKey();
				Object value = entry.getValue();

				if (key != null) {

					final PropertyKey propertyKey     = entity.getTraits().key(key);
					final PropertyConverter converter = propertyKey.databaseConverter(securityContext, entity);

					if (converter != null) {

						try {
							Object propertyValue = converter.revert(value);
							resultMap.put(key, propertyValue);

						} catch(ClassCastException cce) {

							throw new FrameworkException(422, "Invalid JSON input for key " + propertyKey.jsonName() + ", expected a JSON " + propertyKey.typeName() + ".");
						}

					} else {

						resultMap.put(key, value);
					}
				}
			}
		}

		return resultMap;
	}

	public static PropertyMap databaseTypeToJavaType(final SecurityContext securityContext, Traits traits, final Map<String, Object> source) throws FrameworkException {

		final PropertyMap resultMap = new PropertyMap();

		if (source != null) {

			for (Entry<String, Object> entry : source.entrySet()) {

				String key   = entry.getKey();
				Object value = entry.getValue();

				if (key != null) {

					final PropertyKey propertyKey = traits.key(key);
					PropertyConverter converter   = propertyKey.databaseConverter(securityContext);

					if (converter != null) {

						try {
							final Object propertyValue = converter.revert(value);
							resultMap.put(key, propertyValue);

						} catch(ClassCastException cce) {

							throw new FrameworkException(422, "Invalid JSON input for key " + propertyKey.jsonName() + ", expected a JSON " + propertyKey.typeName() + ".");
						}

					} else {

						resultMap.put(key, value);
					}
				}
			}
		}

		return resultMap;
	}

	public static PropertyMap inputTypeToJavaType(final SecurityContext securityContext, final Map<String, Object> source) throws FrameworkException {

		if (source != null) {

			Object typeName = source.get("type");
			if (typeName != null) {

				final Traits type = Traits.of(typeName.toString());
				if (type != null) {

					return inputTypeToJavaType(securityContext, typeName.toString(), source);

				} else {

					logger.warn("No entity type found for raw type {}", typeName);
				}

			} else {

				logger.warn("No entity type found in source map: {}", source);
			}
		}

		return fallbackPropertyMap(source);
	}

	public static PropertyMap inputTypeToJavaType(final SecurityContext securityContext, final String type, final Map<String, Object> source) throws FrameworkException {

		final PropertyMap resultMap = new PropertyMap();
		final Traits traits         = Traits.of(type);

		// caution, source can be null when an empty nested property group is encountered!
		if (source != null) {

			final String batchType = securityContext.getAttribute("batchType", "__");
			if (batchType.equals(source.get("type"))) {

				// only to batching if a type is set for which batch is enable
				final Integer count   = securityContext.getAttribute("objectCount", 0);
				final Integer overall = securityContext.getAttribute("overallCount", 0);

				securityContext.setAttribute("objectCount",  count   + 1);
				securityContext.setAttribute("overallCount", overall + 1);

				if (count == 100) {

					final Tx tx = (Tx)securityContext.getAttribute("currentTransaction");
					if (tx != null) {

						logger.info("Committing batch transaction after {} objects of type {}.", overall, batchType);

						// try to commit this batch
						tx.success();
						tx.close();

						// open new transaction and store it in context
						securityContext.setAttribute("currentTransaction", StructrApp.getInstance(securityContext).tx());
						securityContext.setAttribute("objectCount",        0);
					}
				}
			}

			for (final Entry<String, Object> entry : source.entrySet()) {

				final String key   = entry.getKey();
				final Object value = entry.getValue();

				if (key != null) {

					PropertyKey propertyKey = traits.key(key);
					if (propertyKey == null) {

						// fixme
						propertyKey = new GenericProperty(key);
					}

					if (propertyKey instanceof GenericProperty) {

						if (traits.contains("DOMNode")) {

							// allow custom attributes on DOMNode

						} else if (traits.contains("Principal") && "allowed".equals(key)) {

							// allow "allowed" property for grantees

						} else {

							// check settings on how to handle invalid JSON input
							switch (Settings.InputValidationMode.getValue()) {

								case "reject_warn":
									logger.warn("Rejecting input with unknown JSON key for type {}: \"{}\" = \"{}\"", type, key, value);
								case "reject":
									throw new FrameworkException(422, "Rejecting input with unknown JSON key for type " + type + ": \"" + key + "\" with value \"" + value + "\".");

								case "ignore_warn":
									logger.warn("Ignoring unknown JSON key for type {}: \"{}\" = \"{}\"", type, key, value);
								case "ignore":
									// move on to the next key/value pair
									continue;

								case "accept_warn":
									logger.warn("Accepting unknown JSON key for type {}: \"{}\" = \"{}\"", type, key, value);
								case "accept":
									// allow the key/value pair to be read
							}
						}
					}

					if (propertyKey != null) {

						final PropertyConverter converter = propertyKey.inputConverter(securityContext);

						if (converter != null && value != null) { // && !propertyKey.valueType().isAssignableFrom(value.getClass())) {

							try {

								// test
								converter.setContext(source);

								Object propertyValue = converter.convert(value);
								resultMap.put(key, propertyValue);

							} catch (ClassCastException cce) {

								throw new FrameworkException(422, "Invalid JSON input for key " + propertyKey.jsonName() + ", expected a JSON " + propertyKey.typeName() + ".");
							}

						} else {

							resultMap.put(key, value);
						}
					}
				}
			}
		}

		return resultMap;
	}

	public static Map<String, Object> javaTypeToDatabaseType(final SecurityContext securityContext, final String type, final PropertyMap properties) throws FrameworkException {

		final Map<String, Object> databaseTypedProperties = new LinkedHashMap<>();
		final Traits traits                               = Traits.of(type);

		for (Entry<String, Object> entry : properties.entrySet()) {

			final String key                  = entry.getKey();
			final PropertyKey propertyKey     = traits.key(key);
			final PropertyConverter converter = propertyKey.databaseConverter(securityContext);

			if (converter != null) {

				try {
					Object propertyValue = converter.convert(entry.getValue());
					databaseTypedProperties.put(propertyKey.jsonName(), propertyValue);

				} catch(ClassCastException cce) {

					throw new FrameworkException(422, "Invalid JSON input for key " + propertyKey.jsonName() + ", expected a JSON " + propertyKey.typeName() + ".");
				}

			} else {

				databaseTypedProperties.put(propertyKey.jsonName(), entry.getValue());
			}
		}

		return databaseTypedProperties;
	}

	public static Map<String, Object> javaTypeToInputType(final SecurityContext securityContext, final String type, final PropertyMap properties) throws FrameworkException {

		final Map<String, Object> inputTypedProperties = new LinkedHashMap<>();
		final Traits traits                            = Traits.of(type);

		for (Entry<String, Object> entry : properties.entrySet()) {

			final String key                  = entry.getKey();
			final PropertyKey propertyKey     = traits.key(key);
			final PropertyConverter converter = propertyKey.inputConverter(securityContext);

			if (converter != null) {

				try {
					final Object propertyValue = converter.revert(entry.getValue());
					inputTypedProperties.put(propertyKey.jsonName(), propertyValue);

				} catch(ClassCastException cce) {

					throw new FrameworkException(422, "Invalid JSON input for key " + propertyKey.jsonName() + ", expected a JSON " + propertyKey.typeName() + ".");
				}

			} else {

				inputTypedProperties.put(propertyKey.jsonName(), entry.getValue());
			}
		}

		return inputTypedProperties;
	}

	private static PropertyMap fallbackPropertyMap(final Map<String, Object> source) {

		PropertyMap map = new PropertyMap();

		logger.error("Using GenericProperty for input {}", source);

		if (source != null) {

			for (Entry<String, Object> entry : source.entrySet()) {

				String key   = entry.getKey();
				Object value = entry.getValue();

				if (key != null && value != null) {

					map.put(key, value);
				}
			}
		}

		return map;
	}

	public static GraphObject unwrap(final GraphObject source) {

		if (source instanceof CreationContainer) {

			final CreationContainer container = (CreationContainer)source;

			return container.getWrappedObject();
		}

		return source;
	}
}
