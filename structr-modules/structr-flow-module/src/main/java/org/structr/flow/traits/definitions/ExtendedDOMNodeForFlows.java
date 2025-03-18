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
package org.structr.flow.traits.definitions;

import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.EndNode;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;

import java.util.Set;

public class ExtendedDOMNodeForFlows extends AbstractNodeTraitDefinition {

	public ExtendedDOMNodeForFlows() {
		super("DOMNode.extended", "DOMNode");
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final PropertyKey<NodeInterface> flow = new EndNode("flow", "DOMNodeFLOWFlowContainer");

		return newSet(
			flow
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
