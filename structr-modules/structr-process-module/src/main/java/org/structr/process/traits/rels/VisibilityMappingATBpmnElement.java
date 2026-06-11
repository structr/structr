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
import org.structr.core.traits.definitions.AbstractRelationshipTraitDefinition;
import org.structr.core.traits.definitions.RelationshipBaseTraitDefinition;
import org.structr.process.ProcessTraits;

/**
 * VisibilityMapping -[AT]-> BpmnElement.
 *
 * <p>The BPMN element (userTask or intermediateCatchEvent) the mapping is scoped
 * to. Required for task-scoped states ({@code task-available},
 * {@code task-reserved-by-me}, {@code task-reserved-by-other},
 * {@code task-completed}, {@code task-cancelled}); ignored for process-scoped
 * states.</p>
 */
public class VisibilityMappingATBpmnElement extends AbstractRelationshipTraitDefinition implements RelationshipBaseTraitDefinition {

	public VisibilityMappingATBpmnElement() {
		super(ProcessTraits.VISIBILITY_MAPPING_AT_BPMN_ELEMENT);
	}

	@Override public String getSourceType() { return ProcessTraits.VISIBILITY_MAPPING; }
	@Override public String getTargetType() { return ProcessTraits.BPMN_ELEMENT; }
	@Override public String getRelationshipType() { return "AT"; }
	@Override public Relation.Multiplicity getSourceMultiplicity() { return Relation.Multiplicity.Many; }
	@Override public Relation.Multiplicity getTargetMultiplicity() { return Relation.Multiplicity.One; }
	@Override public int getCascadingDeleteFlag() { return Relation.NONE; }
	@Override public int getAutocreationFlag() { return Relation.NONE; }
	@Override public boolean isInternal() { return false; }
}
