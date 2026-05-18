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
import org.structr.core.function.Functions;
import org.structr.core.graph.NodeInterface;
import org.structr.schema.action.ActionContext;
import org.structr.web.common.RenderContext;
import org.structr.web.entity.ComponentConfiguration;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;

public class ParentDataSource<T extends GraphObject> extends AbstractValueDataSource<T> {

	private static final Logger logger = LoggerFactory.getLogger(ChannelDataSource.class);

	private final ComponentConfiguration parentConfiguration;

	public ParentDataSource(final ComponentConfiguration thisConfiguration, final ComponentConfiguration parentConfiguration, final String name) {

		super(thisConfiguration, name);

		this.parentConfiguration = parentConfiguration;
	}


	@Override
	public String getChannelName() {
		return Functions.cleanString(name);
	}

	@Override
	protected T getDataSourceValue(ActionContext actionContext, ChannelInput channelInput) throws FrameworkException {

		if (actionContext instanceof RenderContext renderContext) {
			return (T) renderContext.getDataNode(name);
		}

		return null;
	}

	@Override
	public String getDataType(final ActionContext actionContext) throws FrameworkException {

		if (configuration != null && parentConfiguration != null) {

			final Channel dataSource = parentConfiguration.getDataSource();
			if (dataSource != null) {

				// this is tricky to understand: the parent data source uses the current configuration's transform value!
				// (e.g. Release->Feature: "parent.features" => type Feature
				return getTransformedDataType(dataSource.getDataType(actionContext), configuration.getTransform());
			}

		} else {

			logger.warn("Cannot evaluate getDataType(): configuration is null in {} '{}'.", getClass().getSimpleName(), getChannelName());
		}

		return null;
	}
}
