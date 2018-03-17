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
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;

/**
 *
 */
public class RelationshipDataFetcher implements DataFetcher<List> {

	private String type = null;

	public RelationshipDataFetcher(final String type) {
		this.type = type;
	}

	@Override
	public List get(final DataFetchingEnvironment environment) {

		try {

			return StructrApp.getInstance().nodeQuery().andType(StructrApp.getConfiguration().getNodeEntityClass(type)).getAsList();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		return null;
	}
}
