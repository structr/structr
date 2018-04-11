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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.core.Export;
import org.structr.core.entity.AbstractNode;
import org.structr.core.property.EndNode;
import org.structr.core.property.EndNodes;
import org.structr.core.property.Property;
import org.structr.core.property.StringProperty;
import org.structr.flow.api.FlowResult;
import org.structr.flow.engine.Context;
import org.structr.flow.engine.FlowEngine;
import org.structr.flow.impl.rels.FlowContainerBaseNode;
import org.structr.flow.impl.rels.FlowContainerFlowNode;

/**
 *
 */
public class FlowContainer extends AbstractNode {

	public static final Property<List<FlowBaseNode>> flowNodes = new EndNodes<>("flowNodes", FlowContainerBaseNode.class);
	public static final Property<FlowNode> startNode           = new EndNode<>("startNode", FlowContainerFlowNode.class);
	public static final Property<String> name                  = new StringProperty("name").indexed().unique().notNull();

	public static final View defaultView = new View(FlowContainer.class, PropertyView.Public, name, flowNodes, startNode);
	public static final View uiView      = new View(FlowContainer.class, PropertyView.Ui,     name, flowNodes, startNode);

	@Export
	public Map<String, Object> evaluate(final Map<String, Object> parameters) {

		final FlowEngine engine       = new FlowEngine(new Context(null, parameters));
		final FlowResult result       = engine.execute(getProperty(startNode));
		final Map<String, Object> map = new LinkedHashMap<>();

		map.put("error",  result.getError());
		map.put("result", result.getResult());

		return map;
	}

}
