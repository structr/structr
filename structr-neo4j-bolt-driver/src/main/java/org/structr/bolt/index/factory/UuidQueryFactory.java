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

import org.apache.commons.lang.StringUtils;
import org.structr.api.search.QueryPredicate;
import org.structr.api.search.UuidQuery;
import org.structr.bolt.index.CypherQuery;

/**
 */
public class UuidQueryFactory extends AbstractQueryFactory {

	@Override
	public boolean createQuery(final QueryFactory parent, final QueryPredicate predicate, final CypherQuery query, final boolean isFirst) {

		checkOccur(query, predicate.getOccurrence(), isFirst);

		final String uuid = ((UuidQuery)predicate).getUuid();
		if (StringUtils.isNotBlank(uuid) && uuid.length() == 32) {

			query.addSimpleParameter(predicate.getName(), "=", uuid);

		} else {

			query.addSimpleParameter(predicate.getName(), "=", "__invalid__uuid__string__");
		}

		return true;
	}
}
