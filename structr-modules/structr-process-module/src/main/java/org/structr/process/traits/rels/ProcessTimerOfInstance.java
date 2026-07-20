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
 * ProcessTimer -[OF_INSTANCE]-> ProcessInstance.
 *
 * Cascade TARGET_TO_SOURCE: deleting a ProcessInstance cascades to its timers,
 * matching the existing token/task/parameter-value lifecycle.
 */
public class ProcessTimerOfInstance extends AbstractRelationshipTraitDefinition implements RelationshipBaseTraitDefinition {

	public ProcessTimerOfInstance() {
		super(ProcessTraits.PROCESS_TIMER_OF_INSTANCE);
	}

	@Override
	public String getSourceType() {
		return ProcessTraits.PROCESS_TIMER;
	}

	@Override
	public String getTargetType() {
		return ProcessTraits.PROCESS_INSTANCE;
	}

	@Override
	public String getRelationshipType() {
		return "OF_INSTANCE";
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
