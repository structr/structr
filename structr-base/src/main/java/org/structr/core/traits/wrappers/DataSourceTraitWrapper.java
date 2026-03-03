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
package org.structr.core.traits.wrappers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.entity.DataProvider;
import org.structr.core.entity.DataSource;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.DataSourceTraitDefinition;

import java.util.List;
import java.util.Map;

public class DataSourceTraitWrapper extends AbstractNodeTraitWrapper implements DataSource {

	private final Gson gson = new GsonBuilder().create();
	private List<Map<String, Object>> fields = null;

	public DataSourceTraitWrapper(final Traits traits, final NodeInterface node) {
		super(traits, node);
	}

	@Override
	public final Iterable<GraphObject> getValues(SecurityContext securityContext) throws FrameworkException {

		final DataProvider dataProvider = getDataProvider();

		return dataProvider.getValues(securityContext);
	}

	@Override
	public List<Map<String, Object>> getFields(final SecurityContext securityContext) throws FrameworkException {

		if (fields == null) {

			final String mappingSource = wrappedObject.getProperty(traits.key(DataSourceTraitDefinition.MAPPING_PROPERTY));
			if (mappingSource != null) {

				fields = gson.fromJson(mappingSource, List.class);
			}
		}

		return fields;
	}

	@Override
	public String getDataKey() {
		return wrappedObject.getProperty(traits.key(DataSourceTraitDefinition.DATA_KEY_PROPERTY));
	}

	@Override
	public DataProvider getDataProvider() {

		final NodeInterface provider = wrappedObject.getProperty(traits.key(DataSourceTraitDefinition.PROVIDER_PROPERTY));
		if (provider != null) {

			return provider.as(DataProvider.class);
		}

		return null;
	}
}
