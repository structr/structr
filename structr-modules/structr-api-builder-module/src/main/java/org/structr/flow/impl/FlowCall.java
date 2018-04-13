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
import org.structr.core.property.Property;
import org.structr.core.property.StartNode;
import org.structr.flow.api.Action;
import org.structr.flow.api.FlowResult;
import org.structr.flow.engine.Context;
import org.structr.flow.engine.FlowEngine;
import org.structr.flow.impl.rels.FlowCallContainer;
import org.structr.flow.impl.rels.FlowDataInput;

public class FlowCall extends FlowNode implements Action {

	private static final Logger logger = LoggerFactory.getLogger(FlowCall.class);

	public static final Property<FlowDataSource> dataSource 		= new StartNode<>("dataSource", FlowDataInput.class);
	public static final Property<FlowContainer> flow              = new EndNode<>("flow", FlowCallContainer.class);

	public static final View defaultView 							= new View(FlowCall.class, PropertyView.Public, flow, dataSource);
	public static final View uiView      							= new View(FlowCall.class, PropertyView.Ui,     flow, dataSource);

	@Override
	public void execute(Context context) {

		FlowDataSource dataSource = getProperty(FlowCall.dataSource);
		FlowContainer flow = getProperty(FlowCall.flow);

		if (flow != null) {

			Context functionContext = new Context(context.getThisObject());

			if (dataSource != null) {

				functionContext.setData(dataSource.get(context));
			} else {

				functionContext.setData(context.getData());
			}

			final FlowEngine engine = new FlowEngine(functionContext);
			FlowNode startNode = flow.getProperty(FlowContainer.startNode);

			if (startNode != null) {

				final FlowResult result = engine.execute(functionContext, startNode);
				// Save result
				context.setData(result.getResult());
			} else {

				logger.warn("Unable to evaluate FlowCall {}, flow container doesn't specify a start node.", getUuid());
			}

			// TODO: handle error

		} else {

			logger.warn("Unable to evaluate FlowCall {}, missing flow container.", getUuid());
		}


	}

}
