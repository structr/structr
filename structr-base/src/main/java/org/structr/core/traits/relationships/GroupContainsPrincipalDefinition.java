/*
 * Copyright (C) 2010-2025 Structr GmbH
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
package org.structr.core.traits.relationships;

import org.structr.core.entity.Relation;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.definitions.AbstractRelationshipTraitDefinition;
import org.structr.core.traits.definitions.RelationshipBaseTraitDefinition;

import java.util.Map;

import static org.structr.core.entity.Relation.Multiplicity.Many;

public class GroupContainsPrincipalDefinition extends AbstractRelationshipTraitDefinition implements RelationshipBaseTraitDefinition {

	public GroupContainsPrincipalDefinition() {
		super(StructrTraits.GROUP_CONTAINS_PRINCIPAL);
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {
		return Map.of();
	}

	@Override
	public String getSourceType() {
		return StructrTraits.GROUP;
	}

	@Override
	public String getTargetType() {
		return StructrTraits.PRINCIPAL;
	}

	@Override
	public String getRelationshipType() {
		return "CONTAINS";
	}

	@Override
	public Relation.Multiplicity getSourceMultiplicity() {
		return Many;
	}

	@Override
	public Relation.Multiplicity getTargetMultiplicity() {
		return Many;
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
		return true;
	}
}
