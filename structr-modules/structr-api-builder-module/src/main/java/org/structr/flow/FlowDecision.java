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

import org.structr.flow.rels.FlowDecisionTrue;
import org.structr.flow.rels.FlowDecisionFalse;
import org.structr.flow.rels.FlowDecisionCondition;
import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.core.property.EndNode;
import org.structr.core.property.Property;
import org.structr.flow.api.DataSource;
import org.structr.flow.api.Decision;
import org.structr.flow.api.FlowElement;

/**
 *
 */
public class FlowDecision extends FlowNode implements Decision {

	public static final Property<FlowDataSource> condition = new EndNode<>("condition", FlowDecisionCondition.class);
	public static final Property<FlowNode> trueElement     = new EndNode<>("trueElement", FlowDecisionTrue.class);
	public static final Property<FlowNode> falseElement    = new EndNode<>("falseElement", FlowDecisionFalse.class);

	public static final View defaultView = new View(FlowDecision.class, PropertyView.Public, condition, trueElement, falseElement);
	public static final View uiView      = new View(FlowDecision.class, PropertyView.Ui,     condition, trueElement, falseElement);

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
}
