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
import java.util.Collections;
import java.util.List;
import org.structr.common.GraphObjectComparator;
import org.structr.common.PagingHelper;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.property.PropertyKey;
import org.structr.schema.ConfigurationProvider;

/**
 *
 */
public class PropertyKeyDataFetcher<T> extends AbstractDataFetcher implements DataFetcher<T> {

	private String typeName = null;
	private String keyName  = null;

	public PropertyKeyDataFetcher(final String typeName, final String keyName) {
		this.typeName = typeName;
		this.keyName  = keyName;
	}

	@Override
	public T get(final DataFetchingEnvironment environment) {

		assertObjectNonNullAndOfType(environment.getSource(), GraphObject.class);

		final ConfigurationProvider config = StructrApp.getConfiguration();
		final Class type                   = config.getNodeEntityClass(typeName);
		final PropertyKey<T> key           = config.getPropertyKeyForJSONName(type, keyName);

		initialize(type, environment.getArguments());

		// fetch result
		final T result = ((GraphObject)environment.getSource()).getProperty(key);

		// check if pagination and sorting can be applied
		if (key.isCollection() && result instanceof List) {

			final List<T> list      = (List<T>)result;
			final Class relatedType = key.relatedType();

			if (sortKey != null && relatedType != null && GraphObject.class.isAssignableFrom(relatedType)) {

				// sort by property key
				Collections.sort((List<GraphObject>)list, new GraphObjectComparator(sortKey, sortDescending));
			}

			// apply pagination
			return (T)PagingHelper.subList(list, pageSize, page);
		}

		return result;
	}
}
