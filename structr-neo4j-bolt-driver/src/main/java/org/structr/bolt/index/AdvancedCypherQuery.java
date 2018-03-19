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
package org.structr.bolt.index;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.structr.api.search.QueryContext;
import org.structr.api.search.SortType;

/**
 *
 */
public class AdvancedCypherQuery implements PageableQuery {

	private final Map<String, Object> parameters = new HashMap<>();
	private final List<String> typeLabels        = new LinkedList<>();
	private final StringBuilder buffer           = new StringBuilder();
	private String sourceTypeLabel               = null;
	private String targetTypeLabel               = null;
	private AbstractCypherIndex<?> index         = null;
	private boolean sortDescending               = false;
	private SortType sortType                    = null;
	private String sortKey                       = null;
	private int page                             = 0;
	private int pageSize                         = 0;
	private int count                            = 0;

	public AdvancedCypherQuery(final QueryContext queryContext, final AbstractCypherIndex<?> index) {
		this.pageSize = queryContext.getPageSize();
		this.index    = index;
	}

	@Override
	public String toString() {
		return getStatement();
	}

	public int getHashCode() {

		int hashCode = 23;

		hashCode += 27 * typeLabels.hashCode();
		hashCode += 37 * getStatement().hashCode();
		hashCode += 47 * deepHashCode(parameters);

		if (sortKey != null) {
			hashCode += 57 * sortKey.hashCode();
		}

		if (sortDescending) {
			hashCode += 1;
		}

		return hashCode;
	}

	@Override
	public void nextPage() {
		page++;
	}

	@Override
	public int pageSize() {
		return this.pageSize;
	}

	@Override
	public String getStatement() {

		final StringBuilder buf = new StringBuilder();
		final int typeCount     = typeLabels.size();

		switch (typeCount) {

			case 0:

				buf.append(index.getQueryPrefix(null, sourceTypeLabel, targetTypeLabel));

				if (buffer.length() > 0) {
					buf.append(" WHERE ");
					buf.append(buffer);
				}

				buf.append(index.getQuerySuffix());
				break;

			case 1:

				buf.append(index.getQueryPrefix(typeLabels.get(0), sourceTypeLabel, targetTypeLabel));

				if (buffer.length() > 0) {
					buf.append(" WHERE ");
					buf.append(buffer);
				}

				buf.append(index.getQuerySuffix());
				break;

			default:

				// create UNION query
				for (final Iterator<String> it = typeLabels.iterator(); it.hasNext();) {

					buf.append(index.getQueryPrefix(it.next(), sourceTypeLabel, targetTypeLabel));

					if (buffer.length() > 0) {
						buf.append(" WHERE ");
						buf.append(buffer);
					}

					buf.append(index.getQuerySuffix());

					if (it.hasNext()) {
						buf.append(" UNION ");
					}
				}
				break;
		}

		if (sortKey != null) {

			switch (sortType) {

				case Default:
					// default is "String"
					// no COALESCE needed => much faster
					buf.append(" ORDER BY n.`");
					buf.append(sortKey);
					buf.append("` ");

					break;

				default:
					// other types are numeric
					buf.append(" ORDER BY COALESCE(n.`");
					buf.append(sortKey);
					buf.append("`, ");

					// COALESCE needs a correctly typed minimum value,
					// so we need to supply a value based on the sort
					// type.

					buf.append("-1");
					buf.append(")");
			}

			if (sortDescending) {
				buf.append(" DESC");
			}
		}

		buf.append(" SKIP ");
		buf.append(page * pageSize);
		buf.append(" LIMIT ");
		buf.append(pageSize);

		return buf.toString();
	}

	@Override
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

	public void not() {
		buffer.append(" NOT ");
	}

	public void andNot() {
		buffer.append(" AND NOT ");
	}

	public void or() {
		buffer.append(" OR ");
	}

	public void noop() {
		buffer.append(" True ");
	}

	public void typeLabel(final String typeLabel) {
		this.typeLabels.add(typeLabel);
	}

	public void addSimpleParameter(final String key, final String operator, final Object value) {
		addSimpleParameter(key, operator, value, true);
	}

	public void addSimpleParameter(final String key, final String operator, final Object value, final boolean isProperty) {
		addSimpleParameter(key, operator, value, isProperty, false);
	}

	public void addSimpleParameter(final String key, final String operator, final Object value, final boolean isProperty, final boolean caseInsensitive) {

		if (value != null) {

			final String paramKey = "param" + count++;

			if (isProperty) {

				if (caseInsensitive) {
					buffer.append("toLower(");
				}

				buffer.append("n.`");
			}

			buffer.append(key);

			if (isProperty) {

				if (caseInsensitive) {
					buffer.append("`) ");
				} else {
					buffer.append("` ");
				}

			} else {

				buffer.append(" ");
			}

			buffer.append(operator);
			buffer.append(" {");
			buffer.append(paramKey);
			buffer.append("}");

			parameters.put(paramKey, caseInsensitive && value instanceof String ? ((String) value).toLowerCase() : value);

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

		buffer.append("(n.`");
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
		buffer.append("})");

		parameters.put(paramKey1, value1);
		parameters.put(paramKey2, value2);
	}

	public void sort(final SortType sortType, final String sortKey, final boolean sortDescending) {

		this.sortDescending = sortDescending;
		this.sortType       = sortType;
		this.sortKey        = sortKey;
	}

	public void setSourceType(final String sourceTypeLabel) {
		this.sourceTypeLabel = sourceTypeLabel;
	}

	public void setTargetType(final String targetTypeLabel) {
		this.targetTypeLabel = targetTypeLabel;
	}

	private int deepHashCode(final Map<String, Object> map) {

		final StringBuilder buf = new StringBuilder();

		for (final Entry<String, Object> entry : map.entrySet()) {

			buf.append("|");
			buf.append(entry.getKey());
			buf.append("|");
			buf.append(entry.getValue());
		}

		return buf.toString().hashCode();
	}
}
