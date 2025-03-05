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

import org.structr.flow.api.DataSource;
import org.structr.flow.api.ThrowingElement;
import org.structr.module.api.DeployableEntity;

public interface FlowCall extends FlowActionNode, DataSource, DeployableEntity, ThrowingElement {

	/*

	private static final Logger logger 									= LoggerFactory.getLogger(FlowCall.class);

	public static final Property<Iterable<FlowBaseNode>> dataTarget       = new EndNodes<>("dataTarget", FlowDataInput.class);
	public static final Property<Iterable<FlowParameterInput>> parameters = new StartNodes<>("parameters", FlowCallParameter.class);
	public static final Property<FlowContainer> flow                      = new EndNode<>("flow", FlowCallContainer.class);

	public static final View defaultView 								= new View(FlowCall.class, PropertyView.Public, flow, dataTarget, parameters, isStartNodeOfContainer);
	public static final View uiView      								= new View(FlowCall.class, PropertyView.Ui,     flow, dataTarget, parameters, isStartNodeOfContainer, flowContainer);

	@Override
	public void execute(Context context) throws FlowException {

		final List<FlowParameterInput> params = Iterables.toList(getProperty(parameters));
		final FlowContainer flow              = getProperty(FlowCall.flow);

		if (flow != null) {

			Context functionContext = new Context(context.getThisObject());

			final FlowEngine engine = new FlowEngine(functionContext);
			FlowNode startNode = flow.getProperty(FlowContainer.startNode);

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
	public Object get(Context context) throws FlowException {
		if (!context.hasData(getUuid())) {
			this.execute(context);
		}
		return context.getData(getUuid());
	}

	@Override
	public Map<String, Object> exportData() {
		Map<String, Object> result = new HashMap<>();

		result.put("id", this.getUuid());
		result.put("type", this.getClass().getSimpleName());
		result.put("visibleToPublicUsers", this.getProperty(visibleToPublicUsers));
		result.put("visibleToAuthenticatedUsers", this.getProperty(visibleToAuthenticatedUsers));

		return result;
	}

	@Override
	public FlowExceptionHandler getExceptionHandler(Context context) {
		return null;
	}
	*/
}
