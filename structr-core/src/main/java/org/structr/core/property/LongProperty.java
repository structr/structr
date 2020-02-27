/**
 * Copyright (C) 2010-2020 Structr GmbH
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

import org.apache.chemistry.opencmis.commons.enums.PropertyType;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.search.SortType;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.NumberToken;
import org.structr.core.GraphObject;
import org.structr.core.converter.PropertyConverter;

/**
 * A property that stores and retrieves a simple Long value.
 *
 *
 */
public class LongProperty extends AbstractPrimitiveProperty<Long> implements NumericalPropertyKey<Long> {

	private static final Logger logger = LoggerFactory.getLogger(DoubleProperty.class.getName());

	public LongProperty(final String name) {
		super(name);
	}

	@Override
	public String typeName() {
		return "Long";
	}

	@Override
	public Class valueType() {
		return Long.class;
	}

	@Override
	public SortType getSortType() {
		return SortType.Long;
	}

	@Override
	public PropertyConverter<Long, Long> databaseConverter(SecurityContext securityContext) {
		return null;
	}

	@Override
	public PropertyConverter<Long, Long> databaseConverter(SecurityContext securityContext, GraphObject entity) {
		return null;
	}

	@Override
	public PropertyConverter<?, Long> inputConverter(SecurityContext securityContext) {
		return new InputConverter(securityContext);
	}

	@Override
	public Long convertToNumber(Double source) {

		if (source != null) {
			return source.longValue();
		}

		return null;
	}

	protected class InputConverter extends PropertyConverter<Object, Long> {

		public InputConverter(SecurityContext securityContext) {
			super(securityContext);
		}

		@Override
		public Object revert(Long source) throws FrameworkException {
			return source;
		}

		@Override
		public Long convert(Object source) throws FrameworkException {

			if (source == null) return null;

			if (source instanceof Number) {

				return ((Number)source).longValue();

			}

			if (source instanceof String && StringUtils.isNotBlank((String) source)) {

				try {
					return Long.valueOf(source.toString());

				} catch (Throwable t) {

					throw new FrameworkException(422, "Cannot parse input " + source + " for property " + jsonName(), new NumberToken(declaringClass.getSimpleName(), LongProperty.this));
				}
			}

			return null;
		}
	}

	@Override
	public Object fixDatabaseProperty(Object value) {

		if (value != null) {

			if (value instanceof Long) {
				return value;
			}

			if (value instanceof Number) {
				return ((Number)value).longValue();
			}

			try {

				return Long.parseLong(value.toString());

			} catch (Throwable t) {

				// no chance, give up..
			}
		}

		return null;
	}

	@Override
	public Object getIndexValue(final Object value) {
		return fixDatabaseProperty(value);
	}

	// ----- CMIS support -----
	@Override
	public PropertyType getDataType() {
		return PropertyType.INTEGER;
	}
}
