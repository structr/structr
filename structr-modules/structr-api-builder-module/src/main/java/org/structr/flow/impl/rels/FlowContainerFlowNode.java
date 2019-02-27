/**
 * Copyright (C) 2010-2019 Structr GmbH
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
package org.structr.flow.impl.rels;

import org.structr.core.entity.OneToOne;
import org.structr.core.entity.Relation;
import org.structr.flow.impl.FlowContainer;
import org.structr.flow.impl.FlowNode;

/**
 *
 */
public class FlowContainerFlowNode extends OneToOne<FlowContainer, FlowNode> {

	@Override
	public Class<FlowContainer> getSourceType() {
		return FlowContainer.class;
	}

	@Override
	public Class<FlowNode> getTargetType() {
		return FlowNode.class;
	}

	@Override
	public String name() {
		return "START_NODE";
	}

	@Override
	public int getAutocreationFlag() {
		return Relation.ALWAYS;
	}
}
