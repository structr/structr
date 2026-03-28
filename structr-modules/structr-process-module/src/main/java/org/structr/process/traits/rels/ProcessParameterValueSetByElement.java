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

/** ProcessParameterValue -[SET_BY_ELEMENT]-> BpmnElement */
public class ProcessParameterValueSetByElement extends AbstractRelationshipTraitDefinition implements RelationshipBaseTraitDefinition {

	public ProcessParameterValueSetByElement() { super(ProcessTraits.PROCESS_PARAMETER_VALUE_SET_BY_ELEMENT); }

	@Override public String getSourceType() { return ProcessTraits.PROCESS_PARAMETER_VALUE; }
	@Override public String getTargetType() { return ProcessTraits.BPMN_ELEMENT; }
	@Override public String getRelationshipType() { return "SET_BY_ELEMENT"; }
	@Override public Relation.Multiplicity getSourceMultiplicity() { return Relation.Multiplicity.Many; }
	@Override public Relation.Multiplicity getTargetMultiplicity() { return Relation.Multiplicity.One; }
	@Override public int getCascadingDeleteFlag() { return Relation.NONE; }
	@Override public int getAutocreationFlag() { return Relation.NONE; }
	@Override public boolean isInternal() { return false; }
	@Override public PropagationDirection getPropagationDirection() { return PropagationDirection.None; }
	@Override public PropagationMode getReadPropagation() { return PropagationMode.Keep; }
	@Override public PropagationMode getWritePropagation() { return PropagationMode.Keep; }
	@Override public PropagationMode getDeletePropagation() { return PropagationMode.Keep; }
	@Override public PropagationMode getAccessControlPropagation() { return PropagationMode.Keep; }
	@Override public String getDeltaProperties() { return null; }
}
