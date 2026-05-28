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
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.function.Functions;
import org.structr.schema.action.ActionContext;
import org.structr.web.common.RenderContext;
import org.structr.web.entity.ComponentConfiguration;
import org.structr.web.entity.dom.DOMNode;

public class ParentDataSource<T extends GraphObject> extends AbstractValueDataSource<T> {

	private static final Logger logger = LoggerFactory.getLogger(ChannelDataSource.class);

	public ParentDataSource(final String name) {
		super(name);
	}

	@Override
	public String getChannelName() {
		return Functions.cleanString(name);
	}

	@Override
	public int getDimension() {
		// parent is exactly one object
		return 0;
	}

	@Override
	protected T getDataSourceValue(final ActionContext actionContext) throws FrameworkException {

		if (actionContext instanceof RenderContext renderContext) {
			return (T) renderContext.getDataNode(name);
		}

		return null;
	}

	@Override
	protected String resolveDataType(final ActionContext actionContext) throws FrameworkException {

		if (configuration != null) {

			// find the closest ancestor component and get its data type
			final DOMNode component = configuration.getComponent();
			if (component != null) {

				final DOMNode parent = component.getParent();
				if (parent != null) {

					final DOMNode parentComponent = parent.getClosestComponent();
					if (parentComponent != null) {

						final ComponentConfiguration parentConfig = parentComponent.getComponentConfiguration();
						if (parentConfig != null) {

							final Channel dataSource = parentConfig.getDataSource();
							if (dataSource != null) {

								// the parent data source type + this configuration's transform gives the result type
								// e.g. if parent is "Project" and transform is "tasks", result type is "Task"
								final String parentType    = dataSource.getDataType(actionContext);
								final String thisTransform = configuration.getTransform();

								return getTransformedDataType(parentType, thisTransform);
							}
						}
					}
				}
			}

		} else {

			logger.warn("Cannot evaluate resolveDataType(): configuration is null in {} '{}'.", getClass().getSimpleName(), getChannelName());
		}

		return null;
	}
}
