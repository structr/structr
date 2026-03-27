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
import org.structr.core.datasources.ChannelResult;
import org.structr.core.entity.DataSource;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.Traits;
import org.structr.core.traits.operations.datasource.DataSourceOperations;
import org.structr.web.common.RenderContext;
import org.structr.web.datasource.FieldDefinition;

import java.util.LinkedHashMap;
import java.util.Map;

public class DataSourceTraitWrapper extends AbstractNodeTraitWrapper implements DataSource<GraphObject> {

	private Map<ChannelInput, ChannelResult<GraphObject>> cachedResults = new LinkedHashMap<>();

	public DataSourceTraitWrapper(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	@Override
	public final ChannelResult<GraphObject> getResult(final RenderContext renderContext, final ChannelInput input) throws FrameworkException {

		ChannelResult<GraphObject> result = cachedResults.get(input);
		if (result == null) {

			result = ChannelResult.fromStream(traits.getMethod(DataSourceOperations.class).getValues(renderContext, this, input));

			cachedResults.put(input, result);
		}

		return result;
	}

	@Override
	public final Map<String, FieldDefinition> getFields(final RenderContext renderContext) throws FrameworkException {
		return traits.getMethod(DataSourceOperations.class).getFields(renderContext, this);
	}

	@Override
	public String getDataType(final RenderContext renderContext) throws FrameworkException {
		return traits.getMethod(DataSourceOperations.class).getDataType(renderContext, this);
	}
}
