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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.ChannelInput;
import org.structr.common.PropertyView;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.function.Functions;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.schema.action.ActionContext;
import org.structr.web.common.RenderContext;
import org.structr.web.datasource.FieldDefinition;
import org.structr.web.entity.ComponentConfiguration;
import org.structr.web.traits.definitions.AbstractFileTraitDefinition;

import java.util.LinkedHashMap;
import java.util.Map;

public class RootFoldersDataSource<T extends GraphObject> implements Channel<T> {

	private static final Logger logger = LoggerFactory.getLogger(RootFoldersDataSource.class);

	private final String name;

	public RootFoldersDataSource(final String name) {

		this.name= name;
	}

	@Override
	public String getChannelName() {

		return Functions.cleanString(name);
	}

	@Override
	public int getDimension() {

		return 1;
	}

	@Override
	public final ChannelResult<T> getResult(final ActionContext actionContext, final ChannelInput channelInput, final String transform) throws FrameworkException {

		final Traits traits                                  = Traits.of(StructrTraits.FOLDER);
		final PropertyKey<Iterable<NodeInterface>> parentKey = traits.key(AbstractFileTraitDefinition.PARENT_PROPERTY);

		return (ChannelResult) ChannelResult.fromStream(StructrApp.getInstance(actionContext.getSecurityContext()).nodeQuery(StructrTraits.FOLDER).key(parentKey, null).getResultStream());
	}

	@Override
	public final Map<String, FieldDefinition> getFields(final ActionContext actionContext) throws FrameworkException {

		final Map<String, FieldDefinition> output = new LinkedHashMap<>();
		final Traits traits = Traits.of(StructrTraits.FOLDER);

		// transform input
		for (final PropertyKey key : traits.getPropertyKeysForView(PropertyView.All)) {

			output.put(key.jsonName(), key.getFieldDefinition());
		}

		return output;
	}

	@Override
	public String getDataType(final ActionContext actionContext) throws FrameworkException {

		return StructrTraits.FOLDER;
	}

	public Object evaluate(final ActionContext actionContext, final String key, final String defaultValue, final GraphObject contextObject, final int row, final int column) throws FrameworkException {

		final RenderContext renderContext = (RenderContext) actionContext;

		switch (key) {

			case "name":

				return name;

			case "values":

				return getResult(renderContext);

			case "dataType":

				return getDataType(renderContext);

			case "selectedValue":
				throw new UnsupportedOperationException("RootFoldersDataSource has no selectedValue.");

			case "currentValue":
				// the loop object
				//return renderContext.getDataNode(configuration.getDataAdapter().getDataKey());
				throw new UnsupportedOperationException("currentValue is currently not supported.");
		}

		return null;
	}
}

