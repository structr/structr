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

	protected final String name;
	protected ComponentConfiguration configuration;

	protected abstract T getDataSourceValue(final ActionContext actionContext) throws FrameworkException;

	/**
	 * Subclasses implement their primary type-resolution logic here.
	 * Return null when the type cannot be determined from the data source itself
	 * (e.g. ChannelDataSource when no controller is present on the page).
	 * The base class will then try the expectedDataType fallback automatically.
	 */
	protected abstract String resolveDataType(final ActionContext actionContext) throws FrameworkException;

	public AbstractValueDataSource(final String name) {

		this.name = name;
	}

	public void setConfiguration(final ComponentConfiguration configuration) {

		this.configuration = configuration;
	}

	@Override
	public String getChannelName() {

		return Functions.cleanString(name);
	}

	/**
	 * Template method: first asks the subclass to resolve the type. If that
	 * returns null (e.g. no controller found on the page), falls back to the
	 * expectedDataType configured on the ComponentConfiguration. This covers
	 * standalone forms that have no controller in the page.
	 */
	@Override
	public final String getDataType(final ActionContext actionContext) throws FrameworkException {

		final String resolved = resolveDataType(actionContext);
		if (resolved != null) {

			return resolved;
		}

		if (configuration != null) {

			final String expectedDataType = configuration.getExpectedDataType();
			if (expectedDataType != null) {

				return getTransformedDataType(expectedDataType, configuration.getTransform());
			}
		}

		return null;
	}

	@Override
	public final ChannelResult<T> getResult(final ActionContext actionContext, final ChannelInput channelInput, final String transform) throws FrameworkException {

		if (name != null) {

			final GraphObject node = getDataSourceValue(actionContext);
			if (node != null) {

				if (transform != null) {

					final Traits traits = node.getTraits();
					final PropertyKey key = traits.key(transform);

					if (key != null) {

						final Object value = node.getProperty(key);
						if (value instanceof Iterable iterable) {

							final ChannelInput input             = channelInput != null ? channelInput : new ChannelInput();
							final Iterable<T> filteredIterable   = Iterables.filter(input, iterable);
							final PagingIterable<T> pagingResult = new PagingIterable<>(transform + " of " + node.getUuid(), filteredIterable, input.pageSize(), input.page());

							return ChannelResult.fromIterable(pagingResult);
						}

						if (value != null) {

							return ChannelResult.fromObject((T) value);
						}
					}

					return new ChannelResult<>();
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

		final RenderContext renderContext = (RenderContext) actionContext;

		switch (key) {

			case "name":

				return name;

			case "values":
				throw new UnsupportedOperationException("values is currently not supported.");

			case "dataType":

				return getDataType(actionContext);

			case "selectedValue":
				// the selected object from the channel

				return getValue(renderContext);

			case "currentValue":
				throw new UnsupportedOperationException("currentValue is currently not supported.");
				//return renderContext.getDataNode(configuration.getDataAdapter().getDataKey());
		}

		return null;
	}

	// ----- protected methods -----
	protected String getTransformedDataType(final String type, final String transform) {

		if (transform != null && Traits.exists(type)) {

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
	private T getValue(final ActionContext actionContext) throws FrameworkException {

		final ChannelResult<T> result = getResult(actionContext, null);
		if (!result.isEmpty()) {

			return result.getFirst();
		}

		return null;
	}
}
