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

import org.structr.api.util.Iterables;
import org.structr.common.ChannelInput;
import org.structr.common.PropertyView;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.Traits;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.EvaluationHints;
import org.structr.web.common.RenderContext;
import org.structr.web.datasource.FieldDefinition;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ChannelDataSource implements Channel {

	private final String name;

	public ChannelDataSource(final String name) {
		this.name = name;
	}

	@Override
	public final Iterable<GraphObject> getValues(final RenderContext renderContext, final ChannelInput input) throws FrameworkException {

		if (name != null) {

			final String uuid = renderContext.getChannelValue(name);
			if (uuid != null) {

				final NodeInterface node = StructrApp.getInstance(renderContext.getSecurityContext()).getNodeById(uuid);
				if (node != null) {

					if (input != null) {

						final String transform = input.transform();
						if (transform != null) {

							final Traits traits = node.getTraits();
							final PropertyKey key = traits.key(transform);

							if (key != null) {

								return (Iterable) node.getProperty(key);
							}
						}

					} else {

						return List.of(node);
					}
				}
			}
		}

		return List.of();
	}

	@Override
	public final Map<String, FieldDefinition> getFields(final RenderContext renderContext) throws FrameworkException {

		final Map<String, FieldDefinition> output = new LinkedHashMap<>();
		final GraphObject value = getValue(renderContext);

		if (value != null) {

			final Traits traits = value.getTraits();

			// transform input
			for (final PropertyKey key : traits.getPropertyKeysForView(PropertyView.All)) {

				output.put(key.jsonName(), key.getFieldDefinition());
			}
		}

		return output;
	}

	@Override
	public String getDataType(final RenderContext renderContext) throws FrameworkException {

		final GraphObject value = getValue(renderContext);
		if (value != null) {

			return value.getType();
		}

		return null;
	}

	@Override
	public String getName() {
		return name;
	}

	public Object evaluate(final ActionContext actionContext, final String key, final String defaultValue, final EvaluationHints hints, final int row, final int column) throws FrameworkException {

		final RenderContext renderContext = (RenderContext) actionContext;

		switch (key) {

			case "values":
				return getValues(renderContext, null);

			case "dataType":
				return getDataType(renderContext);
		}

		return null;
	}

	// ----- private methods -----
	private GraphObject getValue(final RenderContext renderContext) throws FrameworkException {

		final List<GraphObject> values = Iterables.toList(getValues(renderContext, null));
		if (!values.isEmpty()) {

			return values.get(0);
		}

		return null;
	}
}
