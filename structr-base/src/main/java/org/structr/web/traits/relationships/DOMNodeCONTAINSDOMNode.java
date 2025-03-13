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
package org.structr.web.traits.relationships;

import org.structr.core.entity.Relation;
import org.structr.core.property.IntProperty;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.definitions.AbstractRelationshipTraitDefinition;
import org.structr.core.traits.definitions.RelationshipBaseTraitDefinition;

import java.util.Set;

public class DOMNodeCONTAINSDOMNode extends AbstractRelationshipTraitDefinition implements RelationshipBaseTraitDefinition {

	private static final Property<Integer> position  = new IntProperty("position");

	public DOMNodeCONTAINSDOMNode() {
		super(StructrTraits.DOM_NODE_CONTAINS_DOM_NODE);
	}

	@Override
	public String getSourceType() {
		return StructrTraits.DOM_NODE;
	}

	@Override
	public String getTargetType() {
		return StructrTraits.DOM_NODE;
	}

	@Override
	public String getRelationshipType() {
		return "CONTAINS";
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

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		return Set.of(
			position
		);
	}
}
