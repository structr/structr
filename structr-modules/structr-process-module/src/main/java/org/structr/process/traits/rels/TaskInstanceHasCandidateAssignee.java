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
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.definitions.AbstractRelationshipTraitDefinition;
import org.structr.core.traits.definitions.RelationshipBaseTraitDefinition;
import org.structr.process.ProcessTraits;

/**
 * TaskInstance -[HAS_CANDIDATE_ASSIGNEE]-> Principal.
 *
 * The set of principals (users or groups) eligible to claim this task. When a
 * Principal here is a Group, any member of that group is authorized to claim.
 *
 * <p>Naming note: the BPMN file element is {@code <potentialOwner>} (spec-mandated
 * vocabulary), but the runtime property and relationship use "candidate assignee"
 * to avoid clashing with Structr's node-ownership concept (every node has an
 * {@code owner} principal). See design decision 11.25.</p>
 */
public class TaskInstanceHasCandidateAssignee extends AbstractRelationshipTraitDefinition implements RelationshipBaseTraitDefinition {

	public TaskInstanceHasCandidateAssignee() {

		super(ProcessTraits.TASK_INSTANCE_HAS_CANDIDATE_ASSIGNEE);
	}

	@Override
	public String getSourceType() {

		return ProcessTraits.TASK_INSTANCE;
	}

	@Override
	public String getTargetType() {

		return StructrTraits.PRINCIPAL;
	}

	@Override
	public String getRelationshipType() {

		return "HAS_CANDIDATE_ASSIGNEE";
	}

	@Override
	public Relation.Multiplicity getSourceMultiplicity() {

		return Relation.Multiplicity.Many;
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

		return Relation.NONE;
	}

	@Override
	public boolean isInternal() {

		return false;
	}

	@Override
	public PropagationDirection getPropagationDirection() {

		return PropagationDirection.None;
	}

	@Override
	public PropagationMode getReadPropagation() {

		return PropagationMode.Keep;
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
