/*
 * Copyright (C) 2010-2026 Structr GmbH
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
import org.structr.api.search.SortType;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.converter.PropertyConverter;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
* A property that stores and retrieves a simple Boolean value.
 */
public class BooleanProperty extends AbstractPrimitiveProperty<Boolean> {

	private static final Logger logger = LoggerFactory.getLogger(BooleanProperty.class.getName());
	private static final Set<String> TRUE_VALUES = new LinkedHashSet<>(Arrays.asList(new String[] { "true", "1", "on" }));

	public BooleanProperty(final String name) {
		super(name);
	}

	public BooleanProperty(final String jsonName, final String dbName) {
		super(jsonName, dbName);
	}

	@Override
	public Property<Boolean> indexed() {
		return super.passivelyIndexed();
	}

	@Override
	public String typeName() {
		return "Boolean";
	}

	@Override
	public Class valueType() {
		return Boolean.class;
	}

	@Override
	public SortType getSortType() {
		return SortType.Default;
	}

	@Override
	public PropertyConverter<Boolean, ?> databaseConverter(final SecurityContext securityContext) {
		return databaseConverter(securityContext, null);
	}

	@Override
	public PropertyConverter<Boolean, ?> databaseConverter(final SecurityContext securityContext, final GraphObject entity) {
		this.securityContext = securityContext;
		this.entity          = entity;
		return new DatabaseConverter(securityContext);
	}

	@Override
	public PropertyConverter<?, Boolean> inputConverter(final SecurityContext securityContext, boolean fromString) {
		return new InputConverter(securityContext);
	}

	@Override
	public Object fixDatabaseProperty(final Object value) {

		final boolean fixedValue;

		if (value != null) {

			if (value instanceof Boolean) {
				return value;
			}

			if (value instanceof String) {

				fixedValue = TRUE_VALUES.contains(value.toString().toLowerCase());

				if (entity != null) {

					try {
						setProperty(securityContext, entity, fixedValue);

					} catch (FrameworkException fex) {
						logger.error("Cound not set fixed property {} on graph object {}", new Object[]{fixedValue, entity});
					}
				}

				return fixedValue;
			}
		}

		return false;
	}

	@Override
	public boolean isArray() {
		return false;
	}

	// ----- OpenAPI -----
	@Override
	public Object getExampleValue(final String type, final String viewName) {
		return true;
	}

	@Override
	public Map<String, Object> describeOpenAPIOutputSchema(String type, String viewName) {
		return null;
	}

	// ----- nested classes -----
	protected class DatabaseConverter extends PropertyConverter<Boolean, Object> {

		public DatabaseConverter(final SecurityContext securityContext) {
			super(securityContext);
		}

		@Override
		public Boolean revert(Object source) throws FrameworkException {

			if (source != null) {

				if (!(source instanceof Boolean)) {

					logger.warn("Wrong database type for {}. Expected: {}, found: {}", new Object[]{dbName, Boolean.class.getName(), source.getClass().getName()});

					return (Boolean) fixDatabaseProperty(source);

				}

				return (Boolean) source;
			}

			return defaultValue != null ? defaultValue : false;
		}

		@Override
		public Boolean convert(final Boolean source) {

			if (source != null) {
				return source;
			}

			return false;
		}
	}

	protected class InputConverter extends PropertyConverter<Object, Boolean> {

		public InputConverter(final SecurityContext securityContext) {
			super(securityContext);
		}

		@Override
		public Object revert(final Boolean source) throws FrameworkException {

			if (source != null) {
				return source;
			}

			return false;
		}

		@Override
		public Boolean convert(final Object source) {

			boolean returnValue = false;

			// FIXME: be more strict when dealing with "wrong" input types
			if (source != null) {

				if (source instanceof Boolean) {
					return (Boolean)source;
				}

				if (source instanceof String) {

					// don't log this
					// logger.warn("Wrong input type for {}. Expected: {}, found: {}", new Object[]{jsonName, Boolean.class.getName(), source.getClass().getName()});

					returnValue = TRUE_VALUES.contains(source.toString().toLowerCase());

				}
			}

			return returnValue;
		}
	}
}
