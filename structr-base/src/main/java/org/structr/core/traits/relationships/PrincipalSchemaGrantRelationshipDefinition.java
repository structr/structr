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

public class PrincipalSchemaGrantRelationshipDefinition extends AbstractRelationshipTraitDefinition implements RelationshipBaseTraitDefinition {

	public PrincipalSchemaGrantRelationshipDefinition() {
		super(StructrTraits.PRINCIPAL_SCHEMA_GRANT_RELATIONSHIP);
	}

	@Override
	public String getSourceType() {
		return StructrTraits.PRINCIPAL;
	}

	@Override
	public String getTargetType() {
		return StructrTraits.SCHEMA_GRANT;
	}

	@Override
	public String getRelationshipType() {
		return "SCHEMA_GRANT";
	}

	@Override
	public Relation.Multiplicity getSourceMultiplicity() {
		return One;
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
