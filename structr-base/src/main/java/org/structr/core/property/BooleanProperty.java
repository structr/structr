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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.search.SortType;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.PropertyInputParsingException;
import org.structr.common.error.ValueToken;
import org.structr.core.GraphObject;
import org.structr.core.converter.PropertyConverter;
import org.structr.docs.DocumentableType;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
* A property that stores and retrieves a simple Boolean value.
 */
public class BooleanProperty extends AbstractPrimitiveProperty<Boolean> {

	private static final Logger logger = LoggerFactory.getLogger(BooleanProperty.class.getName());
	private static final Set<String> TRUE_VALUES     = new LinkedHashSet<>(Arrays.asList("true", "1", "on"));
	private static final Set<String> FALSE_VALUES    = new LinkedHashSet<>(Arrays.asList("false", "0", "off"));
	private static final Set<String> ACCEPTED_VALUES = new LinkedHashSet<>(Arrays.asList("true", "false", "1", "0", "on", "off"));

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
	public String editTemplate() {

		// a boolean edits as a checkbox, not the generic textfield (Property's default)

		return "checkbox";
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

						logger.error("Cound not set fixed property {} on graph object {}", fixedValue, entity);
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

	// ----- interface Documentable -----
	@Override
	public String getShortDescription() {

		return "A property for boolean values.";
	}

	@Override
	public String getLongDescription() {

		return null;
	}

	// ----- OpenAPI -----
	@Override
	public Object getExampleValue(final int index) {

		return index % 2 == 0;
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

					logger.warn("Wrong database type for {}. Expected: {}, found: {}", dbName, Boolean.class.getName(), source.getClass().getName());

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
		public Boolean convert(final Object source) throws FrameworkException {

			// no value means false, as everywhere else for a boolean
			if (source == null) {

				return false;
			}

			if (source instanceof Boolean booleanValue) {

				return booleanValue;
			}

			// Accept the textual and numerical spellings of both values, i.e. "true"/"on"/"1"/1 and their
			// counterparts. Anything else is rejected instead of silently becoming false: an input that is
			// not a boolean at all says nothing about which value was meant (see PropertyInputParsingException
			// for the same treatment of unparsable numbers in IntProperty).
			final String stringValue = source.toString().toLowerCase();

			// A blank string means "no value given", the same way IntProperty ignores blank input. This is
			// deliberately limited to strings: an object or array whose textual form happens to be empty is
			// still the wrong type, not an omitted value.
			if (source instanceof String && StringUtils.isBlank(stringValue)) {

				return false;
			}

			if (TRUE_VALUES.contains(stringValue)) {

				return true;
			}

			if (FALSE_VALUES.contains(stringValue)) {

				return false;
			}

			throw new PropertyInputParsingException(BooleanProperty.this.jsonName(), new ValueToken(declaringTrait.getLabel(), BooleanProperty.this.jsonName(), ACCEPTED_VALUES));
		}
	}
}
