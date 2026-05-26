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

import org.apache.commons.lang3.StringUtils;
import org.structr.common.ChannelInput;
import org.structr.common.error.FrameworkException;
import org.structr.core.datasources.Channel;
import org.structr.core.entity.DataAdapter;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.Traits;
import org.structr.core.traits.wrappers.AbstractNodeTraitWrapper;
import org.structr.schema.action.Function;
import org.structr.web.common.RenderContext;
import org.structr.web.datasource.DataField;
import org.structr.web.entity.ComponentConfiguration;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.traits.definitions.ComponentConfigurationTraitDefinition;

import java.util.Arrays;
import java.util.List;
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

			dataSource = Channel.forName(this, getDataSourceName());
			if (dataSource != null) {

				// step 2: cache data source
				cache.put("_cached_data_source", dataSource);
			}
		}

		return dataSource;
	}

	@Override
	public String getDataSourceName() {

		// Process-bound mode: derive the data-source name from the bound
		// UserTask's `subjectType` so that process-side changes to the
		// subject propagate automatically into the rendered widget. The
		// `boundUserTask` rel and the BpmnElement's `subjectType` property
		// are both registered by the process module (cross-module), so
		// they are absent when the process module is not loaded. Read by
		// string key name to keep this wrapper module-agnostic.
		//
		// Channel.forName expects the `node:TypeName` syntax for
		// SchemaNode-backed channels (a bare type name throws "Unknown
		// data source type"); the standalone-widget datasource picker
		// stores values in that form, so we mirror it here.
		if (isProcessBound() && traits.hasKey(ComponentConfigurationTraitDefinition.BOUND_USER_TASK_PROPERTY)) {

			final NodeInterface userTask = wrappedObject.getProperty(traits.key(ComponentConfigurationTraitDefinition.BOUND_USER_TASK_PROPERTY));
			if (userTask != null && userTask.getTraits().hasKey("subjectType")) {

				final String subjectType = userTask.getProperty(userTask.getTraits().key("subjectType"));
				if (subjectType != null && !subjectType.isEmpty()) {
					return "node:" + subjectType;
				}
			}
		}

		// Standalone mode, or process-bound mode with no resolvable
		// subject yet (e.g. UserTask removed, process designer has not
		// declared a subjectType): fall through to the raw dataSource
		// string so existing widgets keep behaving as before.
		return wrappedObject.getProperty(traits.key(ComponentConfigurationTraitDefinition.DATA_SOURCE_PROPERTY));
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
	public String getBindingMode() {
		return wrappedObject.getProperty(traits.key(ComponentConfigurationTraitDefinition.BINDING_MODE_PROPERTY));
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

		final Channel channel = getDataSource();
		if (channel != null) {

			final String transform     = getTransform();
			final String sortKey       = channel.getSortKey();
			final String filterKey     = channel.getFilterKey();
			final String paginationKey = channel.getPaginationKey();
			final String[] sortStrings = renderContext.getRequestParameterValues(sortKey);
			final String filterString  = renderContext.getRequestParameter(filterKey);
			final String pageString    = renderContext.getRequestParameter(paginationKey);

			int page = 1;

			if (pageString != null) {

				page = Integer.valueOf(pageString);
			}

			final ChannelInput input = new ChannelInput(transform, filterString, sortStrings != null ? Arrays.asList(sortStrings) : null, getPageSize(), page);

			if (dataAdapter != null) {

				for (final DataField field : dataAdapter.augmentFields(renderContext, channel, false).values()) {

					if (field.isSearchable()) {
						input.searchableFields().add(field);
					}
				}
			}

			return input;
		}

		return null;

	}

	@Override
	public void setFieldSet(final String fieldSet) throws FrameworkException {
		wrappedObject.setProperty(traits.key(ComponentConfigurationTraitDefinition.FIELD_SET_PROPERTY), fieldSet);
	}

	@Override
	public void updateFieldSetForChildren() throws FrameworkException {

		final DOMNode domNode =  getComponent();
		if (domNode != null) {

			final String       fieldSet          = getFieldSet();
			final List<String> fields            = Function.splitAndTrim(fieldSet, ",");
			boolean            hasTreeChildField = false;

			for (final String field : fields) {

				hasTreeChildField |= field.startsWith("$");
			}

			if (!domNode.hasChildNodes()) {

				// remove tree child fields from field set
				setFieldSet(StringUtils.join(fields.stream().filter(field -> !field.startsWith("$")).toList(), ","));

			} else if (!hasTreeChildField) {

				// add tree child field to field set
				setFieldSet(fieldSet + ",$*");
			}
		}
	}
}
