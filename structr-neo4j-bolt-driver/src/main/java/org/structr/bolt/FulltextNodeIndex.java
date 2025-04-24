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

import org.neo4j.driver.exceptions.ClientException;
import org.neo4j.driver.exceptions.DatabaseException;
import org.structr.api.graph.Direction;
import org.structr.api.graph.Node;
import org.structr.api.graph.RelationshipType;
import org.structr.api.search.QueryContext;
import org.structr.api.search.SortOrder;
import org.structr.api.search.SortSpec;
import org.structr.api.util.Iterables;
import org.structr.api.util.QueryTimer;

import java.util.Collections;
import java.util.Map;

/**
 *
 */
class FulltextNodeIndex extends AbstractCypherIndex<Node> {

	public FulltextNodeIndex(final BoltDatabaseService db) {
		super(db);
	}

	@Override
	public AdvancedCypherQuery createQuery(final QueryContext context, final int requestedPageSize, final int requestedPage) {
		return new FulltextNodeIndexQuery(context, this, requestedPageSize, requestedPage);
	}

	@Override
	public String getQueryPrefix(final String typeLabel, final AdvancedCypherQuery query) {
		return "";
	}

	@Override
	public String getQuerySuffix(final AdvancedCypherQuery query) {
		return "";
	}

	@Override
	public Iterable<Node> getResult(final CypherQuery query) {
		return Iterables.map(new NodeNodeMapper(db), Iterables.map(new RecordNodeMapper(), new LazyRecordIterable(db, query)));
	}
}



























