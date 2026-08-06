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
 * TaskInstance -[DECLINED_BY]-> Principal.
 *
 * Records that a Principal (user or group) has actively declined this task.
 * Decline is a vote, not a permission change: the declining principal keeps
 * their R+W grant on the task (so they can undo their decision) but their
 * intent is recorded for audit and for "all-declined" stalled-task detection.
 *
 * Multiple principals can decline the same task; a principal can decline
 * multiple tasks. Engine clears the entry on (re-)claim, since claiming
 * supersedes a previous decline.
 */
public class TaskInstanceDeclinedBy extends AbstractRelationshipTraitDefinition implements RelationshipBaseTraitDefinition {

	public TaskInstanceDeclinedBy() {

		super(ProcessTraits.TASK_INSTANCE_DECLINED_BY);
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

		return "DECLINED_BY";
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
