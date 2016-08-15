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
package org.structr.bolt.index;

import java.util.HashMap;
import java.util.Map;
import org.structr.api.search.SortType;

/**
 *
 * @author Christian Morgner
 */
public class CypherQuery {

	private final Map<String, Object> parameters = new HashMap<>();
	private final StringBuilder buffer           = new StringBuilder();
	private boolean sortDescending               = false;
	private SortType sortType                    = null;
	private String sortKey                       = null;
	private String suffix                        = null;
	private String prefix                        = null;
	private int count                            = 0;

	public CypherQuery(final String prefix, final String suffix) {
		this.prefix = prefix;
		this.suffix = suffix;
	}

	@Override
	public String toString() {
		return getStatement();
	}

	public String getStatement() {

		final StringBuilder buf = new StringBuilder();

		buf.append(prefix);
		buf.append(buffer);
		buf.append(suffix);

		if (sortKey != null) {

			buf.append(" ORDER BY COALESCE(n.`");
			buf.append(sortKey);
			buf.append("`, ");

			// COALESCE needs a correctly typed minimum value,
			// so we need to supply a value based on the sort
			// type.

			switch (sortType) {

				case Default:
					// default is "String"
					buf.append("''");
					break;

				default:
					// other types are numeric
					buf.append("-1");
			}

			buf.append(")");

			if (sortDescending) {
				buf.append(" DESC");
			}
		}

		return buf.toString();
	}

	public Map<String, Object> getParameters() {
		return parameters;
	}

	public void beginGroup() {
		buffer.append("(");
	}

	public void endGroup() {
		buffer.append(")");
	}

	public void and() {
		buffer.append(" AND ");
	}

	public void andNot() {
		buffer.append(" AND NOT ");
	}

	public void or() {
		buffer.append(" OR ");
	}

	public void addSimpleParameter(final String key, final String operator, final Object value) {
		addSimpleParameter(key, operator, value, true);
	}

	public void addSimpleParameter(final String key, final String operator, final Object value, final boolean isProperty) {

		if (value != null) {

			final String paramKey = "param" + count++;

			if (isProperty) {
				buffer.append("n.`");
			}

			buffer.append(key);

			if (isProperty) {
				buffer.append("` ");
			}

			buffer.append(operator);
			buffer.append(" {");
			buffer.append(paramKey);
			buffer.append("}");

			parameters.put(paramKey, value);

		} else {

			if (isProperty) {
				buffer.append("n.`");
			}

			buffer.append(key);

			if (isProperty) {
				buffer.append("` ");
			}

			buffer.append(operator);
			buffer.append(" Null");
		}
	}

	public void addListParameter(final String key, final String operator, final Object value) {

		if (value != null) {

			final String paramKey = "param" + count++;

			buffer.append("ANY(x IN n.`");
			buffer.append(key);
			buffer.append("` WHERE x ");
			buffer.append(operator);
			buffer.append(" {");
			buffer.append(paramKey);
			buffer.append("})");

			parameters.put(paramKey, value);

		} else {

			buffer.append("ANY(x IN n.`");
			buffer.append(key);
			buffer.append("` WHERE x ");
			buffer.append(operator);
			buffer.append(" Null)");
		}
	}

	public void addParameters(final String key, final String operator1, final Object value1, final String operator2, final Object value2) {

		final String paramKey1 = "param" + count++;
		final String paramKey2 = "param" + count++;

		buffer.append("n.`");
		buffer.append(key);
		buffer.append("` ");
		buffer.append(operator1);
		buffer.append(" {");
		buffer.append(paramKey1);
		buffer.append("}");
		buffer.append(" AND ");
		buffer.append("n.`");
		buffer.append(key);
		buffer.append("` ");
		buffer.append(operator2);
		buffer.append(" {");
		buffer.append(paramKey2);
		buffer.append("}");

		parameters.put(paramKey1, value1);
		parameters.put(paramKey2, value2);
	}

	public void sort(final SortType sortType, final String sortKey, final boolean sortDescending) {

		this.sortDescending = sortDescending;
		this.sortType       = sortType;
		this.sortKey        = sortKey;
	}
}
