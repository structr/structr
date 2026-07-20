/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.process.traits.wrappers;

import org.structr.api.util.Iterables;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.Traits;
import org.structr.core.traits.wrappers.AbstractNodeTraitWrapper;
import org.structr.process.entity.BpmnElement;
import org.structr.process.entity.BpmnLane;
import org.structr.process.traits.definitions.BpmnBaseNodeTraitDefinition;
import org.structr.process.traits.definitions.BpmnLaneTraitDefinition;

public class BpmnLaneTraitWrapper extends AbstractNodeTraitWrapper implements BpmnLane {

	public BpmnLaneTraitWrapper(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	@Override
	public String getBpmnId() {
		return wrappedObject.getProperty(traits.key(BpmnBaseNodeTraitDefinition.BPMN_ID_PROPERTY));
	}

	@Override
	public String getBpmnName() {
		return wrappedObject.getProperty(traits.key(BpmnLaneTraitDefinition.BPMN_NAME_PROPERTY));
	}

	@Override
	public Iterable<BpmnElement> getFlowNodeRefs() {

		final PropertyKey<Iterable<NodeInterface>> key = traits.key(BpmnLaneTraitDefinition.FLOW_NODE_REFS_PROPERTY);

		return Iterables.map(n -> n.as(BpmnElement.class), wrappedObject.getProperty(key));
	}
}
