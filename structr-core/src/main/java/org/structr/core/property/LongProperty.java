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
import org.structr.core.graph.search.LongSearchAttribute;
import org.structr.core.graph.search.SearchAttribute;

/**
 * A property that stores and retrieves a simple Long value.
 *
 * @author Christian Morgner
 */
public class LongProperty extends AbstractPrimitiveProperty<Long> {

	private static final Logger logger = Logger.getLogger(DoubleProperty.class.getName());

	public static final String LONG_EMPTY_FIELD_VALUE = NumericUtils.longToPrefixCoded(Long.MIN_VALUE);

	public LongProperty(String name) {
		this(name, name, null);
	}

	public LongProperty(final String jsonName, final String dbName) {
		this(jsonName, dbName, null);
	}

	public LongProperty(String name, final PropertyValidator<Long>... validators) {
		this(name, name, null, validators);
	}

	public LongProperty(String name, Long defaultValue, PropertyValidator<Long>... validators) {
		this(name, name, defaultValue, validators);
	}

	public LongProperty(String jsonName, String dbName, Long defaultValue, PropertyValidator<Long>... validators) {
		super(jsonName, dbName, defaultValue);

		for (PropertyValidator<Long> validator : validators) {
			addValidator(validator);
		}
	}

	@Override
	public String typeName() {
		return "Long";
	}

	@Override
	public Integer getSortType() {
		return SortField.LONG;
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

					logger.log(Level.WARNING, "Unable to convert {0} to Long.", source);

					throw new FrameworkException(declaringClass.getSimpleName(), new NumberToken(LongProperty.this));
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
	public SearchAttribute getSearchAttribute(SecurityContext securityContext, BooleanClause.Occur occur, Long searchValue, boolean exactMatch, final Query query) {
		return new LongSearchAttribute(this, searchValue, occur, exactMatch);
	}

	@Override
	public void index(GraphObject entity, Object value) {
		super.index(entity, value != null ? ValueContext.numeric((Number)fixDatabaseProperty(value)) : value);
	}

	@Override
	public String getValueForEmptyFields() {
		return LONG_EMPTY_FIELD_VALUE;
	}


}
