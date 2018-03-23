/**
 * Copyright (C) 2010-2018 Structr GmbH
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
package org.structr.schema.graphql;

import java.util.Map;
import org.structr.core.app.StructrApp;
import org.structr.core.property.PropertyKey;

/**
 * Base class for data fetchers in Structr's GraphQL implementation.
 * This class contains convenience methods needed in all fetchers.
 */
public class AbstractDataFetcher {

	protected PropertyKey sortKey             = null;
	protected boolean sortDescending          = false;
	protected int pageSize                    = Integer.MAX_VALUE;
	protected int page                        = 1;

	protected void initialize(final Class type, final Map<String, Object> args) {

		pageSize       = parseInt(args.get("_pageSize"), Integer.MAX_VALUE);
		page           = parseInt(args.get("_page"), 1);
		sortKey        = getPropertyKey(type, args.get("_sort"));
		sortDescending = parseBoolean(args.get("_desc"), false);

		args.remove("_pageSize");
		args.remove("_page");
		args.remove("_sort");
		args.remove("_desc");
	}

	protected void assertObjectNonNullAndOfType(final Object value, final Class type) {

		if (value == null) {

			throw new IllegalArgumentException("Data value must not be null.");
		}

		if (!type.isAssignableFrom(value.getClass())) {

			throw new IllegalArgumentException("Data value " + value + " must be of type " + type.getSimpleName());
		}
	}

	protected <T> T get(final T value, final T defaultValue) {

		if (value != null) {
			return value;
		}

		return defaultValue;
	}

	protected Integer parseInt(final Object source, final Integer defaultValue) {

		if (source instanceof Integer) {

			return ((Integer)source);
		}

		if (source instanceof Number) {

			return ((Number)source).intValue();
		}

		if (source instanceof String) {

			return Integer.parseInt((String)source);
		}

		return defaultValue;
	}

	protected Boolean parseBoolean(final Object source, final Boolean defaultValue) {

		if (source instanceof Boolean) {

			return ((Boolean)source);
		}

		if (source instanceof String) {

			return Boolean.parseBoolean((String)source);
		}

		return defaultValue;
	}

	protected PropertyKey getPropertyKey(final Class type, final Object name) {

		if (type != null && name != null && name instanceof String) {

			return StructrApp.key(type, (String)name);
		}

		return null;
	}
}
