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
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.definitions.AbstractRelationshipTraitDefinition;
import org.structr.core.traits.definitions.RelationshipBaseTraitDefinition;

import static org.structr.core.entity.Relation.Multiplicity.Many;
import static org.structr.core.entity.Relation.Multiplicity.One;

public class SchemaRelationshipTargetNodeDefinition extends AbstractRelationshipTraitDefinition implements RelationshipBaseTraitDefinition {

	public SchemaRelationshipTargetNodeDefinition() {
		super(StructrTraits.SCHEMA_RELATIONSHIP_TARGET_NODE);
	}

	@Override
	public String getSourceType() {
		return StructrTraits.SCHEMA_RELATIONSHIP_NODE;
	}

	@Override
	public String getTargetType() {
		return StructrTraits.SCHEMA_NODE;
	}

	@Override
	public String getRelationshipType() {
		return "IS_RELATED_TO";
	}

	@Override
	public Relation.Multiplicity getSourceMultiplicity() {
		return Many;
	}

	@Override
	public Relation.Multiplicity getTargetMultiplicity() {
		return One;
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
		return true;
	}
}
