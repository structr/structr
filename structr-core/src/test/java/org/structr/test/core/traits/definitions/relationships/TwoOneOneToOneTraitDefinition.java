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
package org.structr.test.core.traits.definitions.relationships;

import org.structr.core.entity.Relation;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.definitions.RelationshipTraitDefinition;

import java.util.Map;

public class TwoOneOneToOneTraitDefinition extends RelationshipTraitDefinition {

	public TwoOneOneToOneTraitDefinition() {
		super("TwoOneOneToOne");
	}

	@Override
	protected String getSourceType() {
		return "TestTwo";
	}

	@Override
	protected String getTargetType() {
		return "TestOne";
	}

	@Override
	protected String getRelationshipType() {
		return "IS_AT";
	}

	@Override
	protected Relation.Multiplicity getSourceMultiplicity() {
		return Relation.Multiplicity.One;
	}

	@Override
	protected Relation.Multiplicity getTargetMultiplicity() {
		return Relation.Multiplicity.One;
	}

	@Override
	protected int getCascadingDeleteFlag() {
		return Relation.NONE;
	}

	@Override
	protected int getAutocreationFlag() {
		return Relation.NONE;
	}

	@Override
	protected boolean isInternal() {
		return false;
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {
		return Map.of();
	}
}
