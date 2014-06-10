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

import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.SortField;
import org.apache.lucene.util.NumericUtils;
import org.neo4j.index.lucene.ValueContext;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.NumberToken;
import org.structr.core.GraphObject;
import org.structr.core.PropertyValidator;
import org.structr.core.app.Query;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.graph.search.IntegerSearchAttribute;
import org.structr.core.graph.search.SearchAttribute;

/**
* A property that stores and retrieves a simple Integer value.
 *
 * @author Christian Morgner
 */
public class IntProperty extends AbstractPrimitiveProperty<Integer> {

	private static final Logger logger = Logger.getLogger(IntProperty.class.getName());

	public static final String INT_EMPTY_FIELD_VALUE = NumericUtils.intToPrefixCoded(Integer.MIN_VALUE);

	public IntProperty(String name) {
		this(name, name, null);
	}

	public IntProperty(final String jsonName, final String dbName) {
		this(jsonName, dbName, null);
	}

	public IntProperty(String name, PropertyValidator<Integer>... validators) {
		this(name, name, null, validators);
	}

	public IntProperty(String name, Integer defaultValue, PropertyValidator<Integer>... validators) {
		this(name, name, defaultValue, validators);
	}

	public IntProperty(String jsonName, String dbName, Integer defaultValue, PropertyValidator<Integer>... validators) {
		super(jsonName, dbName, defaultValue);

		for (PropertyValidator<Integer> validator : validators) {
			addValidator(validator);
		}
	}

	@Override
	public String typeName() {
		return "Integer";
	}

	@Override
	public Integer getSortType() {
		return SortField.INT;
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

					logger.log(Level.WARNING, "Unable to convert {0} to Integer.", source);

					throw new FrameworkException(declaringClass.getSimpleName(), new NumberToken(IntProperty.this));
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
	public SearchAttribute getSearchAttribute(SecurityContext securityContext, BooleanClause.Occur occur, Integer searchValue, boolean exactMatch, final Query query) {
		return new IntegerSearchAttribute(this, searchValue, occur, exactMatch);
	}

	@Override
	public void index(GraphObject entity, Object value) {
		super.index(entity, value != null ? ValueContext.numeric((Number)fixDatabaseProperty(value)) : value);
	}

	@Override
	public String getValueForEmptyFields() {
		return INT_EMPTY_FIELD_VALUE;
	}

}