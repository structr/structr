/*
 * Copyright (C) 2010-2024 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.flow.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.util.Iterables;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.Traits;
import org.structr.flow.api.DataSource;
import org.structr.flow.api.FlowResult;
import org.structr.flow.api.ThrowingElement;
import org.structr.flow.engine.Context;
import org.structr.flow.engine.FlowEngine;
import org.structr.flow.engine.FlowException;
import org.structr.module.api.DeployableEntity;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class FlowCall extends FlowActionNode implements DataSource, DeployableEntity, ThrowingElement {

	private static final Logger logger = LoggerFactory.getLogger(FlowCall.class);

	public FlowCall(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	public Iterable<FlowParameterInput> getParameters() {

		final Iterable<NodeInterface> nodes = wrappedObject.getProperty(traits.key("parameters"));

		return Iterables.map(n -> n.as(FlowParameterInput.class), nodes);
	}

	public FlowContainer getFlow() {

		final NodeInterface node = wrappedObject.getProperty(traits.key("flow"));
		if (node != null) {

			return node.as(FlowContainer.class);
		}

		return null;
	}

	@Override
	public void execute(Context context) throws FlowException {

		final List<FlowParameterInput> params = Iterables.toList(getParameters());
		final FlowContainer flow              = getFlow();

		if (flow != null) {

			final Context functionContext = new Context(context.getThisObject());
			final FlowEngine engine       = new FlowEngine(functionContext);
			final FlowNode startNode      = flow.getStartNode();

			if (startNode != null) {

				// Inject all parameters into context
				if (params != null && params.size() > 0) {

					for (FlowParameterInput p : params) {
						p.process(context, functionContext);
					}
				}

				try {
					final FlowResult result = engine.execute(functionContext, startNode);

					// Save result
					context.setData(getUuid(), result.getResult());

					if (result.getError() != null) {

						throw new FrameworkException(422, "FlowCall encountered an unexpected exception during execution." + result.getError().getMessage());
					}
				} catch (FrameworkException ex) {

					throw new FlowException(ex, this);
				}

			} else {

				logger.warn("Unable to evaluate FlowCall {}, flow container doesn't specify a start node.", getUuid());
			}

			// TODO: handle error

		} else {

			logger.warn("Unable to evaluate FlowCall {}, missing flow container.", getUuid());
		}

	}

	@Override
	public Object get(final Context context) throws FlowException {
		if (!context.hasData(getUuid())) {
			this.execute(context);
		}
		return context.getData(getUuid());
	}

	@Override
	public Map<String, Object> exportData() {

		final Map<String, Object> result = new TreeMap<>();

		result.put("id",                          getUuid());
		result.put("type",                        getType());
		result.put("visibleToPublicUsers",        isVisibleToPublicUsers());
		result.put("visibleToAuthenticatedUsers", isVisibleToAuthenticatedUsers());

		return result;
	}

	@Override
	public FlowExceptionHandler getExceptionHandler(Context context) {
		return null;
	}
}
