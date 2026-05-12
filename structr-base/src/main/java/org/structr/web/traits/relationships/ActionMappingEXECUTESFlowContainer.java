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
package org.structr.web.traits.relationships;

import org.structr.core.entity.Relation;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.definitions.AbstractRelationshipTraitDefinition;
import org.structr.core.traits.definitions.RelationshipBaseTraitDefinition;

/**
 * ActionMapping -[EXECUTES]-> FlowContainer.
 *
 * <p>Set when an ActionMapping's {@code action} is "flow" and the {@code flow} string
 * resolves to a FlowContainer node. The relationship is the source of truth for flow
 * bindings; the {@code flow} string is kept in sync.</p>
 *
 * <p>Auto-resolved by the {@code OnCreation} / {@code OnModification} lifecycle hooks on
 * ActionMapping. Cleared when the string clears.</p>
 */
public class ActionMappingEXECUTESFlowContainer extends AbstractRelationshipTraitDefinition implements RelationshipBaseTraitDefinition {

	public ActionMappingEXECUTESFlowContainer() {
		super(StructrTraits.ACTION_MAPPING_EXECUTES_FLOW_CONTAINER);
	}

	@Override
	public String getSourceType() {
		return StructrTraits.ACTION_MAPPING;
	}

	@Override
	public String getTargetType() {
		return StructrTraits.FLOW_CONTAINER;
	}

	@Override
	public String getRelationshipType() {
		return "EXECUTES";
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
