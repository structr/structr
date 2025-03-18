/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.test.rest.traits.definitions.relationships;

import org.structr.core.entity.Relation;
import org.structr.core.traits.definitions.AbstractRelationshipTraitDefinition;
import org.structr.core.traits.definitions.RelationshipBaseTraitDefinition;

/**
 *
 *
 */
public class ThreeFiveOneToMany extends AbstractRelationshipTraitDefinition implements RelationshipBaseTraitDefinition {

	public ThreeFiveOneToMany() {
		super("ThreeFiveOneToMany");
	}

	@Override
	public String getSourceType() {
		return "TestThree";
	}

	@Override
	public String getTargetType() {
		return "TestFive";
	}

	@Override
	public String getRelationshipType() {
		return "ONE_TO_MANY";
	}

	@Override
	public Relation.Multiplicity getSourceMultiplicity() {
		return Relation.Multiplicity.One;
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
}
