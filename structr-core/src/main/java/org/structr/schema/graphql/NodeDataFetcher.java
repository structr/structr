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
package org.structr.schema.graphql;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.List;
import java.util.Map;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.Query;
import org.structr.core.app.StructrApp;
import org.structr.core.property.PropertyMap;

/**
 *
 */
public class NodeDataFetcher extends AbstractDataFetcher implements DataFetcher<List> {

	private String typeName = null;

	public NodeDataFetcher(final String typeName) {
		this.typeName = typeName;
	}

	@Override
	public List get(final DataFetchingEnvironment environment) {

		try {

			final SecurityContext securityContext = get(environment.getContext(), SecurityContext.getSuperUserInstance());
			final Class type                      = StructrApp.getConfiguration().getNodeEntityClass(typeName);
			final Map<String, Object> args        = environment.getArguments();

			// initialize this DataFetcher from arguments
			initialize(type, args);

			// return query
			final Query query = StructrApp.getInstance(securityContext)
				.nodeQuery(type)
				.and(PropertyMap.inputTypeToJavaType(securityContext, type, args))
				.pageSize(pageSize)
				.page(page);

			// sort result?
			if (sortKey != null) {

				if (sortDescending) {

					query.sortDescending(sortKey);

				} else {

					query.sortAscending(sortKey);
				}
			}

			return query.getAsList();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		return null;
	}
}
