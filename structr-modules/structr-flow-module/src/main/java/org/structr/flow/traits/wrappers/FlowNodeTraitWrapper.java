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
package org.structr.flow.traits.wrappers;

import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.Traits;
import org.structr.flow.api.FlowElement;
import org.structr.flow.api.FlowType;
import org.structr.flow.impl.FlowContainer;
import org.structr.flow.impl.FlowNode;


/**
 *
 */
public class FlowNodeTraitWrapper extends FlowBaseNodeTraitWrapper implements FlowNode {

	public FlowNodeTraitWrapper(final Traits traits, final NodeInterface node) {
		super(traits, node);
	}

	@Override
	public FlowType getFlowType() {
		return null;
	}

	@Override
	public FlowContainer getFlowContainer() {

		final NodeInterface container = wrappedObject.getProperty(traits.key("flowContainer"));
		if (container != null) {

			return container.as(FlowContainer.class);
		}

		return null;
	}

	@Override
	public FlowElement next() {

		final NodeInterface next = wrappedObject.getProperty(traits.key("next"));
		if (next != null) {

			return next.as(FlowElement.class);
		}

		return null;
	}
}
