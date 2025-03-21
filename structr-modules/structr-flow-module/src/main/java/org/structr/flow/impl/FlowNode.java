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
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.Traits;
import org.structr.flow.api.FlowType;
import org.structr.flow.traits.definitions.FlowNodeTraitDefinition;
import org.structr.flow.traits.operations.GetFlowType;

public class FlowNode extends FlowBaseNode {

	public FlowNode(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	public final FlowType getFlowType() {
		return traits.getMethod(GetFlowType.class).getFlowType(this);
	}

	public FlowNode next() {

		final NodeInterface node = wrappedObject.getProperty(traits.key(FlowNodeTraitDefinition.NEXT_PROPERTY));
		if (node != null) {

			return node.as(FlowNode.class);
		}

		return null;
	}

	public void setNext(final FlowNode next) throws FrameworkException {
		wrappedObject.setProperty(traits.key(FlowNodeTraitDefinition.NEXT_PROPERTY), next);
	}
}
