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
package org.structr.web.traits.wrappers;

import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.Traits;
import org.structr.core.traits.wrappers.AbstractNodeTraitWrapper;
import org.structr.web.entity.StorageConfiguration;
import org.structr.web.entity.StorageConfigurationEntry;
import org.structr.web.traits.definitions.StorageConfigurationEntryTraitDefinition;

public class StorageConfigurationEntryTraitWrapper extends AbstractNodeTraitWrapper implements StorageConfigurationEntry {

	public StorageConfigurationEntryTraitWrapper(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	@Override
	public String getName() {
		return wrappedObject.getName();
	}

	public StorageConfiguration getConfiguration() {
		return wrappedObject.getProperty(traits.key(StorageConfigurationEntryTraitDefinition.CONFIGURATION_PROPERTY));
	}

	public void setName(final String name) throws FrameworkException {
		wrappedObject.setProperty(traits.key(StorageConfigurationEntryTraitDefinition.NAME_PROPERTY), name);
	}

	public String getValue() {
		return wrappedObject.getProperty(traits.key(StorageConfigurationEntryTraitDefinition.VALUE_PROPERTY));
	}

	public void setValue(final String value) throws FrameworkException {
		wrappedObject.setProperty(traits.key(StorageConfigurationEntryTraitDefinition.VALUE_PROPERTY), value);
	}
}
