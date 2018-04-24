/**
 * Copyright (C) 2010-2018 Structr GmbH
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
import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.core.property.EndNode;
import org.structr.core.property.EndNodes;
import org.structr.core.property.Property;
import org.structr.core.property.StartNodes;
import org.structr.flow.api.DataSource;
import org.structr.flow.api.FlowResult;
import org.structr.flow.engine.Context;
import org.structr.flow.engine.FlowEngine;
import org.structr.flow.impl.rels.FlowCallContainer;
import org.structr.flow.impl.rels.FlowCallParameter;
import org.structr.flow.impl.rels.FlowDataInput;

import java.util.List;

public class FlowCall extends FlowActionNode implements DataSource {

	private static final Logger logger 									= LoggerFactory.getLogger(FlowCall.class);

	public static final Property<List<FlowBaseNode>> dataTarget			= new EndNodes<>("dataTarget", FlowDataInput.class);
	public static final Property<List<FlowParameterInput>> parameters 	= new StartNodes<>("parameters", FlowCallParameter.class);
	public static final Property<FlowContainer> flow                	= new EndNode<>("flow", FlowCallContainer.class);

	public static final View defaultView 								= new View(FlowCall.class, PropertyView.Public, flow, dataTarget, parameters);
	public static final View uiView      								= new View(FlowCall.class, PropertyView.Ui,     flow, dataTarget, parameters);

	@Override
	public void execute(Context context) {
		FlowContainer flow = getProperty(FlowCall.flow);
		List<FlowParameterInput> params = getProperty(parameters);

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

				final FlowResult result = engine.execute(functionContext, startNode);
				// Save result
				context.setData(getUuid(), result.getResult());
			} else {

				logger.warn("Unable to evaluate FlowCall {}, flow container doesn't specify a start node.", getUuid());
			}

			// TODO: handle error

		} else {

			logger.warn("Unable to evaluate FlowCall {}, missing flow container.", getUuid());
		}

	}

	@Override
	public Object get(Context context) {
		return context.getData(getUuid());
	}
}
