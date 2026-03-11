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

import org.structr.common.ChannelInput;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.entity.DataSource;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.Traits;
import org.structr.core.traits.operations.datasource.DataSourceOperations;
import org.structr.web.common.RenderContext;
import org.structr.web.datasource.FieldDefinition;

import java.util.Map;

public class DataSourceTraitWrapper extends AbstractNodeTraitWrapper implements DataSource {

	public DataSourceTraitWrapper(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	@Override
	public final Iterable<GraphObject> getValues(final RenderContext renderContext, final ChannelInput input) throws FrameworkException {
		return traits.getMethod(DataSourceOperations.class).getValues(renderContext, this, input);
	}

	@Override
	public final Map<String, FieldDefinition> getFields(final RenderContext renderContext) throws FrameworkException {
		return traits.getMethod(DataSourceOperations.class).getFields(renderContext, this);
	}

	@Override
	public String getDataType(final RenderContext renderContext) throws FrameworkException {
		return traits.getMethod(DataSourceOperations.class).getDataType(renderContext, this);
	}

	/**
	 * Returns the context value that is currently associated with
	 * the data key of this data source.
	 *
	 * @param renderContext
	 * @return
	 * @throws FrameworkException
	 */
	@Override
	public Object getCurrentValue(final RenderContext renderContext) throws FrameworkException {

		/*
		final DataSource dataSource = getDataSource();
		if (dataSource != null) {

			// if this channel has a data source, we return the value that is currently
			// stored under the data key
			if (actionContext instanceof RenderContext renderContext) {

				final String dataKey = getDataKey();
				if (dataKey != null) {

					// allow fallback if no data node is set
					final GraphObject dataNode = renderContext.getDataNode(dataKey);
					if (dataNode != null) {

						return dataNode;
					}
				}
			}
		}

		// if this channel has no data source, we return the single value
		// we get from getValues()


		FIXME: we're in the middle of merging channel and data source!
		*/


		return null;
	}
}
