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
 * TaskInstance -[TASK_OF]-> ProcessInstance.
 *
 * Cascade direction is TARGET_TO_SOURCE: deleting a ProcessInstance also
 * deletes its TaskInstances. A task has no meaningful identity outside its
 * parent instance, so this keeps the engine's runtime graph self-consistent.
 * Tokens and parameter values cascade for the same reason via their own rels.
 *
 * <p>Read propagation: {@code In} means read flows from target (instance) back
 * to source (task). The engine grants read on the ProcessInstance to every
 * participant (initiator, candidates, assignees); propagation extends that
 * grant to all the instance's tasks. Single grant point, generic across all
 * roles regardless of process domain.</p>
 */
public class TaskInstanceOfProcess extends AbstractRelationshipTraitDefinition implements RelationshipBaseTraitDefinition {

	public TaskInstanceOfProcess() {
		super(ProcessTraits.TASK_INSTANCE_OF_PROCESS);
	}

	@Override
	public String getSourceType() {
		return ProcessTraits.TASK_INSTANCE;
	}

	@Override
	public String getTargetType() {
		return ProcessTraits.PROCESS_INSTANCE;
	}

	@Override
	public String getRelationshipType() {
		return "TASK_OF";
	}

	@Override
	public Relation.Multiplicity getSourceMultiplicity() {
		return Relation.Multiplicity.Many;
	}

	@Override
	public Relation.Multiplicity getTargetMultiplicity() {
		return Relation.Multiplicity.One;
	}

	@Override
	public int getCascadingDeleteFlag() {
		return Relation.TARGET_TO_SOURCE;
	}

	@Override
	public int getAutocreationFlag() {
		return Relation.NONE;
	}

	@Override
	public boolean isInternal() {
		return false;
	}

	@Override
	public PropagationDirection getPropagationDirection() {
		return PropagationDirection.In;
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
