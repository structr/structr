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
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.neo4j.index.lucene;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.neo4j.gis.spatial.indexprovider.LayerNodeIndex;
import org.structr.api.graph.PropertyContainer;
import org.structr.api.QueryResult;
import org.structr.api.search.GroupQuery;
import org.structr.api.search.QueryPredicate;
import org.structr.api.search.SpatialQuery;
import org.structr.neo4j.Neo4jDatabaseService;
import org.structr.neo4j.index.AbstractIndexWrapper;
import org.structr.neo4j.index.IndexHitsWrapper;

/**
 *
 */
public class SpatialIndexWrapper <S extends org.neo4j.graphdb.PropertyContainer, T extends PropertyContainer> extends AbstractIndexWrapper<S, T> {

	public SpatialIndexWrapper(final Neo4jDatabaseService graphDb, final org.neo4j.graphdb.index.Index<S> index) {
		super(graphDb, index);
	}

	@Override
	protected Object convertForIndexing(final Object source, final Class typeHint) {
		return source;
	}

	@Override
	protected Object convertForQuerying(final Object source, final Class typeHint) {
		return source;
	}

	@Override
	public QueryResult<T> query(final QueryPredicate predicate) {

		final SpatialQuery spatialPredicate = findSpatialPredicate(predicate);
		if (spatialPredicate != null) {

			final Map<String, Object> params = new HashMap<>();
			final Double[] coords            = spatialPredicate.getCoords();
			final Double dist                = spatialPredicate.getDistance();

			params.put(LayerNodeIndex.POINT_PARAMETER, coords);
			params.put(LayerNodeIndex.DISTANCE_IN_KM_PARAMETER, dist);

			return new IndexHitsWrapper<>(graphDb, index.query(LayerNodeIndex.WITHIN_DISTANCE_QUERY, params));
		}

		return null;
	}

	// ----- private methods -----
	private SpatialQuery findSpatialPredicate(final QueryPredicate predicate) {

		if (predicate != null) {

			if (predicate instanceof SpatialQuery) {
				return (SpatialQuery)predicate;
			}

			if (predicate instanceof GroupQuery) {

				final GroupQuery group              = (GroupQuery)predicate;
				final List<QueryPredicate> children = group.getQueryPredicates();

				if (children != null) {

					for (final QueryPredicate child : children) {

						final QueryPredicate candidate = findSpatialPredicate(child);
						if (candidate != null) {

							return (SpatialQuery)candidate;
						}
					}
				}


			}
		}

		// not found
		return null;
	}
}
