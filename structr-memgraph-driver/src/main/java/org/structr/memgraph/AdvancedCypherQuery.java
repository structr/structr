/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.memgraph;

import org.structr.api.config.Settings;
import org.structr.api.search.QueryContext;
import org.structr.api.search.SortOrder;
import org.structr.api.search.SortSpec;
import org.structr.api.util.Iterables;

import java.util.*;

/**
 *
 */
public class AdvancedCypherQuery implements CypherQuery {

	private final Map<String, Object> parameters    = new HashMap<>();
	private final Set<String> indexLabels           = new LinkedHashSet<>();
	private final Set<String> typeLabels            = new LinkedHashSet<>();
	private final StringBuilder buffer              = new StringBuilder();
	private final int fetchSize                     = Settings.FetchSize.getValue();
	private String sourceTypeLabel                  = null;
	private String targetTypeLabel                  = null;
	private AbstractCypherIndex<?> index            = null;
	private SortOrder sortOrder                     = null;
	private int fetchPage                           = 0;
	private int count                               = 0;
	private QueryContext queryContext               = null;

	public AdvancedCypherQuery(final QueryContext queryContext, final AbstractCypherIndex<?> index, final int requestedPageSize, final int requestedPage) {

		this.queryContext      = queryContext;
		this.index             = index;

		if (queryContext.isSuperuser() && requestedPageSize < Integer.MAX_VALUE) {

			final int firstRequestedIndex = (requestedPage - 1) * requestedPageSize;
			final int firstFetchIndex     = (firstRequestedIndex / fetchSize);

			fetchPage = Math.max(0, firstFetchIndex);

			// notify query context that we skipped a number of nodes
			queryContext.setSkipped(firstFetchIndex * fetchSize);
		}
	}

	@Override
	public String toString() {
		return getStatement(false);
	}

	@Override
	public void nextPage() {
		fetchPage++;
	}

	@Override
	public int pageSize() {
		return this.fetchSize;
	}

	public SortOrder getSortOrder() {
		return sortOrder;
	}

	@Override
	public String getStatement(final boolean paged) {

		final boolean hasPredicates = buffer.length() > 0;
		final StringBuilder buf     = new StringBuilder();
		final int typeCount         = typeLabels.size();

		switch (typeCount) {

			case 0:

				buf.append(index.getQueryPrefix(getTypeQueryLabel(null), sourceTypeLabel, targetTypeLabel, hasPredicates));

				if (hasPredicates) {
					buf.append(" WHERE ");
					buf.append(buffer);
				}

				buf.append(index.getQuerySuffix(this));
				break;

			case 1:

				buf.append(index.getQueryPrefix(getTypeQueryLabel(Iterables.first(typeLabels)), sourceTypeLabel, targetTypeLabel, hasPredicates));

				if (hasPredicates) {
					buf.append(" WHERE ");
					buf.append(buffer);
				}

				buf.append(index.getQuerySuffix(this));
				break;

			default:

				// create UNION query
				for (final Iterator<String> it = typeLabels.iterator(); it.hasNext();) {

					buf.append(index.getQueryPrefix(getTypeQueryLabel(it.next()), sourceTypeLabel, targetTypeLabel, hasPredicates));

					if (hasPredicates) {
						buf.append(" WHERE ");
						buf.append(buffer);
					}

					buf.append(index.getQuerySuffix(this));

					if (it.hasNext()) {
						buf.append(" UNION ");
					}
				}
				break;
		}

		if (sortOrder != null) {

			boolean first     = true;
			int sortSpecIndex = 0;


			for (final SortSpec spec : sortOrder.getSortElements()) {

				if (first) {

					buf.append(" ORDER BY");

				} else {

					buf.append(", ");
				}

				buf.append(" sortKey");
				buf.append(sortSpecIndex);

				if (spec.sortDescending()) {
					buf.append(" DESC");
				}

				sortSpecIndex++;
				first = false;
			}
		}

		if (paged) {

			buf.append(" SKIP ");
			buf.append(fetchPage * fetchSize);
			buf.append(" LIMIT ");
			buf.append(fetchSize);
		}

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

	@Override
	public void and() {
		buffer.append(" AND ");
	}

	@Override
	public void not() {
		buffer.append(" NOT ");
	}

	@Override
	public void andNot() {
		buffer.append(" AND NOT ");
	}

	@Override
	public void or() {
		buffer.append(" OR ");
	}

	public void indexLabel(final String indexLabel) {
		this.indexLabels.add(indexLabel);
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
			buffer.append(" $");
			buffer.append(paramKey);

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

		final String listFunction = index.anyOrSingleFunction();

		if (value != null) {

			final String paramKey = "param" + count++;

			buffer.append(listFunction);
			buffer.append("(x IN n.`");
			buffer.append(key);
			buffer.append("` WHERE x ");
			buffer.append(operator);
			buffer.append(" $");
			buffer.append(paramKey);
			buffer.append(")");

			parameters.put(paramKey, value);

		} else {

			buffer.append(listFunction);
			buffer.append("(x IN n.`");
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
		buffer.append(" $");
		buffer.append(paramKey1);
		buffer.append(" AND ");
		buffer.append("n.`");
		buffer.append(key);
		buffer.append("` ");
		buffer.append(operator2);
		buffer.append(" $");
		buffer.append(paramKey2);
		buffer.append(")");

		parameters.put(paramKey1, value1);
		parameters.put(paramKey2, value2);
	}

	@Override
	public void sort(final SortOrder sortOrder) {
		this.sortOrder = sortOrder;
	}

	public void setSourceType(final String sourceTypeLabel) {
		this.sourceTypeLabel = sourceTypeLabel;
	}

	public String getSourceType() {
		return sourceTypeLabel;
	}

	public void setTargetType(final String targetTypeLabel) {
		this.targetTypeLabel = targetTypeLabel;
	}

	public String getTargetType() {
		return targetTypeLabel;
	}

	@Override
	public QueryContext getQueryContext() {
		return queryContext;
	}

	// ----- private methods -----
	private String getTypeQueryLabel(final String mainType) {

		if (mainType != null) {

			final StringBuilder buf = new StringBuilder(mainType);

			for (final String indexLabel : indexLabels) {

				buf.append(":");
				buf.append(indexLabel);
			}

			return buf.toString();
		}

		// null indicates "no main type"
		return null;
	}
}
