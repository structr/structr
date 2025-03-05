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
import org.structr.core.property.*;
import org.structr.core.traits.Traits;
import org.structr.flow.api.DataSource;
import org.structr.flow.api.FlowElement;
import org.structr.flow.api.FlowType;
import org.structr.flow.api.Switch;
import org.structr.flow.impl.rels.FlowContainerFlowNode;
import org.structr.flow.impl.rels.FlowDataInput;
import org.structr.flow.impl.rels.FlowNodes;
import org.structr.flow.impl.rels.FlowSwitchCases;
import org.structr.module.api.DeployableEntity;

public class FlowSwitch extends FlowNode implements Switch, DeployableEntity {
	public static final Property<Iterable<FlowNode>> prev              = new StartNodes<>("prev", FlowNodes.class);
	public static final Property<FlowNode> switchDefault               = new EndNode<>("default", FlowNodes.class);
	public static final Property<Iterable<FlowSwitchCase>> cases       = new EndNodes<>("cases", FlowSwitchCases.class);
	public static final Property<DataSource> dataSource                = new StartNode<>("dataSource", FlowDataInput.class);
	public static final Property<FlowContainer> isStartNodeOfContainer = new StartNode<>("isStartNodeOfContainer", FlowContainerFlowNode.class);

	public static final View defaultView 						       = new View(FlowAction.class, PropertyView.Public, prev, switchDefault, cases, dataSource);
	public static final View uiView      						       = new View(FlowAction.class, PropertyView.Ui, prev, switchDefault, cases, dataSource);

	public FlowSwitch(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	@Override
	public FlowType getFlowType() {
		return FlowType.Switch;
	}

	@Override
	public FlowContainer getFlowContainer() {
		return getProperty(flowContainer);
	}

	@Override
	public FlowElement next() {
		return getProperty(switchDefault);
	}
}
