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
import org.structr.core.traits.definitions.DataSourceTraitDefinition;
import org.structr.core.traits.operations.datasource.DataSourceOperations;
import org.structr.schema.action.ActionContext;
import org.structr.web.datasource.FieldDefinition;

import java.util.LinkedHashMap;
import java.util.Map;

public class DataSourceTraitWrapper extends AbstractNodeTraitWrapper implements DataSource<GraphObject> {

	private Map<ChannelInput, ChannelResult<GraphObject>> cachedResults = new LinkedHashMap<>();

	public DataSourceTraitWrapper(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	@Override
	public final ChannelResult<GraphObject> getResult(final ActionContext actionContext, final ChannelInput input, final String transform) throws FrameworkException {

		ChannelResult<GraphObject> result = cachedResults.get(input);
		if (result == null) {

			result = ChannelResult.fromStream(traits.getMethod(DataSourceOperations.class).getValues(actionContext, this, input));

			cachedResults.put(input, result);
		}

		return result;
	}

	@Override
	public final Map<String, FieldDefinition> getFields(final ActionContext actionContext) throws FrameworkException {
		return traits.getMethod(DataSourceOperations.class).getFields(actionContext, this);
	}

	@Override
	public String getDataType(final ActionContext actionContext) throws FrameworkException {
		return traits.getMethod(DataSourceOperations.class).getDataType(actionContext, this);
	}

	@Override
	public int getDimension() {
		return traits.getMethod(DataSourceOperations.class).getDimension(this);
	}

	@Override
	public boolean includeHidden() {
		return wrappedObject.getProperty(traits.key(DataSourceTraitDefinition.INCLUDE_HIDDEN_PROPERTY));
	}
}
