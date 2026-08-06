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
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.datasources.Channel;
import org.structr.core.entity.DataAdapterField;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.DataAdapterFieldTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.web.entity.ComponentConfiguration;
import org.structr.web.entity.Widget;
import org.structr.web.traits.definitions.WidgetTraitDefinition;

import java.util.LinkedHashMap;
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

	@Override
	public String getEditModeCondition() {

		return wrappedObject.getProperty(traits.key(DataAdapterFieldTraitDefinition.EDIT_MODE_CONDITION_PROPERTY));
	}

	@Override
	public Boolean isSearchable() {

		return wrappedObject.getProperty(traits.key(DataAdapterFieldTraitDefinition.IS_SEARCHABLE_PROPERTY));
	}

	@Override
	public Integer getRows() {

		return wrappedObject.getProperty(traits.key(DataAdapterFieldTraitDefinition.ROWS_PROPERTY));
	}

	@Override
	public Integer getColumns() {

		return wrappedObject.getProperty(traits.key(DataAdapterFieldTraitDefinition.COLUMNS_PROPERTY));
	}

	@Override
	public String getColumnDataSource() {

		return wrappedObject.getProperty(traits.key(DataAdapterFieldTraitDefinition.COLUMN_DATA_SOURCE_PROPERTY));
	}

	@Override
	public String getColumnKey() {

		return wrappedObject.getProperty(traits.key(DataAdapterFieldTraitDefinition.COLUMN_KEY_PROPERTY));
	}

	@Override
	public void setIsSearchable(final boolean isSearchable) throws FrameworkException {

		wrappedObject.setProperty(traits.key(DataAdapterFieldTraitDefinition.IS_SEARCHABLE_PROPERTY), isSearchable);
	}

	@Override
	public Map<String, Object> getConfig() {

		final String configSource = wrappedObject.getProperty(traits.key(DataAdapterFieldTraitDefinition.CONFIG_PROPERTY));
		if (configSource != null) {

			try {

				return new GsonBuilder().create().fromJson(configSource, Map.class);

			} catch (Throwable t) {}
		}

		return new LinkedHashMap<>();
	}

	@Override
	public void setConfig(final Map<String, Object> detailConfig) throws FrameworkException {

		wrappedObject.setProperty(traits.key(DataAdapterFieldTraitDefinition.CONFIG_PROPERTY), new GsonBuilder().create().toJson(detailConfig));
	}

	@Override
	public void setRenderTemplate(final String renderTemplateName) throws FrameworkException {

		copyDefaultValueFromWidgetConfig(renderTemplateName);
		wrappedObject.setProperty(traits.key(DataAdapterFieldTraitDefinition.RENDER_TEMPLATE_PROPERTY), renderTemplateName);
	}

	@Override
	public void setEditTemplate(final String editTemplateName) throws FrameworkException {

		copyDefaultValueFromWidgetConfig(editTemplateName);
		wrappedObject.setProperty(traits.key(DataAdapterFieldTraitDefinition.EDIT_TEMPLATE_PROPERTY), editTemplateName);
	}

	// ----- private methods -----
	private void copyDefaultValueFromWidgetConfig(final String widgetName) throws FrameworkException{

		if (widgetName != null) {

			final Traits widgetTraits = Traits.of(StructrTraits.WIDGET);
			final App app             = StructrApp.getInstance();

			// find render template widget
			final NodeInterface widgetNode = app.nodeQuery(StructrTraits.WIDGET).key(widgetTraits.key(WidgetTraitDefinition.IS_RENDER_TEMPLATE_PROPERTY), true).key(widgetTraits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY), widgetName).getFirst();
			if (widgetNode != null) {

				final Widget widget = widgetNode.as(Widget.class);
				final String config = widget.getConfiguration();

				if (config != null) {

					// getConfig() creates an empty map if no configuration is set
					final Map<String, Object> dataFieldConfig = getConfig();

					try {

						final Map<String, Object> widgetConfig = new GsonBuilder().create().fromJson(config, Map.class);
						if (widgetConfig != null) {

							for (final String key : widgetConfig.keySet()) {

								final Object value = widgetConfig.get(key);
								if (value != null && value instanceof Map fieldConfig) {

									final Object defaultValue = fieldConfig.get("defaultValue");
									if (defaultValue != null) {

										// copy default value to data field config
										dataFieldConfig.put(key, defaultValue);
									}
								}
							}
						}

					} catch (Throwable t) {

						t.printStackTrace();
					}

					// write data field config back to database
					setConfig(dataFieldConfig);
				}
			}
		}
	}
}