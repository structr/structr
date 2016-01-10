/**
 * Copyright (C) 2010-2015 Structr GmbH
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

import java.util.logging.Logger;
import org.apache.chemistry.opencmis.commons.enums.PropertyType;
import org.apache.commons.lang3.StringUtils;
import org.structr.api.search.SortType;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.NumberToken;
import org.structr.core.GraphObject;
import org.structr.core.converter.PropertyConverter;

/**
* A property that stores and retrieves a simple Integer value.
 *
 *
 */
public class IntProperty extends AbstractPrimitiveProperty<Integer> implements NumericalPropertyKey<Integer> {

	private static final Logger logger = Logger.getLogger(IntProperty.class.getName());

	public IntProperty(final String name) {
		super(name);
	}

	@Override
	public String typeName() {
		return "Integer";
	}

	@Override
	public Class valueType() {
		return Integer.class;
	}

	@Override
	public SortType getSortType() {
		return SortType.Integer;
	}

	@Override
	public PropertyConverter<Integer, Integer> databaseConverter(SecurityContext securityContext) {
		return null;
	}

	@Override
	public PropertyConverter<Integer, Integer> databaseConverter(SecurityContext securityContext, GraphObject entity) {
		return null;
	}

	@Override
	public PropertyConverter<?, Integer> inputConverter(SecurityContext securityContext) {
		return new InputConverter(securityContext);
	}

	@Override
	public Integer convertToNumber(Double source) {

		if (source != null) {
			return source.intValue();
		}

		return null;
	}

	protected class InputConverter extends PropertyConverter<Object, Integer> {

		public InputConverter(SecurityContext securityContext) {
			super(securityContext, null);
		}

		@Override
		public Object revert(Integer source) throws FrameworkException {
			return source;
		}

		@Override
		public Integer convert(Object source) throws FrameworkException {

			if (source == null) return null;

			if (source instanceof Number) {

				return ((Number)source).intValue();
			}

			if (source instanceof String && StringUtils.isNotBlank((String) source)) {

				try {
					return Double.valueOf(source.toString()).intValue();

				} catch (Throwable t) {

					throw new FrameworkException(422, new NumberToken(declaringClass.getSimpleName(), IntProperty.this));
				}
			}

			return null;

		}
	}

	@Override
	public Object fixDatabaseProperty(Object value) {

		if (value != null) {

			if (value instanceof Integer) {
				return value;
			}

			if (value instanceof Number) {
				return ((Number)value).intValue();
			}

			try {

				return Double.valueOf(value.toString()).intValue();

			} catch (Throwable t) {

				// no chance, give up..
			}
		}

		return null;
	}

	@Override
	public void index(GraphObject entity, Object value) {
		super.index(entity, fixDatabaseProperty(value));
	}

	// ----- CMIS support -----
	@Override
	public PropertyType getDataType() {
		return PropertyType.INTEGER;
	}

}