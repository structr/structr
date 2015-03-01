/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
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

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.converter.PropertyConverter;

/**
* A property that stores and retrieves a simple Boolean value.
 *
 * @author Christian Morgner
 */
public class BooleanProperty extends AbstractPrimitiveProperty<Boolean> {

	private static final Logger logger = Logger.getLogger(BooleanProperty.class.getName());
	private static final Set<String> TRUE_VALUES = new LinkedHashSet<>(Arrays.asList(new String[] { "true", "1", "on" }));

	public BooleanProperty(final String name) {
		super(name);
	}

	public BooleanProperty(final String jsonName, final String dbName) {
		super(jsonName, dbName);
	}
//
//	public BooleanProperty(final String name, final Boolean defaultValue) {
//		this(name, name, defaultValue);
//	}
//
//	public BooleanProperty(final String name, final PropertyValidator<Boolean>... validators) {
//		this(name, name, null, validators);
//	}
//
//	public BooleanProperty(final String name, final Boolean defaultValue, final PropertyValidator<Boolean>... validators) {
//		this(name, name, defaultValue, validators);
//	}
//
//	public BooleanProperty(final String jsonName, final String dbName, final Boolean defaultValue, final PropertyValidator<Boolean>... validators) {
//		super(jsonName, dbName, defaultValue);
//
//		for (PropertyValidator<Boolean> validator : validators) {
//			addValidator(validator);
//		}
//	}

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
	public Integer getSortType() {
		return null;
	}

	@Override
	public PropertyConverter<Boolean, ?> databaseConverter(SecurityContext securityContext) {
		return databaseConverter(securityContext, null);
	}

	@Override
	public PropertyConverter<Boolean, ?> databaseConverter(SecurityContext securityContext, GraphObject entity) {
		return new DatabaseConverter(securityContext);
	}

	@Override
	public PropertyConverter<?, Boolean> inputConverter(SecurityContext securityContext) {
		return new InputConverter(securityContext);
	}

	@Override
	public Object fixDatabaseProperty(Object value) {

		if (value != null) {

			if (value instanceof Boolean) {
				return value;
			}

			if (value instanceof String) {

				return TRUE_VALUES.contains(value.toString().toLowerCase());
			}
		}

		return false;
	}

	protected class DatabaseConverter extends PropertyConverter<Boolean, Object> {

		public DatabaseConverter(SecurityContext securityContext) {
			super(securityContext);
		}

		@Override
		public Boolean revert(Object source) throws FrameworkException {

			if (source != null) {

				if (!(source instanceof Boolean)) {

					logger.log(Level.WARNING, "Wrong database type for {0}. Expected: {1}, found: {2}", new Object[]{dbName, Boolean.class.getName(), source.getClass().getName()});

					return (Boolean) fixDatabaseProperty(source);

				}

				return (Boolean) source;
			}

			return defaultValue != null ? defaultValue : false;
		}

		@Override
		public Boolean convert(Boolean source) {

			if (source != null) {
				return source;
			}

			return false;
		}
	}

	protected class InputConverter extends PropertyConverter<Object, Boolean> {

		public InputConverter(SecurityContext securityContext) {
			super(securityContext);
		}

		@Override
		public Object revert(Boolean source) throws FrameworkException {

			if (source != null) {
				return source;
			}

			return false;
		}

		@Override
		public Boolean convert(Object source) {

			boolean returnValue = false;

			// FIXME: be more strict when dealing with "wrong" input types
			if (source != null) {

				if (source instanceof Boolean) {
					return (Boolean)source;
				}

				if (source instanceof String) {

					// don't log this
					// logger.log(Level.WARNING, "Wrong input type for {0}. Expected: {1}, found: {2}", new Object[]{jsonName, Boolean.class.getName(), source.getClass().getName()});

					returnValue = TRUE_VALUES.contains(source.toString().toLowerCase());

				}
			}

			return returnValue;
		}
	}
}
