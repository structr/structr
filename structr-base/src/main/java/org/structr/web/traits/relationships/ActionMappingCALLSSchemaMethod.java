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
 * ActionMapping -[CALLS]-> SchemaMethod.
 *
 * <p>Set when an ActionMapping's {@code action} is "method" and the {@code method} string
 * resolves to a static SchemaMethod node (either a top-level user-defined method or a
 * method defined on the SchemaNode referenced by {@code dataType}). The relationship is
 * the source of truth for static method bindings; the {@code method} string is kept in
 * sync as a fallback for runtime-only / dynamic resolution paths (e.g. when the target
 * type is determined by {@code idExpression} at runtime).</p>
 *
 * <p>Auto-resolved by the {@code OnCreation} / {@code OnModification} lifecycle hooks on
 * ActionMapping when the {@code method} (and optionally {@code dataType}) string changes.
 * Cleared when the string clears.</p>
 */
public class ActionMappingCALLSSchemaMethod extends AbstractRelationshipTraitDefinition implements RelationshipBaseTraitDefinition {

	public ActionMappingCALLSSchemaMethod() {

		super(StructrTraits.ACTION_MAPPING_CALLS_SCHEMA_METHOD);
	}

	@Override
	public String getSourceType() {

		return StructrTraits.ACTION_MAPPING;
	}

	@Override
	public String getTargetType() {

		return StructrTraits.SCHEMA_METHOD;
	}

	@Override
	public String getRelationshipType() {

		return "CALLS";
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
