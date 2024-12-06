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
package org.structr.web.entity.relationship;

import org.structr.api.graph.PropagationDirection;
import org.structr.api.graph.PropagationMode;
import org.structr.common.PermissionPropagation;
import org.structr.core.entity.OneToMany;
import org.structr.core.entity.Relation;
import org.structr.web.entity.StorageConfiguration;
import org.structr.web.entity.StorageConfigurationEntry;

public class StorageConfigurationCONFIG_ENTRYStorageConfigurationEntry extends OneToMany<StorageConfiguration, StorageConfigurationEntry> implements PermissionPropagation {

	@Override
	public Class<StorageConfiguration> getSourceType() {
		return StorageConfiguration.class;
	}

	@Override
	public Class<StorageConfigurationEntry> getTargetType() {
		return StorageConfigurationEntry.class;
	}

	@Override
	public String name() {
		return "CONFIG_ENTRY";
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

	@Override
	public String getDeltaProperties() {
		return "";
	}
}
