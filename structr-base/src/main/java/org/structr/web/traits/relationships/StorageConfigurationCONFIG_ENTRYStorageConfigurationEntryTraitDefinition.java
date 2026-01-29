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

import org.structr.api.graph.PropagationDirection;
import org.structr.api.graph.PropagationMode;
import org.structr.core.entity.Relation;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.definitions.AbstractRelationshipTraitDefinition;
import org.structr.core.traits.definitions.RelationshipBaseTraitDefinition;

public class StorageConfigurationCONFIG_ENTRYStorageConfigurationEntryTraitDefinition extends AbstractRelationshipTraitDefinition implements RelationshipBaseTraitDefinition {

	public StorageConfigurationCONFIG_ENTRYStorageConfigurationEntryTraitDefinition() {
		super(StructrTraits.STORAGE_CONFIGURATION_CONFIG_ENTRY_STORAGE_CONFIGURATION_ENTRY);
	}

	@Override
	public String getSourceType() {
		return StructrTraits.STORAGE_CONFIGURATION;
	}

	@Override
	public String getTargetType() {
		return StructrTraits.STORAGE_CONFIGURATION_ENTRY;
	}

	@Override
	public String getRelationshipType() {
		return "CONFIG_ENTRY";
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
		return Relation.SOURCE_TO_TARGET;
	}

	@Override
	public int getAutocreationFlag() {
		return Relation.SOURCE_TO_TARGET;
	}

	@Override
	public boolean isInternal() {
		return false;
	}

	@Override
	public PropagationDirection getPropagationDirection() {
		return PropagationDirection.Both;
	}

	@Override
	public PropagationMode getReadPropagation() {
		return PropagationMode.Add;
	}

	@Override
	public PropagationMode getWritePropagation() {
		return PropagationMode.Remove;
	}

	@Override
	public PropagationMode getDeletePropagation() {
		return PropagationMode.Remove;
	}

	@Override
	public PropagationMode getAccessControlPropagation() {
		return PropagationMode.Remove;
	}
}
