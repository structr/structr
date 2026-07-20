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
package org.structr.process.traits.rels;

import org.structr.api.graph.PropagationDirection;
import org.structr.api.graph.PropagationMode;
import org.structr.core.entity.Relation;
import org.structr.core.traits.definitions.AbstractRelationshipTraitDefinition;
import org.structr.core.traits.definitions.RelationshipBaseTraitDefinition;
import org.structr.process.ProcessTraits;

/**
 * BpmnLane -[HAS_FLOW_NODE]-> BpmnElement. Encodes a lane's
 * {@code <bpmn:flowNodeRef>} entries: each lane references zero or more
 * elements that visually belong inside it. An element belongs to at most
 * one lane (BPMN 2.0 rule), so target multiplicity is many but elements
 * should never be wired into multiple lanes.
 *
 * <p>Cascading delete is OFF: a lane disappearing must not take its flow
 * elements with it. The lane is purely a layout / grouping construct.</p>
 */
public class BpmnLaneHasFlowNode extends AbstractRelationshipTraitDefinition implements RelationshipBaseTraitDefinition {

	public BpmnLaneHasFlowNode() {
		super(ProcessTraits.BPMN_LANE_HAS_FLOW_NODE);
	}

	@Override
	public String getSourceType() {
		return ProcessTraits.BPMN_LANE;
	}

	@Override
	public String getTargetType() {
		return ProcessTraits.BPMN_ELEMENT;
	}

	@Override
	public String getRelationshipType() {
		return "HAS_FLOW_NODE";
	}

	@Override
	public Relation.Multiplicity getSourceMultiplicity() {
		return Relation.Multiplicity.One;
	}

	@Override
	public Relation.Multiplicity getTargetMultiplicity() {
		return Relation.Multiplicity.Many;
	}

	@Override
	public int getCascadingDeleteFlag() {
		return Relation.NONE;
	}

	@Override
	public int getAutocreationFlag() {
		return Relation.ALWAYS;
	}

	@Override
	public boolean isInternal() {
		return false;
	}

	@Override
	public PropagationDirection getPropagationDirection() {
		return PropagationDirection.Both;
	}

	@Override
	public PropagationMode getReadPropagation() {
		return PropagationMode.Add;
	}

	@Override
	public PropagationMode getWritePropagation() {
		return PropagationMode.Keep;
	}

	@Override
	public PropagationMode getDeletePropagation() {
		return PropagationMode.Keep;
	}

	@Override
	public PropagationMode getAccessControlPropagation() {
		return PropagationMode.Keep;
	}

	@Override
	public String getDeltaProperties() {
		return null;
	}
}
