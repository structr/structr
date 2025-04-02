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
package org.structr.bolt;

import org.apache.commons.lang3.StringUtils;
import org.structr.api.DatabaseService;
import org.structr.api.config.Settings;
import org.structr.api.graph.Direction;
import org.structr.api.graph.RelationshipType;
import org.structr.api.search.Occurrence;
import org.structr.api.search.QueryContext;
import org.structr.api.search.SortOrder;
import org.structr.api.search.SortSpec;
import org.structr.api.util.Iterables;
import org.structr.api.util.QueryHistogram;
import org.structr.api.util.QueryTimer;

import java.util.*;

/**
 *
 */
public class AdvancedCypherQuery implements CypherQuery {

	private final Map<String, Object> parameters    = new TreeMap<>();
	private final Set<String> indexLabels           = new LinkedHashSet<>();
	private final Set<String> typeLabels            = new LinkedHashSet<>();
	private final Map<String, GraphQueryPart> parts = new LinkedHashMap<>();
	private final StringBuilder buffer              = new StringBuilder();
	private AbstractCypherIndex<?> index            = null;
	private QueryContext queryContext               = null;
	private QueryTimer queryTimer                   = null;
	private int fetchSize                           = Settings.FetchSize.getValue();
	private boolean hasOptionalParts                = false;
	private String currentGraphPartIdentifier       = "n";
	private String sourceTypeLabel                  = null;
	private String targetTypeLabel                  = null;
	private String relationshipType                 = null;
	private String type                             = null;
	private boolean outgoing                        = false;
	private SortOrder sortOrder                     = null;
	private int fetchPage                           = 0;
	private int count                               = 0;

	public AdvancedCypherQuery(final QueryContext queryContext, final AbstractCypherIndex<?> index, final int requestedPageSize, final int requestedPage) {

		this.queryContext      = queryContext;
		this.index             = index;

		if (queryContext.overridesFetchSize()) {

			final int overriddenFetchSize = queryContext.getOverriddenFetchSize();
			if (overriddenFetchSize > 0) {

				this.fetchSize = overriddenFetchSize;
			}
		}

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
		return getStatement();
	}

	@Override
	public boolean equals(final Object other) {
		return hashCode() == other.hashCode();
	}

	@Override
	public int hashCode() {

		int hashCode = 31 + getStatement().hashCode();

		for (final Map.Entry<String, Object> p : getParameters().entrySet()) {

			final Object value = p.getValue();
			if (value != null) {

				if (value.getClass().isArray()) {

					hashCode = 31 * hashCode + Arrays.deepHashCode((Object[]) value);

				} else {

					hashCode = 31 * hashCode + value.hashCode();
				}
			}
		}

		return hashCode;
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

	public boolean hasPredicates() {
		return buffer.length() > 0;
	}

	public boolean getHasOptionalParts() {
		return hasOptionalParts;
	}

	@Override
	public String getStatement() {

		final boolean hasPredicates = hasPredicates();
		final StringBuilder buf     = new StringBuilder();
		final int typeCount         = typeLabels.size();

		switch (typeCount) {

			case 0:

				buf.append(index.getQueryPrefix(getTypeQueryLabel(null), this));
				buf.append(getGraphPartForMatch());

				if (hasPredicates) {
					buf.append(" WHERE ");
					buf.append(buffer);
				}

				buf.append(index.getQuerySuffix(this));
				break;

			case 1:

				buf.append(index.getQueryPrefix(getTypeQueryLabel(Iterables.first(typeLabels)), this));
				buf.append(getGraphPartForMatch());

				if (hasPredicates) {
					buf.append(" WHERE ");
					buf.append(buffer);
				}

				buf.append(index.getQuerySuffix(this));
				break;

			default:

				// create UNION query
				for (final Iterator<String> it = typeLabels.iterator(); it.hasNext();) {

					buf.append(index.getQueryPrefix(getTypeQueryLabel(it.next()), this));

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

		buf.append(" SKIP ");
		buf.append(fetchPage * fetchSize);

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

		if ('(' == buffer.charAt(buffer.length() - 1)) {

			buffer.deleteCharAt(buffer.length() - 1);

		} else {

			buffer.append(")");
		}
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
		addSimpleParameter("n", key, operator, value, isProperty, caseInsensitive);
	}

	public void addSimpleParameter(final String identifier, final String key, final String operator, final Object value, final boolean isProperty, final boolean caseInsensitive) {

		if (value != null) {

			final String paramKey = "param" + count++;

			if (isProperty) {

				if (caseInsensitive) {
					buffer.append("toLower(");
				}

				buffer.append(identifier);
				buffer.append(".`");
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
				buffer.append(identifier);
				buffer.append(".`");
			}

			buffer.append(key);

			if (isProperty) {
				buffer.append("` ");
			}

			buffer.append(operator);
			buffer.append(" null");
		}
	}

	public void addNullObjectParameter(final Direction direction, final String relationship) {

		buffer.append("not (n)");

		switch (direction) {

			case INCOMING:
				buffer.append("<");
				break;
		}

		buffer.append("-[:");
		buffer.append(relationship);
		buffer.append("]-");

		switch (direction) {

			case OUTGOING:
				buffer.append(">");
				break;
		}

		buffer.append("()");
	}

	public void addPatternParameter(final Direction direction, final String relationship, final String identifier) {

		buffer.append("(n)");

		switch (direction) {

			case INCOMING:
				buffer.append("<");
				break;
		}

		buffer.append("-[:");
		buffer.append(relationship);
		buffer.append("]-");

		switch (direction) {

			case OUTGOING:
				buffer.append(">");
				break;
		}

		buffer.append("(");
		buffer.append(identifier);
		buffer.append(")");
	}

	public void addListParameter(final String key, final String operator, final Object value) {

		if (value != null) {

			final String paramKey = "param" + count++;

			buffer.append("ANY(x IN n.`");
			buffer.append(key);
			buffer.append("` WHERE x ");
			buffer.append(operator);
			buffer.append(" $");
			buffer.append(paramKey);
			buffer.append(")");

			parameters.put(paramKey, value);

		} else {

			buffer.append("ANY(x IN n.`");
			buffer.append(key);
			buffer.append("` WHERE x ");
			buffer.append(operator);
			buffer.append(" null)");
		}
	}

	public void addExactListParameter(final String key, final String operator, final Object value) {

		if (value != null) {

			final String paramKey = "param" + count++;

			buffer.append("ALL(x IN n.`");
			buffer.append(key);
			buffer.append("` WHERE x ");
			buffer.append(operator);
			buffer.append(" $");
			buffer.append(paramKey);
			buffer.append(")");

			parameters.put(paramKey, value);

		} else {

			buffer.append("ALL(x IN n.`");
			buffer.append(key);
			buffer.append("` WHERE x ");
			buffer.append(operator);
			buffer.append(" null)");
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

	public void addGraphQueryPart(final GraphQueryPart newPart) {

		final Occurrence occurrence = newPart.getOccurrence();
		if (Occurrence.OPTIONAL.equals(occurrence)) {

			final String linkIdentifier       = newPart.getLinkIdentifier();
			final GraphQueryPart existingPart = parts.get(linkIdentifier);

			if (existingPart != null) {

				// re-use identifier in query, do not add new part
				newPart.setIdentifier(existingPart.getIdentifier());

			} else {

				final String identifier = getNextGraphPartIdentifier();

				newPart.setIdentifier(identifier);

				this.parts.put(linkIdentifier, newPart);
			}

		} else {

			final String identifier = getNextGraphPartIdentifier();

			newPart.setIdentifier(identifier);

			this.parts.put(identifier, newPart);
		}
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

	public void hasOptionalParts() {
		hasOptionalParts = true;
	}

	@Override
	public QueryContext getQueryContext() {
		return queryContext;
	}

	public QueryTimer getQueryTimer() {

		if (queryTimer == null) {
			queryTimer = QueryHistogram.newTimer();
		}

		return queryTimer;
	}

	public void storeRelationshipInfo(final String type, final RelationshipType relationshipType, final Direction direction) {

		this.type             = type;
		this.relationshipType = relationshipType.name();
		this.outgoing         = Direction.OUTGOING.equals(direction);
	}

	public String getType() {
		return type;
	}

	public String getRelationshipType() {
		return relationshipType;
	}

	public boolean isOutgoing() {
		return outgoing;
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

	private String getGraphPartForMatch() {

		final DatabaseService db = index.getDatabaseService();
		final String tenantId    = db.getTenantIdentifier();
		final StringBuilder buf  = new StringBuilder();
		final Set<String> with   = new LinkedHashSet<>();
		boolean first            = true;

		for (final GraphQueryPart part : parts.values()) {

			if (first) {

				buf.append(part.getRelationshipPattern());

				buf.append("(");
				buf.append(part.getIdentifier());
				buf.append(":NodeInterface");

				if (tenantId != null) {

					buf.append(":");
					buf.append(tenantId);
				}

				buf.append(":");
				buf.append(part.getOtherLabel());
				buf.append(")");

			} else {

				buf.append(" WITH n, ");
				buf.append(StringUtils.join(with, ", "));
				buf.append(" MATCH (n)");
				buf.append(part.getRelationshipPattern());

				buf.append("(");
				buf.append(part.getIdentifier());
				buf.append(":NodeInterface");

				if (tenantId != null) {

					buf.append(":");
					buf.append(tenantId);
				}

				buf.append(":");
				buf.append(part.getOtherLabel());
				buf.append(")");

			}

			first = false;

			with.add(part.getIdentifier());
		}

		return buf.toString();
	}

	private String getNextGraphPartIdentifier() {

		currentGraphPartIdentifier = Character.toString(currentGraphPartIdentifier.charAt(0) + 1);

		return currentGraphPartIdentifier;
	}
}