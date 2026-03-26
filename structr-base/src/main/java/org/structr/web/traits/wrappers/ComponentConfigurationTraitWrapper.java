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

import org.structr.common.ChannelInput;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.datasources.Channel;
import org.structr.core.datasources.ChannelDataSource;
import org.structr.core.entity.DataAdapter;
import org.structr.core.entity.DataSource;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.wrappers.AbstractNodeTraitWrapper;
import org.structr.web.common.RenderContext;
import org.structr.web.datasource.DataField;
import org.structr.web.entity.ComponentConfiguration;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.traits.definitions.ComponentConfigurationTraitDefinition;

import java.util.Map;

public class ComponentConfigurationTraitWrapper extends AbstractNodeTraitWrapper implements ComponentConfiguration {

	public ComponentConfigurationTraitWrapper(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	@Override
	public DOMNode getComponent() {

		final NodeInterface node = wrappedObject.getProperty(traits.key(ComponentConfigurationTraitDefinition.DOM_NODE_PROPERTY));
		if (node != null) {

			return node.as(DOMNode.class);
		}

		return null;

	}

	@Override
	public DataAdapter getDataAdapter() {

		final NodeInterface node = wrappedObject.getProperty(traits.key(ComponentConfigurationTraitDefinition.DATA_ADAPTER_PROPERTY));
		if (node != null) {

			return node.as(DataAdapter.class);
		}

		return null;
	}

	@Override
	public Channel getDataSource() throws FrameworkException {

		final Map<String, Object> cache = getTemporaryStorage();
		Channel dataSource = (Channel) cache.get("_cached_data_source");

		if (dataSource == null) {

			final String dataSourceName = wrappedObject.getProperty(traits.key(ComponentConfigurationTraitDefinition.DATA_SOURCE_PROPERTY));
			if (dataSourceName != null) {

				if (dataSourceName.contains(":")) {

					final String[] parts = dataSourceName.split(":");
					final String type = parts[0];
					final String name = parts[1];

					switch (type) {

						case "node":
							final NodeInterface node = StructrApp.getInstance(getSecurityContext()).nodeQuery(StructrTraits.DATA_SOURCE).name(name).getFirst();
							if (node != null) {

								dataSource = node.as(DataSource.class);
							}
							break;

						case "channel":
							dataSource = new ChannelDataSource(this, name);

					}
				}
			}

			if (dataSource != null) {

				// step 2: cache data source
				cache.put("_cached_data_source", dataSource);
			}
		}

		return dataSource;
	}

	@Override
	public String getSelectionChannel() throws FrameworkException {
		return wrappedObject.getProperty(traits.key(ComponentConfigurationTraitDefinition.SELECTION_CHANNEL_PROPERTY));
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

	@Override
	public String getTransform() {
		return wrappedObject.getProperty(traits.key(ComponentConfigurationTraitDefinition.TRANSFORM_PROPERTY));
	}

	@Override
	public int getPageSize() {

		final Integer value = wrappedObject.getProperty(traits.key(ComponentConfigurationTraitDefinition.PAGE_SIZE_PROPERTY));
		if (value != null) {

			return value.intValue();
		}

		return 10;
	}

	@Override
	public int getPaginationWindowSize() {

		final Integer value = wrappedObject.getProperty(traits.key(ComponentConfigurationTraitDefinition.PAGINATION_WINDOW_SIZE_PROPERTY));
		if (value != null) {

			return value.intValue();
		}

		return 5;
	}

	@Override
	public ChannelInput getChannelInput(final RenderContext renderContext, final DataAdapter dataAdapter) throws FrameworkException {

		final Channel channel      = getDataSource();
		final String sortKey       = channel.getSortKey();
		final String filterKey     = channel.getFilterKey();
		final String paginationKey = channel.getPaginationKey();
		final String[] sortStrings = renderContext.getRequestParameterValues(sortKey);
		final String filterString  = renderContext.getRequestParameter(filterKey);
		final String pageString    = renderContext.getRequestParameter(paginationKey);

		int pageSize = getPageSize();
		int page     = 1;

		if (pageString != null) {

			page = Integer.valueOf(pageString);
		}

		final ChannelInput input = new ChannelInput(getTransform(), filterString, sortStrings, pageSize, page);

		for (final DataField field : dataAdapter.augmentFields(renderContext, channel).values()) {

			if (field.isSearchable()) {
				input.searchableFields().add(field);
			}
		}

		return input;
	}

	@Override
	public void setFieldSet(final String fieldSet) throws FrameworkException {
		wrappedObject.setProperty(traits.key(ComponentConfigurationTraitDefinition.FIELD_SET_PROPERTY), fieldSet);
	}
}
