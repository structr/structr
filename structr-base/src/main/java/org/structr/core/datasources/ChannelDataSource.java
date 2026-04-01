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
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.Traits;
import org.structr.schema.action.ActionContext;
import org.structr.web.common.RenderContext;
import org.structr.web.datasource.FieldDefinition;
import org.structr.web.entity.ComponentConfiguration;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ChannelDataSource<T extends GraphObject> implements Channel<T> {

	private static final Logger logger = LoggerFactory.getLogger(ChannelDataSource.class);

	private final ComponentConfiguration configuration;
	private final String name;

	public ChannelDataSource(final ComponentConfiguration configuration,  String name) {

		this.configuration = configuration;
		this.name          = name;
	}

	@Override
	public final ChannelResult<T> getResult(final RenderContext renderContext, final ChannelInput input) throws FrameworkException {

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

								// this is where we need to implement pagination and filtering!
								final Object value = node.getProperty(key);

								if (value != null && value instanceof Iterable iterable) {

									final String name                      = transform + " of " + node.getUuid();
									final Iterable<T> filteredIterable     = Iterables.filter(input, iterable);
									final PagingIterable<T> pagingIterable = new PagingIterable<>(name, filteredIterable, input.pageSize(), input.page());

									return ChannelResult.fromIterable(pagingIterable);
								}
							}
						}
					}

					return (ChannelResult<T>) ChannelResult.fromObject(node);
				}
			}
		}

		return new ChannelResult<>();
	}

	@Override
	public final Map<String, FieldDefinition> getFields(final RenderContext renderContext) throws FrameworkException {

		final Map<String, FieldDefinition> output = new LinkedHashMap<>();
		final T value                             = getValue(renderContext, new ChannelInput(configuration.getTransform()));

		if (value != null) {

			final Traits traits = value.getTraits();

			// transform input
			for (final PropertyKey key : traits.getPropertyKeysForView(PropertyView.All)) {

				output.put(key.jsonName(), key.getFieldDefinition());
			}

		} else {

			// no current value => analyze component configuration mappings
			if (configuration != null) {

				final DOMNode component = configuration.getComponent();
				if (component != null) {

					final String transform = configuration.getTransform();
					final Page page        = component.getOwnerDocument();

					for (final NodeInterface childNode : page.getAllChildNodes()) {

						final DOMNode candidate                  = childNode.as(DOMNode.class);
						final ComponentConfiguration otherConfig = candidate.getComponentConfiguration();

						// evaluate component configuration
						if (otherConfig != null && !otherConfig.equals(this)) {

							final String selectionChannel = otherConfig.getSelectionChannel();
							if (name.equals(selectionChannel)) {

								final Channel dataSource                  = otherConfig.getDataSource();
								final Map<String, FieldDefinition> fields = dataSource.getFields(renderContext);

								if (transform != null) {

									final FieldDefinition fieldDefinition = fields.get(transform);
									if (fieldDefinition != null) {

										final String nodeType = fieldDefinition.nodeType();
										if (nodeType != null) {

											if (Traits.exists(nodeType)) {

												final Traits traits = Traits.of(nodeType);

												// transform input
												for (final PropertyKey key : traits.getPropertyKeysForView(PropertyView.All)) {

													output.put(key.jsonName(), key.getFieldDefinition());
												}

											} else {

												logger.warn("Cannot evaluate getFields(): node type '{}' does not exist.", nodeType);
											}

										} else {

											logger.warn("Cannot evaluate getFields(): field '{}' does not specify a node type.", transform);
										}

									} else {

										logger.warn("Cannot evaluated getFields(): data source does not define a field named '{}'", transform);
									}

								} else {

									return otherConfig.getDataSource().getFields(renderContext);
								}
							}
						}
					}

				} else {

					logger.warn("Cannot evaluate getFields(): configuration is not attached to a component.");
				}
			} else {

				logger.warn("Cannot evaluate getFields(): configuration is null in {} '{}'.", getClass().getSimpleName(), getName());
			}
		}

		return output;
	}

	@Override
	public String getDataType(final RenderContext renderContext) throws FrameworkException {

		final T value = getValue(renderContext, new ChannelInput(configuration.getTransform()));
		if (value != null) {

			return value.getType();
		}

		return null;
	}

	@Override
	public String getName() {
		return name;
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
				return getDataType(renderContext);

			case "selectedValue":
				// the selected object from the channel
				return getValue(renderContext, input);

			case "currentValue":
				// the loop object
				return renderContext.getDataNode(configuration.getDataAdapter().getDataKey());
		}

		return null;
	}

	// ----- private methods -----
	private T getValue(final RenderContext renderContext, final ChannelInput input) throws FrameworkException {

		final ChannelResult<T> result = getResult(renderContext, input);
		if (!result.isEmpty()) {

			return result.getFirst();
		}

		return null;
	}
}
