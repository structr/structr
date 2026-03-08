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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.DataProvider;
import org.structr.core.entity.DataSource;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.DataSourceTraitDefinition;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.EvaluationHints;
import org.structr.web.common.RenderContext;
import org.structr.web.datasource.DataField;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DataSourceTraitWrapper extends AbstractNodeTraitWrapper implements DataSource {

	private final Gson gson = new GsonBuilder().create();
	private Map<String, DataField> fields = null;
	private Map<String, List<String>> fieldSets = null;

	public DataSourceTraitWrapper(final Traits traits, final NodeInterface node) {
		super(traits, node);
	}

	@Override
	public final Iterable<GraphObject> getValues(SecurityContext securityContext) throws FrameworkException {

		final DataProvider dataProvider = getDataProvider();

		return dataProvider.getValues(securityContext);
	}

	@Override
	public Map<String, DataField> getFields(final SecurityContext securityContext) throws FrameworkException {

		if (fields == null) {

			final String mappingSource = wrappedObject.getProperty(traits.key(DataSourceTraitDefinition.MAPPING_PROPERTY));
			if (mappingSource != null) {

				final Map<String, Object> data = gson.fromJson(mappingSource, Map.class);
				fields = new LinkedHashMap<>();

				for (final Map.Entry<String, Object> entry : data.entrySet()) {

					final String key = entry.getKey();

					fields.put(key, DataField.fromMap(key, (Map) entry.getValue()));
				}
			}
		}

		return fields;
	}

	@Override
	public Map<String, List<String>> getFieldSets(final SecurityContext securityContext) throws FrameworkException {

		if (fieldSets == null) {

			final String source = wrappedObject.getProperty(traits.key(DataSourceTraitDefinition.FIELD_SETS_PROPERTY));
			if (source != null) {

				fieldSets = gson.fromJson(source, Map.class);
			}
		}

		return fieldSets;
	}

	@Override
	public List<String> getFieldSet(final SecurityContext securityContext, final String name) throws FrameworkException {

		final Map<String, List<String>> fieldSets = getFieldSets(securityContext);
		if (fieldSets != null) {

			final List<String> fieldSet = fieldSets.get(name);
			if (fieldSet != null) {

				return fieldSet;
			}
		}

		return List.of();
	}

	/**
	 * Returns the string value (UUID) of the context value that is currently
	 * selected by the controller of this data source.
	 *
	 * @param actionContext
	 * @return
	 * @throws FrameworkException
	 */
	@Override
	public String getSelectedId(final ActionContext actionContext) throws FrameworkException {

		final String channel = getChannel();
		if (channel != null && actionContext instanceof RenderContext renderContext) {

			return renderContext.getChannelValue(channel);
		}

		return null;
	}

	/**
	 * Returns the context value that is currently selected by the controller
	 * of this data source. The value can be selected via the channel
	 * mechanism.
	 *
	 * @param actionContext
	 * @return
	 * @throws FrameworkException
	 */
	@Override
	public Object getSelectedValue(final ActionContext actionContext) throws FrameworkException {

		final String selectedId = getSelectedId(actionContext);
		if (selectedId != null) {

			return StructrApp.getInstance(actionContext.getSecurityContext()).getNodeById(selectedId);
		}

		return null;
	}

	/**
	 * Returns the context value that is currently associated with
	 * the data key of this data source.
	 *
	 * @param actionContext
	 * @return
	 * @throws FrameworkException
	 */
	@Override
	public Object getCurrentValue(final ActionContext actionContext) throws FrameworkException {

		if (actionContext instanceof RenderContext renderContext) {

			final String dataKey = getDataKey();
			if (dataKey != null) {

				// allow fallback if no data node is set
				final GraphObject dataNode = renderContext.getDataNode(dataKey);
				if (dataNode != null) {

					return dataNode;
				}
			}
		}

		return getSelectedValue(actionContext);
	}

	@Override
	public String getDataType(final SecurityContext securityContext) throws FrameworkException {

		final DataProvider dataProvider = getDataProvider();
		if (dataProvider != null) {

			return dataProvider.getDataType(securityContext);
		}

		return null;
	}

	@Override
	public String getChannel() {
		return wrappedObject.getProperty(traits.key(DataSourceTraitDefinition.CHANNEL_PROPERTY));
	}

	@Override
	public String getDataKey() {
		return wrappedObject.getProperty(traits.key(DataSourceTraitDefinition.DATA_KEY_PROPERTY));
	}

	@Override
	public DataProvider getDataProvider() {

		final NodeInterface provider = wrappedObject.getProperty(traits.key(DataSourceTraitDefinition.PROVIDER_PROPERTY));
		if (provider != null) {

			return provider.as(DataProvider.class);
		}

		return null;
	}

	@Override
	public Object evaluate(final ActionContext actionContext, final String key) throws FrameworkException {

		final SecurityContext securityContext = actionContext.getSecurityContext();

		switch (key) {

			case "fields":
				return getFields(securityContext);

			case "values":
				return getValues(securityContext);

			case "dataKey":
				return getDataKey();

			case "dataType":
				return getDataType(securityContext);

			case "selectedId":
				return getSelectedId(actionContext);

			case "selectedValue":
				return getSelectedValue(actionContext);

			case "currentValue":
				return getCurrentValue(actionContext);
		}

		return this.as(NodeInterface.class).evaluate(actionContext, key, null, new EvaluationHints(), 0, 0);
	}
}