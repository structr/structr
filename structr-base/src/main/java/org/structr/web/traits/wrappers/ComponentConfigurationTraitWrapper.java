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
package org.structr.web.traits.wrappers;

import org.structr.common.error.FrameworkException;
import org.structr.core.entity.DataAdapter;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.Traits;
import org.structr.core.traits.wrappers.AbstractNodeTraitWrapper;
import org.structr.web.entity.ComponentConfiguration;
import org.structr.web.traits.definitions.ComponentConfigurationTraitDefinition;

public class ComponentConfigurationTraitWrapper extends AbstractNodeTraitWrapper implements ComponentConfiguration {

	public ComponentConfigurationTraitWrapper(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	@Override
	public void setDataSource(final NodeInterface dataSourceNode) throws FrameworkException {
		wrappedObject.setProperty(traits.key(ComponentConfigurationTraitDefinition.DATA_ADAPTER_PROPERTY), dataSourceNode);
	}

	@Override
	public DataAdapter getDataSource() {

		final NodeInterface node = wrappedObject.getProperty(traits.key(ComponentConfigurationTraitDefinition.DATA_ADAPTER_PROPERTY));
		if (node != null) {

			return node.as(DataAdapter.class);
		}

		return null;
	}

	@Override
	public Integer getColumns() {
		return wrappedObject.getProperty(traits.key(ComponentConfigurationTraitDefinition.COLUMNS_PROPERTY));
	}

	@Override
	public String getDisplayMode() {
		return wrappedObject.getProperty(traits.key(ComponentConfigurationTraitDefinition.DISPLAY_MODE_PROPERTY));
	}

	@Override
	public String getFieldSet() {
		return wrappedObject.getProperty(traits.key(ComponentConfigurationTraitDefinition.FIELD_SET_PROPERTY));
	}

	@Override
	public String getSaveMode() {
		return wrappedObject.getProperty(traits.key(ComponentConfigurationTraitDefinition.SAVE_MODE_PROPERTY));
	}

	@Override
	public String getRole() {
		return wrappedObject.getProperty(traits.key(ComponentConfigurationTraitDefinition.ROLE_PROPERTY));
	}

	@Override
	public String getReloadBehaviour() {
		return wrappedObject.getProperty(traits.key(ComponentConfigurationTraitDefinition.RELOAD_BEHAVIOUR_PROPERTY));
	}

	@Override
	public Boolean showLabels() {
		return wrappedObject.getProperty(traits.key(ComponentConfigurationTraitDefinition.SHOW_LABELS_PROPERTY));
	}
}
