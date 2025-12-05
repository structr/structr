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
package org.structr.bolt.factory;

import org.structr.api.config.Settings;
import org.structr.api.index.AbstractIndex;
import org.structr.api.index.AbstractQueryFactory;
import org.structr.api.search.QueryPredicate;
import org.structr.api.search.UuidQuery;
import org.structr.bolt.AdvancedCypherQuery;

/**
 */
public class UuidQueryFactory extends AbstractQueryFactory<AdvancedCypherQuery> {

	public UuidQueryFactory(final AbstractIndex index) {
		super(index);
	}

	@Override
	public boolean createQuery(final QueryPredicate predicate, final AdvancedCypherQuery query, final boolean isFirst) {

		//checkOperation(query, predicate.getOperation(), isFirst);

		// add label of declaring class for the given property name
		// to select the correct index
		final String label = predicate.getLabel();
		if (label != null) {

			query.indexLabel(label);
		}

		final String uuid = ((UuidQuery)predicate).getUuid();
		if (Settings.isValidUuid(uuid)) {

			query.addSimpleParameter(predicate.getName(), "=", uuid);

		} else {

			query.addSimpleParameter(predicate.getName(), "=", "__invalid__uuid__string__");
		}

		return true;
	}
}
