/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.memory.index.factory;

import org.structr.api.index.AbstractIndex;
import org.structr.api.index.AbstractQueryFactory;
import org.structr.api.search.QueryPredicate;
import org.structr.memory.index.MemoryQuery;

/**
 *
 */
public class SpatialQueryFactory extends AbstractQueryFactory<MemoryQuery> {

	public SpatialQueryFactory(final AbstractIndex index) {
		super(index);
	}

	@Override
	public boolean createQuery(final QueryPredicate predicate, final MemoryQuery query, final boolean isFirst) {

		/*

		if (predicate instanceof SpatialQuery) {

			checkOccur(query, predicate.getOccurrence(), isFirst);

			final SpatialQuery spatial = (SpatialQuery)predicate;
			final StringBuilder buf    = new StringBuilder();
			final Double[] coords      = spatial.getCoords();

			if (coords == null || coords.length != 2)  {
				return false;
			}

			buf.append("distance(point({latitude:");
			buf.append(coords[0]);
			buf.append(",longitude:");
			buf.append(coords[1]);
			buf.append("}), point({latitude: n.latitude, longitude: n.longitude}))");

			// do not include nodes that have no lat/lon properties
			query.beginGroup();
			query.addSimpleParameter("latitude", "IS NOT", null);
			query.and();
			query.addSimpleParameter("longitude", "IS NOT", null);
			query.and();
			query.addSimpleParameter(buf.toString(), "<", spatial.getDistance() * 1000.0, false); // distance is in kilometers
			query.endGroup();

			return true;
		}
		*/

		return false;
	}

}
