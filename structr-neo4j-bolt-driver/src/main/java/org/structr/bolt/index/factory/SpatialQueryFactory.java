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

import org.structr.api.search.QueryPredicate;
import org.structr.api.search.SpatialQuery;
import org.structr.bolt.index.CypherQuery;

/**
 *
 */
public class SpatialQueryFactory extends AbstractQueryFactory {

	@Override
	public boolean createQuery(final QueryFactory parent, final QueryPredicate predicate, final CypherQuery query, final boolean isFirst) {

		if (predicate instanceof SpatialQuery) {

			checkOccur(query, predicate.getOccurrence(), isFirst);

			final SpatialQuery spatial = (SpatialQuery)predicate;
			final StringBuilder buf    = new StringBuilder();
			final Double[] coords      = spatial.getCoords();

			buf.append("distance(point({latitude:");
			buf.append(coords[0]);
			buf.append(",longitude:");
			buf.append(coords[1]);
			buf.append("}), point(n))");

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

		return false;
	}

}
