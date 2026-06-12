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

import org.structr.core.entity.Relation;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.definitions.AbstractRelationshipTraitDefinition;
import org.structr.core.traits.definitions.RelationshipBaseTraitDefinition;
import org.structr.process.ProcessTraits;

/**
 * ActionMapping -[TARGETS]-> BpmnElement.
 *
 * <p>Set when an ActionMapping's {@code action} is "control-process" and the action
 * is scoped to a specific BPMN element (a userTask for claim/complete/etc., or an
 * intermediateCatchEvent for signal). For process-level operations
 * (start, terminate, suspend, resume) the relationship is left null because the
 * controlsProcess relationship to {@code BpmnProcess} is sufficient.</p>
 *
 * <p>The relationship is the source of truth for the static binding: refactor-safe
 * across BPMN element renames. The runtime dispatcher uses the resolved target's
 * {@code bpmnId} for any element-level disambiguation.</p>
 */
public class ActionMappingTARGETSBpmnElement extends AbstractRelationshipTraitDefinition implements RelationshipBaseTraitDefinition {

	public ActionMappingTARGETSBpmnElement() {
		super(StructrTraits.ACTION_MAPPING_TARGETS_BPMN_ELEMENT);
	}

	@Override
	public String getSourceType() {
		return StructrTraits.ACTION_MAPPING;
	}

	@Override
	public String getTargetType() {
		return ProcessTraits.BPMN_ELEMENT;
	}

	@Override
	public String getRelationshipType() {
		return "TARGETS";
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
}
