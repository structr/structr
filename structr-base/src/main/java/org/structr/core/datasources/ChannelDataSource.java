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
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.function.Functions;
import org.structr.core.graph.NodeInterface;
import org.structr.schema.action.ActionContext;
import org.structr.web.common.RenderContext;
import org.structr.web.entity.ComponentConfiguration;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;

import java.util.Objects;

public class ChannelDataSource<T extends GraphObject> extends AbstractValueDataSource<T> {

	private static final Logger logger = LoggerFactory.getLogger(ChannelDataSource.class);

	public ChannelDataSource(final String name) {

		super(name);
	}

	@Override
	public String getChannelName() {

		return Functions.cleanString(name);
	}

	@Override
	public int getDimension() {

		return 0;
	}

	@Override
	public T getDataSourceValue(final ActionContext actionContext) throws FrameworkException {

		if (actionContext instanceof RenderContext renderContext) {

			final String uuid = renderContext.getChannelValue(name);
			if (uuid != null) {

				return (T) StructrApp.getInstance(actionContext.getSecurityContext()).getNodeById(uuid);
			}
		}

		return null;
	}

	@Override
	protected String resolveDataType(final ActionContext actionContext) throws FrameworkException {

		if (configuration != null) {

			// find channel source by walking all nodes on the same page
			final DOMNode component = configuration.getComponent();
			if (component != null) {

				final Page page = component.getOwnerDocument();
				if (page != null) {

					for (final NodeInterface childNode : page.getAllChildNodes()) {

						final DOMNode candidate                  = childNode.as(DOMNode.class);
						final ComponentConfiguration otherConfig = candidate.getComponentConfiguration();

						// look for a controller that writes to our channel
						if (otherConfig != null && !Objects.equals(otherConfig, configuration)) {

							final String selectionChannel = otherConfig.getSelectionChannel();
							if (name.equals(selectionChannel)) {

								final Channel dataSource = otherConfig.getDataSource();
								if (dataSource != null) {

									final String otherType     = dataSource.getDataType(actionContext);
									final String thisTransform = configuration.getTransform();

									return getTransformedDataType(otherType, thisTransform);
								}
							}
						}
					}
				}

			} else {

				logger.warn("Cannot evaluate resolveDataType(): configuration is not attached to a component.");
			}

		} else {

			logger.warn("Cannot evaluate resolveDataType(): configuration is null in {} '{}'.", getClass().getSimpleName(), getChannelName());
		}

		return null;
	}
}
