/*
 * Copyright (C) 2010-2026 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
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
 * BpmnParticipant -[OF_PROCESS]-> BpmnProcess. The participant references
 * exactly one process; the process is referenced by at most one participant.
 * Cascading delete is OFF: deleting a participant must not delete its process
 * (running instances may still depend on it). The collaboration owns the
 * participant; the definitions / collaboration owns the process.
 */
public class BpmnParticipantOfProcess extends AbstractRelationshipTraitDefinition implements RelationshipBaseTraitDefinition {

	public BpmnParticipantOfProcess() { super(ProcessTraits.BPMN_PARTICIPANT_OF_PROCESS); }

	@Override public String getSourceType() { return ProcessTraits.BPMN_PARTICIPANT; }
	@Override public String getTargetType() { return ProcessTraits.BPMN_PROCESS; }
	@Override public String getRelationshipType() { return "OF_PROCESS"; }
	@Override public Relation.Multiplicity getSourceMultiplicity() { return Relation.Multiplicity.One; }
	@Override public Relation.Multiplicity getTargetMultiplicity() { return Relation.Multiplicity.One; }
	@Override public int getCascadingDeleteFlag() { return Relation.NONE; }
	@Override public int getAutocreationFlag() { return Relation.ALWAYS; }
	@Override public boolean isInternal() { return false; }
	@Override public PropagationDirection getPropagationDirection() { return PropagationDirection.Both; }
	@Override public PropagationMode getReadPropagation() { return PropagationMode.Add; }
	@Override public PropagationMode getWritePropagation() { return PropagationMode.Keep; }
	@Override public PropagationMode getDeletePropagation() { return PropagationMode.Keep; }
	@Override public PropagationMode getAccessControlPropagation() { return PropagationMode.Keep; }
	@Override public String getDeltaProperties() { return null; }
}
