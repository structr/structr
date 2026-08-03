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
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.datasources.Channel;
import org.structr.core.datasources.ChannelDataSource;
import org.structr.core.datasources.ParentDataSource;
import org.structr.core.entity.DataAdapter;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.AbstractReadOnlyCollectionProperty;
import org.structr.core.property.ArrayProperty;
import org.structr.core.property.EndNodes;
import org.structr.core.property.HyperRelationProperty;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.StartNodes;
import org.structr.core.traits.Traits;
import org.structr.core.traits.wrappers.AbstractNodeTraitWrapper;
import org.structr.schema.action.ActionContext;
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
	public String getDataSourceName() {

		// Process-bound mode: the widget renders the ONE subject of the process
		// instance the page is showing -- i.e. the page's current details object
		// (`/<page>/<instance-uuid>`), not every node of the subject type. So the
		// data source is the `current` channel (a single object), NOT
		// `node:<subjectType>` (which resolves to a type-wide collection and makes
		// a single-object form fail with "data source returns a collection").
		//
		// The subject *type* is not the data source; it is the expected data type
		// (see getExpectedDataType) -- the `current` channel carries no type of its
		// own, so the form learns which fields to render from expectedDataType.
		//
		// `channel:current` is the same value the standalone datasource picker
		// stores for its "current" option (see _Widgets.dataSourcesSelector);
		// Channel.forName routes it to a ChannelDataSource of dimension 0.
		if (isProcessBound() && traits.hasKey(ComponentConfigurationTraitDefinition.BOUND_USER_TASK_PROPERTY)) {

			final NodeInterface userTask = wrappedObject.getProperty(traits.key(ComponentConfigurationTraitDefinition.BOUND_USER_TASK_PROPERTY));
			if (userTask != null) {
				return "channel:current";
			}
		}

		// Standalone mode, or process-bound mode with a broken binding (UserTask
		// removed): fall through to the raw dataSource string so existing widgets
		// and manual fallbacks keep behaving as before.
		return wrappedObject.getProperty(traits.key(ComponentConfigurationTraitDefinition.DATA_SOURCE_PROPERTY));
	}

	/**
	 * The process-level {@code subjectType} of the BpmnProcess that owns {@code node} (any node
	 * carrying a {@code process} relationship -- e.g. a BpmnElement / UserTask), or {@code null}.
	 *
	 * <p>The subject type is a process-level fact (one subject per instance), so callers hop
	 * {@code node -> process -> subjectType}. Read by string key so this stays agnostic of the
	 * process module's trait classes: those keys are absent when that module is not loaded, and
	 * the guards below short-circuit cleanly in that case.</p>
	 */
	public static String subjectTypeOfOwningProcess(final NodeInterface node) {

		if (node == null) return null;

		final Traits traits = node.getTraits();
		if (!traits.hasKey("process")) return null;

		final NodeInterface process = node.getProperty(traits.key("process"));
		if (process == null)           return null;

		final Traits processTraits = process.getTraits();
		if (!processTraits.hasKey("subjectType")) return null;

		return process.getProperty(processTraits.key("subjectType"));
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
	public String getExpectedDataType() {

		// Process-bound mode: the expected type IS the process's subjectType (the
		// data source is the `current` channel, which carries no type of its own --
		// see getDataSourceName). Derived at render time so changing the subject
		// type on the BPMN side updates every generated form's field set, matching
		// the data-source derivation above. Falls back to the stored property when
		// the process declares no subjectType yet (or the binding is broken).
		if (isProcessBound() && traits.hasKey(ComponentConfigurationTraitDefinition.BOUND_USER_TASK_PROPERTY)) {

			final NodeInterface userTask = wrappedObject.getProperty(traits.key(ComponentConfigurationTraitDefinition.BOUND_USER_TASK_PROPERTY));
			final String subjectType     = subjectTypeOfOwningProcess(userTask);
			if (subjectType != null && !subjectType.isEmpty()) {
				return subjectType;
			}
		}

		return wrappedObject.getProperty(traits.key(ComponentConfigurationTraitDefinition.EXPECTED_DATA_TYPE_PROPERTY));
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
	public void checkCompatibility() throws FrameworkException {

		// check if the configuration is valid (dimension of data source + transform <= dimension of component)
		final Channel<GraphObject> dataSource = getDataSource();
		if (dataSource != null) {

			final int dataDimension = dataSource.getDimension();
			final String transform  = getTransform();

			// Group 1: Data source + transform compatibility
			//   - list data source + any transform => error (cannot navigate a property on a collection)
			//   - single object data source + single object transform => ok
			//   - single object data source + list transform => ok
			if (dataDimension == 1 && transform != null) {

				throw new FrameworkException(422,
					"Transform '" + transform + "' cannot be applied to collection data source '" + dataSource.getChannelName() + "'. " +
					"Transform is only supported for single-object data sources."
				);
			}

			// Group 2: Component + transformed data source compatibility
			// Compute the effective dimension after the transform (if any).
			// For dim=0 + transform: check whether the transform property returns a list or a single object.
			final DOMNode component = getComponent();
			if (component != null) {

				final int componentDimension = component.getDimensions(true);
				int effectiveDimension       = dataDimension;

				if (componentDimension == -1) {
					throw new FrameworkException(422, "Component " + component.getName() + " has no dimension.");
				}

				if (transform != null && dataDimension == 0) {

					// Try to resolve the data type so we can inspect the transform property.
					// First check expectedDataType, then ask the data source itself.
					String dataType = getExpectedDataType();
					if (dataType == null) {

						try {
							dataType = dataSource.getDataType(new ActionContext(SecurityContext.getSuperUserInstance()));
						} catch (Exception ignore) {}
					}

					if (dataType != null && Traits.exists(dataType)) {

						final Traits traits = Traits.of(dataType);
						if (traits.hasKey(transform)) {

							final PropertyKey transformKey = traits.key(transform);
							final boolean isCollection     = transformKey.isCollection();

							effectiveDimension = isCollection ? 1 : 0;
						}
					}
				}

				if (effectiveDimension > componentDimension) {

					final StringBuilder error = new StringBuilder("Data source '");

					error.append(dataSource.getChannelName()).append("'");

					if (transform != null) {
						error.append(" with transform '").append(transform).append("'");
					}

					error.append(" is not compatible with component '").append(component.getName()).append("'. ");
					error.append("Component expects a single object but data source ");
					error.append("returns a collection.");

					throw new FrameworkException(422, error.toString());
				}
			}
		}
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

	@Override
	public Channel getDataSource() throws FrameworkException {

		final Map<String, Object> cache = getTemporaryStorage();
		Channel dataSource = (Channel) cache.get("_cached_data_source");

		if (dataSource == null) {

			dataSource = Channel.forName(getDataSourceName());
			if (dataSource != null) {

				// inject configuration so dynamic datasources can resolve their data type
				if (dataSource instanceof ChannelDataSource cds) {
					cds.setConfiguration(this);
				}

				if (dataSource instanceof ParentDataSource pds) {
					pds.setConfiguration(this);
				}

				cache.put("_cached_data_source", dataSource);
			}
		}

		return dataSource;
	}

	@Override
	public ChannelInput getChannelInput(final RenderContext renderContext) throws FrameworkException {

		final Channel channel = getDataSource();
		if (channel != null) {

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

			final ChannelInput input = new ChannelInput(filterString, sortStrings != null ? Arrays.asList(sortStrings) : null, getPageSize(), page);

			// augment fields for search
			final DataAdapter dataAdapter = getDataAdapter();
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
}
