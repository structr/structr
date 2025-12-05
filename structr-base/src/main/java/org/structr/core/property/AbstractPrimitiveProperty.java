/*
 * Copyright (C) 2010-2025 Structr GmbH
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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.NotInTransactionException;
import org.structr.api.Predicate;
import org.structr.api.RetryException;
import org.structr.api.graph.PropertyContainer;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.entity.Principal;
import org.structr.core.entity.SuperUser;
import org.structr.core.graph.CreationContainer;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.schema.Transformer;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;


/**
 * Abstract base class for primitive properties.
 *
 *
 * @param <T>
 */
public abstract class AbstractPrimitiveProperty<T> extends Property<T> {

	private static final Logger logger = LoggerFactory.getLogger(AbstractPrimitiveProperty.class.getName());

	protected SecurityContext securityContext = null;
	protected GraphObject entity              = null;

	public AbstractPrimitiveProperty(final String name) {
		super(name);
	}

	public AbstractPrimitiveProperty(final String jsonName, final String dbName) {
		super(jsonName, dbName);
	}

	public AbstractPrimitiveProperty(final String jsonName, final String dbName, final T defaultValue) {
		super(jsonName, dbName, defaultValue);
	}

	@Override
	public T getProperty(final SecurityContext securityContext, final GraphObject obj, final boolean applyConverter) {
		return getProperty(securityContext, obj, applyConverter, null);
	}

	@Override
	public T getProperty(final SecurityContext securityContext, final GraphObject obj, final boolean applyConverter, final Predicate<GraphObject> predicate) {

		Object value = null;

		final PropertyContainer propertyContainer = obj.getPropertyContainer();
		if (propertyContainer != null) {

			value = propertyContainer.getProperty(dbName());
		}

		if (applyConverter) {

			// apply property converters
			PropertyConverter converter = databaseConverter(securityContext, PropertyMap.unwrap(obj));
			if (converter != null) {

				try {
					value = converter.revert(value);

				} catch (Throwable t) {

					logger.warn("Unable to convert property {} of type {}: {}", dbName(), getClass().getSimpleName(), t);
				}
			}
		}

		// use transformators from property
		for (final String fqcn : transformators) {

			// first test, use caching here later..
			final Transformer transformator = getTransformator(fqcn);
			if (transformator != null) {

				value = transformator.getProperty(PropertyMap.unwrap(obj), this, value);
			}

		}

		// no value found, use schema default
		if (value == null) {
			value = defaultValue();
		}

		return (T) value;
	}

	@Override
	public Object setProperty(final SecurityContext securityContext, final GraphObject obj, final T value) throws FrameworkException {

		final PropertyConverter converter = databaseConverter(securityContext, PropertyMap.unwrap(obj));
		Object convertedValue             = value;

		if (converter != null) {

			convertedValue = converter.convert(value);
		}

		// use transformators from property
		for (final String fqcn : transformators) {

			// first test, use caching here later..
			final Transformer transformator = getTransformator(fqcn);
			if (transformator != null) {

				convertedValue = transformator.setProperty(PropertyMap.unwrap(obj), this, convertedValue);
			}

		}

		final PropertyContainer propertyContainer = obj.getPropertyContainer();
		if (propertyContainer != null) {

			if (!TransactionCommand.inTransaction()) {

				throw new NotInTransactionException("setProperty outside of transaction");
			}

			boolean internalSystemPropertiesUnlocked = (obj instanceof CreationContainer);

			// notify only non-system properties

			// collect modified properties
			if (obj instanceof NodeInterface) {

				if (!unvalidated) {

					TransactionCommand.nodeModified(
						securityContext.getCachedUser(),
						(NodeInterface)obj,
						AbstractPrimitiveProperty.this,
						propertyContainer.hasProperty(dbName()) ? propertyContainer.getProperty(dbName()) : null,
						value
					);
				}

				internalSystemPropertiesUnlocked = obj.systemPropertiesUnlocked();

			} else if (obj instanceof RelationshipInterface rel) {

				if (!unvalidated) {

					TransactionCommand.relationshipModified(
						securityContext.getCachedUser(),
						(RelationshipInterface)obj,
						AbstractPrimitiveProperty.this,
						propertyContainer.hasProperty(dbName()) ? propertyContainer.getProperty(dbName()) : null,
						value
					);
				}

				internalSystemPropertiesUnlocked = rel.systemPropertiesUnlocked();
			}

			// catch all sorts of errors and wrap them in a FrameworkException
			try {

				// save space
				if (convertedValue == null) {

					propertyContainer.removeProperty(dbName());

				} else {

					if (!isSystemInternal() || internalSystemPropertiesUnlocked) {

						propertyContainer.setProperty(dbName(), convertedValue);

					} else {

						logger.warn("Tried to set internal system property {} to {}. Action was denied.", new Object[]{dbName(), convertedValue});

					}
				}

				updateAccessInformation(securityContext, propertyContainer);

			} catch (final RetryException rex) {

				// don't catch RetryException here
				throw rex;

			} catch (Throwable t) {

				// throw FrameworkException with the given cause
				final FrameworkException fex = new FrameworkException(500, "Unable to set property " + jsonName() + " on entity with ID " + obj.getUuid() + ": " + t.toString());
				fex.initCause(t);

				throw fex;
			}
		}

		return null;
	}

	@Override
	public String relatedType() {
		return null;
	}

	@Override
	public boolean isCollection() {
		return false;
	}

	// ----- OpenAPI -----
	@Override
	public Map<String, Object> describeOpenAPIOutputType(final String type, final String viewName, final int level) {

		final Map<String, Object> map = new TreeMap<>();
		final Class valueType         = valueType();

		final Map<String, String> openApiTypeMap = new HashMap<>();
		openApiTypeMap.put("image", "object");
		openApiTypeMap.put("double", "number");

		if (valueType != null) {
			String simpleName = valueType.getSimpleName().toLowerCase();

			if (openApiTypeMap.containsKey(simpleName)) {
				simpleName = openApiTypeMap.get(simpleName);
			}

			map.put("type", simpleName);
			map.put("example", getExampleValue(type, viewName));

			if (this.isReadOnly()) {
				map.put("readOnly", true);
			}

		}
		return map;
	}

	@Override
	public Map<String, Object> describeOpenAPIInputType(final String type, final String viewName, final int level) {

		final Map<String, Object> map = new TreeMap<>();
		final Class valueType         = valueType();

		if (valueType != null) {

			String valueTypeName = valueType.getSimpleName().toLowerCase();

			if (StringUtils.equals(valueTypeName, "double")) {
				valueTypeName = "number";
			}

			map.put("type", valueTypeName);
			map.put("example", getExampleValue(type, viewName));

			if (this.isReadOnly()) {
				map.put("readOnly", true);
			}
		}

		return map;
	}

	// ----- private methods -----
	private void updateAccessInformation(final SecurityContext securityContext, final PropertyContainer propertyContainer) throws FrameworkException {

		try {

			if (securityContext.modifyAccessTime()) {

				final Principal user = securityContext.getUser(false);
				String modifiedById  = null;

				if (user != null) {

					if (user instanceof SuperUser) {

						// "virtual" UUID of superuser
						modifiedById = Principal.SUPERUSER_ID;

					} else {

						modifiedById = user.getUuid();
					}

					propertyContainer.setProperty(GraphObjectTraitDefinition.LAST_MODIFIED_BY_PROPERTY, modifiedById);
				}

				propertyContainer.setProperty(GraphObjectTraitDefinition.LAST_MODIFIED_DATE_PROPERTY, System.currentTimeMillis());
			}


		} catch (Throwable t) {

			// fail without throwing an exception here
			logger.warn("", t);
		}
	}

	private Transformer getTransformator(final String fqcn) {

		try {

			final Class clazz = Class.forName(fqcn);

			return (Transformer)clazz.getConstructor().newInstance();

		} catch (Throwable t) {
		}

		return null;
	}
}
