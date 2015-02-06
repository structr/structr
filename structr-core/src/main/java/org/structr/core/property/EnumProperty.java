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

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.ValueToken;
import org.structr.core.GraphObject;
import org.structr.core.PropertyValidator;
import org.structr.core.converter.PropertyConverter;

/**
 * A property that stores and retrieves a simple enum value of the given type.
 *
 * @author Christian Morgner
 */
public class EnumProperty<T extends Enum> extends AbstractPrimitiveProperty<T> {

	private Class<T> enumType = null;

	public EnumProperty(String name, Class<T> enumType, final PropertyValidator<T>... validators) {
		this(name, enumType, null, validators);
	}

	public EnumProperty(String name, Class<T> enumType, T defaultValue, final PropertyValidator<T>... validators) {
		this(name, name, enumType, defaultValue, validators);
	}

	public EnumProperty(String jsonName, String dbName, Class<T> enumType, T defaultValue, final PropertyValidator<T>... validators) {

		super(jsonName, dbName, defaultValue);

		this.enumType = enumType;
		addEnumValuesToFormat();

		for (final PropertyValidator<T> validator : validators) {
			addValidator(validator);
		}
	}

	@Override
	public String typeName() {
		return "Enum";
	}

	@Override
	public Integer getSortType() {
		return null;
	}

	@Override
	public PropertyConverter<T, String> databaseConverter(SecurityContext securityContext) {
		return databaseConverter(securityContext, null);
	}

	@Override
	public PropertyConverter<T, String> databaseConverter(SecurityContext securityContext, GraphObject entity) {
		return new DatabaseConverter(securityContext, entity);
	}

	@Override
	public PropertyConverter<String, T> inputConverter(SecurityContext securityContext) {
		return new InputConverter(securityContext);
	}

	@Override
	public Object fixDatabaseProperty(Object value) {

		if (value != null) {

			if (value instanceof String) {
				return value;
			}
		}

		return null;
	}

	public Class<T> getEnumType() {
		return enumType;
	}

	protected class DatabaseConverter extends PropertyConverter<T, String> {

		public DatabaseConverter(SecurityContext securityContext, GraphObject entity) {
			super(securityContext, entity);
		}

		@Override
		public T revert(String source) throws FrameworkException {

			if (source != null) {

				return (T) Enum.valueOf(enumType, source);
			}

			return null;

		}

		@Override
		public String convert(T source) throws FrameworkException {

			if (source != null) {

				return source.toString();
			}

			return null;
		}

	}

	protected class InputConverter extends PropertyConverter<String, T> {

		public InputConverter(SecurityContext securityContext) {
			super(securityContext, null);
		}

		@Override
		public String revert(T source) throws FrameworkException {

			if (source != null) {

				return source.toString();
			}

			return null;
		}

		@Override
		public T convert(String source) throws FrameworkException {

			if (source != null) {

				try {
					return (T) Enum.valueOf(enumType, source.toString());

				} catch (Throwable t) {

					throw new FrameworkException(declaringClass.getSimpleName(), new ValueToken(EnumProperty.this, enumType.getEnumConstants()));
				}
			}

			return null;

		}

	}

	private void addEnumValuesToFormat() {

		this.format = "";

		for (T enumConst : enumType.getEnumConstants()) {
			this.format += (enumConst.toString()) + ",";
		}

		this.format = this.format.substring(0, this.format.length() - 1);
	}
}
