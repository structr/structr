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

import org.structr.flow.rels.FlowNodes;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.core.Export;
import org.structr.core.property.EndNode;
import org.structr.core.property.Property;
import org.structr.core.property.StartNode;
import org.structr.core.property.StartNodes;
import org.structr.core.property.StringProperty;
import org.structr.flow.api.FlowElement;
import org.structr.flow.api.FlowResult;
import org.structr.flow.engine.FlowEngine;
import org.structr.flow.rels.FlowDecisionFalse;
import org.structr.flow.rels.FlowDecisionTrue;
import org.structr.flow.rels.FlowForEachBody;

/**
 *
 */
public abstract class FlowNode extends FlowBaseNode implements FlowElement {

	public static final Property<FlowNode> next                      = new EndNode<>("next", FlowNodes.class);
	public static final Property<List<FlowDecision>> isTrueResultOf  = new StartNodes<>("isTrueResultOf", FlowDecisionTrue.class);
	public static final Property<List<FlowDecision>> isFalseResultOf = new StartNodes<>("isFalseResultOf", FlowDecisionFalse.class);
	public static final Property<FlowForEach> isLoopBodyOf           = new StartNode<>("isLoopBodyOf", FlowForEachBody.class);
	public static final Property<String> name                        = new StringProperty("name").indexed().unique().notNull();

	public static final View defaultView = new View(FlowNode.class, PropertyView.Public, name, next, isTrueResultOf, isFalseResultOf, isLoopBodyOf);
	public static final View uiView      = new View(FlowNode.class, PropertyView.Ui,     name, next, isTrueResultOf, isFalseResultOf, isLoopBodyOf);

	@Override
	public FlowElement next() {
		return getProperty(next);
	}

	@Override
	public void connect(FlowElement next) {
	}

	@Export
	public Map<String, Object> evaluate(final Map<String, Object> parameters) {

		final Map<String, Object> map = new LinkedHashMap<>();
		final FlowEngine engine = new FlowEngine();
		final FlowResult result = engine.execute(this);

		map.put("error",  result.getError());
		map.put("result", result.getResult());

		return map;
	}
}
