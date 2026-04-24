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
package org.structr.core.datasources;

import org.structr.common.ChannelInput;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.DataSource;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.schema.action.ActionContext;
import org.structr.web.datasource.FieldDefinition;
import org.structr.web.entity.ComponentConfiguration;

import java.util.Map;

/**
 * We need an interface that does not extend NodeInterface to be able
 * to provide both graph-based and non-graph implementations of the
 * same interface for CurrentDataSource and SchemaNode etc.
 *
 * Note: all data sources that implement this interface must resolve
 * the strings "values", "dataType" and "currentValue" to the
 * correct values in their evaluate() method.
 */
public interface Channel<T> {

	ChannelResult<T> getResult(final ActionContext actionContext, final ChannelInput input) throws FrameworkException;
	Map<String, FieldDefinition> getFields(final ActionContext actionContext) throws FrameworkException;

	String getDataType(final ActionContext actionContext) throws FrameworkException;
	String getChannelName();

	Object evaluate(final ActionContext actionContext, final String key, final String defaultValue, final GraphObject contextObject, final int row, final int column) throws FrameworkException;

	default String getSortKey() {
		return getChannelName().toLowerCase() + ".sort";
	}

	default String getPaginationKey() {
		return getChannelName().toLowerCase() + ".page";
	}

	default String getFilterKey() {
		return getChannelName().toLowerCase() + ".filter";
	}

	static <T> Channel<T> forName(final SecurityContext securityContext, final ComponentConfiguration config, final String dataSourceName) throws FrameworkException {

		if (dataSourceName != null) {

			if (dataSourceName.contains(":")) {

				final String[] parts = dataSourceName.split(":");
				final String type    = parts[0];
				final String name    = parts[1];

				switch (type) {

					case "node":
						final NodeInterface node = StructrApp.getInstance(securityContext).nodeQuery(StructrTraits.DATA_SOURCE).name(name).getFirst();
						if (node != null) {

							return node.as(DataSource.class);

						} else {

							final NodeInterface byUuid = StructrApp.getInstance(securityContext).getNodeById(name);
							if (byUuid != null) {

								return byUuid.as(DataSource.class);
							}

							// check if name is an existing trait & override if we are super user
							if (Traits.exists(name) && securityContext.isSuperUser()) {

								final NodeInterface newNode = StructrApp.getInstance().create(StructrTraits.SCHEMA_NODE, name);

								return newNode.as(DataSource.class);
							}

						}
						break;

					case "channel":
						return new ChannelDataSource(config, name);

				}

			} else {

				switch (dataSourceName) {

					case "root-folders":
						return new RootFoldersDataSource(config, "root-folders");

					default:
						throw new IllegalStateException("Unknown data source type: " + dataSourceName);
				}
			}
		}

		return null;
	}
}
