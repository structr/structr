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
import org.structr.schema.action.ActionContext;
import org.structr.web.common.RenderContext;
import org.structr.web.datasource.FieldDefinition;
import org.structr.web.entity.ComponentConfiguration;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;

public class ChannelDataSource<T extends GraphObject> extends AbstractValueDataSource<T> {

	private static final Logger logger = LoggerFactory.getLogger(ChannelDataSource.class);

	public ChannelDataSource(final ComponentConfiguration configuration, final String name) {
		super(configuration, name);
	}

	@Override
	public String getChannelName() {
		return Functions.cleanString(name);
	}

	@Override
	public T getDataSourceValue(final ActionContext actionContext, final ChannelInput input) throws FrameworkException {

		if (actionContext instanceof RenderContext renderContext) {

			final String uuid = renderContext.getChannelValue(name);
			if (uuid != null) {

				return (T) StructrApp.getInstance(actionContext.getSecurityContext()).getNodeById(uuid);
			}
		}

		return null;
	}

	@Override
	public String getDataType(final ActionContext actionContext) throws FrameworkException {

		// find channel source and return data type from there
		if (configuration != null) {

			final DOMNode component = configuration.getComponent();
			if (component != null) {

				final Page page = component.getOwnerDocument();

				for (final NodeInterface childNode : page.getAllChildNodes()) {

					final DOMNode candidate                  = childNode.as(DOMNode.class);
					final ComponentConfiguration otherConfig = candidate.getComponentConfiguration();

					// evaluate component configuration
					if (otherConfig != null && !otherConfig.equals(this)) {

						final String selectionChannel = otherConfig.getSelectionChannel();
						if (name.equals(selectionChannel)) {

							final Channel dataSource = otherConfig.getDataSource();
							if (dataSource != null) {

								return getTransformedDataType(dataSource.getDataType(actionContext), otherConfig.getTransform());
							}
						}
					}
				}

			} else {

				logger.warn("Cannot evaluate getDataType(): configuration is not attached to a component.");
			}

		} else {

			logger.warn("Cannot evaluate getDataType(): configuration is null in {} '{}'.", getClass().getSimpleName(), getChannelName());
		}

		return null;
	}
}
