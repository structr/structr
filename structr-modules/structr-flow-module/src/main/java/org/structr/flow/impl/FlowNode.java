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

import org.structr.common.error.FrameworkException;
import org.structr.flow.api.FlowType;


/**
 *
 */
public interface FlowNode extends FlowBaseNode {

	FlowType getFlowType();
	FlowContainer getFlowContainer();
	FlowNode next();


	void setNext(final FlowNode next) throws FrameworkException;
	/*

	public static final Property<FlowContainer> isStartNodeOfContainer = new StartNode<>("isStartNodeOfContainer", FlowContainerFlowNode.class);
	public static final Property<Iterable<FlowNode>> prev              = new StartNodes<>("prev", FlowNodes.class);
	public static final Property<FlowNode> next                        = new EndNode<>("next", FlowNodes.class);
	public static final Property<FlowForEach> prevForEach              = new StartNode<>("prevForEach", FlowForEachBody.class);

	public static final View defaultView = new View(FlowNode.class, PropertyView.Public, prev, next, isStartNodeOfContainer);
	public static final View uiView      = new View(FlowNode.class, PropertyView.Ui,     prev, next, isStartNodeOfContainer);

	@Override
	public FlowContainer getFlowContainer() {
		return getProperty(flowContainer);
	}

	@Override
	public FlowElement next() {
		return getProperty(next);
	}
	*/
}
