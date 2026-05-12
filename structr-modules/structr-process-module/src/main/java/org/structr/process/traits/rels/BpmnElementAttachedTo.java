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
 * BpmnElement (boundary event) -[ATTACHED_TO]-> BpmnElement (host activity).
 * Encodes the BPMN 2.0 {@code attachedToRef} attribute on a boundary event:
 * each boundary is attached to exactly one host activity (task,
 * sub-process or call-activity), and a host can carry many boundaries
 * (interrupting + non-interrupting timer / message / error / signal /
 * escalation / cancel / compensate).
 *
 * <p>Modelled as BpmnElement on both ends rather than introducing a
 * BoundaryEvent sub-trait: every BPMN element shares the same trait
 * surface, the boundary identity is carried by {@code bpmnElementType
 * = "boundaryEvent"}, and a typed relationship would otherwise force a
 * sub-trait split that costs more than it saves.</p>
 *
 * <p>Cascading delete is OFF: removing the host doesn't take the
 * boundary with it (the importer / exporter wires it up; the schema
 * shouldn't fight authoring tools that delete and re-link).</p>
 */
public class BpmnElementAttachedTo extends AbstractRelationshipTraitDefinition implements RelationshipBaseTraitDefinition {

	public BpmnElementAttachedTo() { super(ProcessTraits.BPMN_ELEMENT_ATTACHED_TO); }

	@Override public String getSourceType() { return ProcessTraits.BPMN_ELEMENT; }
	@Override public String getTargetType() { return ProcessTraits.BPMN_ELEMENT; }
	@Override public String getRelationshipType() { return "ATTACHED_TO"; }
	@Override public Relation.Multiplicity getSourceMultiplicity() { return Relation.Multiplicity.Many; }
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
