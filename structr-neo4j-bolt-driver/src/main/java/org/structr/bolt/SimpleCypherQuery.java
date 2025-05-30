/*
 * Copyright (C) 2010-2025 Structr GmbH
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

import org.structr.api.graph.Direction;
import org.structr.api.graph.RelationshipType;
import org.structr.api.search.QueryContext;
import org.structr.api.search.SortOrder;
import org.structr.api.util.QueryHistogram;
import org.structr.api.util.QueryTimer;

import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 */
class SimpleCypherQuery implements CypherQuery {

	private final QueryContext queryContext = new QueryContext();
	private Map<String, Object> params      = null;
	private QueryTimer queryTimer           = null;
	private String type                     = null;
	private String statement                = null;
	private String relationshipType         = null;
	private boolean outgoing                = false;

	public SimpleCypherQuery(final StringBuilder buf) {
		this(buf.toString(), new TreeMap<>());
	}

	public SimpleCypherQuery(final StringBuilder buf, final Map<String, Object> parameters) {
		this(buf.toString(), parameters);
	}

	public SimpleCypherQuery(final String statement) {
		this(statement, new TreeMap<>());
	}

	public SimpleCypherQuery(final String statement, final Map<String, Object> parameters) {

		this.statement = statement;
		this.params    = parameters;
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
	}

	@Override
	public int pageSize() {
		return Integer.MAX_VALUE;
	}

	@Override
	public String getStatement() {
		return statement;
	}

	@Override
	public Map<String, Object> getParameters() {
		return params;
	}

	@Override
	public void and() {
	}

	@Override
	public void or() {
	}

	@Override
	public void not() {
	}

	@Override
	public void andNot() {
	}

	@Override
	public void sort(final SortOrder sortOrder) {
	}

	@Override
	public QueryContext getQueryContext() {
		return queryContext;
	}

	@Override
	public QueryTimer getQueryTimer() {

		if (queryTimer == null) {
			queryTimer = QueryHistogram.newTimer();
		}

		return queryTimer;
	}

	public void storeRelationshipInfo(final String type, final RelationshipType relationshipType, final Direction direction) {

		if (relationshipType != null) {

			this.type             = type;
			this.relationshipType = relationshipType.name();
			this.outgoing         = Direction.OUTGOING.equals(direction);
		}
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
}
