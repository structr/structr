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
 * ProcessTimer -[FOR_TOKEN]-> ProcessToken.
 *
 * For intermediate timer events: the token waiting at the catch event.
 * For boundary timer events: the token of the parent activity (so cancelling
 * the timer or the activity can find each other).
 *
 * Optional -- timerStart timers have no associated token at creation time.
 */
public class ProcessTimerForToken extends AbstractRelationshipTraitDefinition implements RelationshipBaseTraitDefinition {

	public ProcessTimerForToken() {

		super(ProcessTraits.PROCESS_TIMER_FOR_TOKEN);
	}

	@Override
	public String getSourceType() {

		return ProcessTraits.PROCESS_TIMER;
	}

	@Override
	public String getTargetType() {

		return ProcessTraits.PROCESS_TOKEN;
	}

	@Override
	public String getRelationshipType() {

		return "FOR_TOKEN";
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
