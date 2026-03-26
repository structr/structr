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
package org.structr.core.traits.wrappers;

import com.google.gson.GsonBuilder;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.DataAdapterField;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.DataAdapterFieldTraitDefinition;

import java.util.Map;

public class DataAdapterFieldTraitWrapper extends AbstractNodeTraitWrapper implements DataAdapterField {

	public DataAdapterFieldTraitWrapper(final Traits traits, final NodeInterface node) {
		super(traits, node);
	}

	@Override
	public String getRenderTemplate() {
		return wrappedObject.getProperty(traits.key(DataAdapterFieldTraitDefinition.RENDER_TEMPLATE_PROPERTY));
	}

	@Override
	public String getEditTemplate() {
		return wrappedObject.getProperty(traits.key(DataAdapterFieldTraitDefinition.EDIT_TEMPLATE_PROPERTY));
	}

	@Override
	public String getValue() {
		return wrappedObject.getProperty(traits.key(DataAdapterFieldTraitDefinition.VALUE_PROPERTY));
	}

	@Override
	public String getDataType() {
		return wrappedObject.getProperty(traits.key(DataAdapterFieldTraitDefinition.DATA_TYPE_PROPERTY));
	}

	@Override
	public String getLabel() {
		return wrappedObject.getProperty(traits.key(DataAdapterFieldTraitDefinition.LABEL_PROPERTY));
	}

	@Override
	public String getSortKey() {
		return wrappedObject.getProperty(traits.key(DataAdapterFieldTraitDefinition.SORT_KEY_PROPERTY));
	}

	public Boolean isSearchable() {
		return wrappedObject.getProperty(traits.key(DataAdapterFieldTraitDefinition.IS_SEARCHABLE_PROPERTY));
	}

	public Integer getRows() {
		return wrappedObject.getProperty(traits.key(DataAdapterFieldTraitDefinition.ROWS_PROPERTY));
	}

	public Integer getColumns() {
		return wrappedObject.getProperty(traits.key(DataAdapterFieldTraitDefinition.COLUMNS_PROPERTY));
	}

	@Override
	public Map<String, Object> getConfig() {

		final String configSource = wrappedObject.getProperty(traits.key(DataAdapterFieldTraitDefinition.CONFIG_PROPERTY));
		if (configSource != null) {

			try {

				return new GsonBuilder().create().fromJson(configSource, Map.class);

			} catch (Throwable t) {}
		}

		return null;
	}

	@Override
	public void setConfig(final Map<String, Object> detailConfig) throws FrameworkException {
		wrappedObject.setProperty(traits.key(DataAdapterFieldTraitDefinition.CONFIG_PROPERTY), new GsonBuilder().create().toJson(detailConfig));
	}
}