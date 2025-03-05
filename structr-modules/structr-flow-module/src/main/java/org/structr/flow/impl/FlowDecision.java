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

import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.EndNode;
import org.structr.core.property.Property;
import org.structr.core.property.StartNode;
import org.structr.core.traits.Traits;
import org.structr.flow.api.DataSource;
import org.structr.flow.api.Decision;
import org.structr.flow.api.FlowElement;
import org.structr.flow.impl.rels.FlowDecisionCondition;
import org.structr.flow.impl.rels.FlowDecisionFalse;
import org.structr.flow.impl.rels.FlowDecisionTrue;
import org.structr.module.api.DeployableEntity;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class FlowDecision extends FlowNode implements Decision, DeployableEntity {

	public FlowDecision(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	public static final Property<FlowCondition> condition = new StartNode<>("condition", FlowDecisionCondition.class);
	public static final Property<FlowNode> trueElement    = new EndNode<>("trueElement", FlowDecisionTrue.class);
	public static final Property<FlowNode> falseElement   = new EndNode<>("falseElement", FlowDecisionFalse.class);

	public static final View defaultView = new View(FlowDecision.class, PropertyView.Public, condition, trueElement, falseElement, isStartNodeOfContainer);
	public static final View uiView      = new View(FlowDecision.class, PropertyView.Ui,     condition, trueElement, falseElement, isStartNodeOfContainer);

	@Override
	public DataSource getCondition() {
		return getProperty(condition);
	}

	@Override
	public FlowElement getTrueElement() {
		return getProperty(trueElement);
	}

	@Override
	public FlowElement getFalseElement() {
		return getProperty(falseElement);
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
}
