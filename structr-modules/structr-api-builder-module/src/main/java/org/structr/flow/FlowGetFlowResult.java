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
package org.structr.flow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.core.property.EndNode;
import org.structr.core.property.Property;
import org.structr.flow.api.FlowResult;
import org.structr.flow.engine.Context;
import org.structr.flow.engine.FlowEngine;
import org.structr.flow.rels.FlowDataSourceFlowNode;

/**
 *
 */
public class FlowGetFlowResult extends FlowDataSource {

	private static final Logger logger = LoggerFactory.getLogger(FlowGetFlowResult.class);

	public static final Property<FlowNode> otherFlow = new EndNode<>("otherFlow", FlowDataSourceFlowNode.class);

	public static final View defaultView = new View(FlowGetFlowResult.class, PropertyView.Public, otherFlow);
	public static final View uiView      = new View(FlowGetFlowResult.class, PropertyView.Ui,     otherFlow);

	@Override
	public Object get(final Context context) {

		final FlowNode _otherSource = getProperty(otherFlow);
		if (_otherSource != null) {

			final FlowEngine engine = new FlowEngine(context);
			final FlowResult result = engine.execute(_otherSource);

			// TODO: handle error
			return result.getResult();

		} else {

			logger.warn("Unable to evaluate FlowDataSource {}, missing flow source.", getUuid());
		}

		return null;
	}
}
