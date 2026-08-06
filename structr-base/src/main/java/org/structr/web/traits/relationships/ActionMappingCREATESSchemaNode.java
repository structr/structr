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
 * ActionMapping -[CREATES]-> SchemaNode.
 *
 * <p>Set when an ActionMapping's {@code action} is "create" and the {@code dataType}
 * string resolves to a SchemaNode (the type of object the action creates). The
 * relationship is the source of truth for the data-type binding; the {@code dataType}
 * string is kept in sync.</p>
 *
 * <p>Note: the same {@code dataType} string is also used by other actions (e.g. as a
 * static-method type qualifier when {@code method} is set). The relationship represents
 * the type binding regardless of which action consumes it.</p>
 *
 * <p>Auto-resolved by the {@code OnCreation} / {@code OnModification} lifecycle hooks on
 * ActionMapping. Cleared when the string clears.</p>
 */
public class ActionMappingCREATESSchemaNode extends AbstractRelationshipTraitDefinition implements RelationshipBaseTraitDefinition {

	public ActionMappingCREATESSchemaNode() {

		super(StructrTraits.ACTION_MAPPING_CREATES_SCHEMA_NODE);
	}

	@Override
	public String getSourceType() {

		return StructrTraits.ACTION_MAPPING;
	}

	@Override
	public String getTargetType() {

		return StructrTraits.SCHEMA_NODE;
	}

	@Override
	public String getRelationshipType() {

		return "CREATES";
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
