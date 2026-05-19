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
import org.structr.api.util.Iterables;
import org.structr.api.util.PagingIterable;
import org.structr.common.ChannelInput;
import org.structr.common.PropertyView;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.function.Functions;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.RelationProperty;
import org.structr.core.traits.Traits;
import org.structr.schema.action.ActionContext;
import org.structr.web.common.RenderContext;
import org.structr.web.datasource.FieldDefinition;
import org.structr.web.entity.ComponentConfiguration;

import java.util.LinkedHashMap;
import java.util.Map;

public abstract class AbstractValueDataSource<T extends GraphObject> implements Channel<T> {

	protected final ComponentConfiguration configuration;
	protected final String name;

	protected abstract T getDataSourceValue(final ActionContext actionContext, final ChannelInput channelInput) throws FrameworkException;

	public AbstractValueDataSource(final ComponentConfiguration configuration, String name) {

		this.configuration = configuration;
		this.name          = name;
	}

	@Override
	public String getChannelName() {
		return Functions.cleanString(name);
	}

	@Override
	public final ChannelResult<T> getResult(final ActionContext actionContext, final ChannelInput input) throws FrameworkException {

		if (name != null) {

			final GraphObject node = getDataSourceValue(actionContext, input);
			if (node != null) {

				if (input != null) {

					final String transform = input.transform();
					if (transform != null) {

						final Traits      traits = node.getTraits();
						final PropertyKey key    = traits.key(transform);

						if (key != null) {

							// this is where we need to implement pagination and filtering!
							final Object value = node.getProperty(key);

							if (value != null) {

								if (value instanceof Iterable iterable) {

									final String            name             = transform + " of " + node.getUuid();
									final Iterable<T>       filteredIterable = Iterables.filter(input, iterable);
									final PagingIterable<T> pagingIterable   = new PagingIterable<>(name, filteredIterable, input.pageSize(), input.page());

									return ChannelResult.fromIterable(pagingIterable);
								}

								// return single result as well
								return ChannelResult.fromObject((T) value);
							}
						}
					}
				}

				return (ChannelResult<T>) ChannelResult.fromObject(node);
			}
		}

		return new ChannelResult<>();
	}

	@Override
	public final Map<String, FieldDefinition> getFields(final ActionContext actionContext) throws FrameworkException {


		final Map<String, FieldDefinition> output = new LinkedHashMap<>();
		final String dataType = getDataType(actionContext);

		if (dataType != null) {

			final Traits traits = Traits.of(dataType);

			// transform input
			for (final PropertyKey key : traits.getPropertyKeysForView(PropertyView.All)) {

				output.put(key.jsonName(), key.getFieldDefinition());
			}

		} else {

			System.out.println("DataSource " + name + " has no dataType.");
		}

		return output;
	}

	public Object evaluate(final ActionContext actionContext, final String key, final String defaultValue, final GraphObject contextObject, final int row, final int column) throws FrameworkException {

		final ChannelInput input          = new ChannelInput(configuration.getTransform());
		final RenderContext renderContext = (RenderContext) actionContext;

		switch (key) {

			case "name":
				return name;

			case "values":
				return getResult(renderContext, input);

			case "dataType":
				return getDataType(actionContext);

			case "selectedValue":
				// the selected object from the channel
				return getValue(renderContext, input);

			case "currentValue":
				// the loop object
				return renderContext.getDataNode(configuration.getDataAdapter().getDataKey());
		}

		return null;
	}

	// ----- protected methods -----
	protected String getTransformedDataType(final String type, final String transform) {

		if (Traits.exists(type)) {

			final Traits traits = Traits.of(type);

			if (traits.hasKey(transform)) {

				final PropertyKey key = traits.key(transform);

				if (key instanceof RelationProperty rel) {

					return rel.getTargetType();
				}
			}
		}

		return type;
	}

	// ----- private methods -----
	private T getValue(final ActionContext actionContext, final ChannelInput input) throws FrameworkException {

		final ChannelResult<T> result = getResult(actionContext, input);
		if (!result.isEmpty()) {

			return result.getFirst();
		}

		return null;
	}
}
