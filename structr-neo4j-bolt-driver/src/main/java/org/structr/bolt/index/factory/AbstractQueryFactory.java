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
package org.structr.bolt.index.factory;

import org.structr.api.search.TypeConverter;
import org.structr.bolt.index.AbstractCypherIndex;

/**
 *
 * @author Christian Morgner
 */
public abstract class AbstractQueryFactory implements QueryFactory {

	protected Object getReadValue(final Object value) {

		if (value != null) {

			final TypeConverter converter = AbstractCypherIndex.CONVERTERS.get(value.getClass());
			if (converter != null) {

				return converter.getReadValue(value);
			}

			// use string value of enum types
			if (value.getClass().isEnum()) {
				return value.toString();
			}
		}

		return value;
	}

	protected Object getWriteValue(final Object value) {

		if (value != null) {

			final TypeConverter converter = AbstractCypherIndex.CONVERTERS.get(value.getClass());
			if (converter != null) {

				return converter.getWriteValue(value);
			}
		}

		return value;
	}
}
