/**
 * Copyright (C) 2010-2016 Structr GmbH
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
package org.structr.neo4j.index.lucene.converter;

import org.apache.lucene.util.NumericUtils;
import org.neo4j.index.lucene.ValueContext;
import org.structr.api.search.TypeConverter;

/**
 *
 */
public class LongTypeConverter implements TypeConverter {

	private static final Long EMPTY_VALUE = Long.MIN_VALUE;

	@Override
	public Object getReadValue(final Object value) {

		if (value != null && value instanceof Long) {

			return NumericUtils.longToPrefixCoded((Long)value);
		}

		return NumericUtils.longToPrefixCoded(EMPTY_VALUE);
	}

	@Override
	public Object getWriteValue(final Object value) {

		if (value != null && value instanceof Long) {

			return ValueContext.numeric((Long)value);
		}

		return ValueContext.numeric(EMPTY_VALUE);
	}

	@Override
	public Object getInexactValue(final Object value) {
		return getReadValue(value).toString().toLowerCase();
	}
}
