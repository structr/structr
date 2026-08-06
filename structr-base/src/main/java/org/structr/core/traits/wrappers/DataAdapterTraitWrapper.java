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

import org.structr.common.error.FrameworkException;
import org.structr.core.datasources.Channel;
import org.structr.core.entity.DataAdapter;
import org.structr.core.entity.DataAdapterField;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.DataAdapterTraitDefinition;
import org.structr.web.common.RenderContext;
import org.structr.web.datasource.DataField;
import org.structr.web.datasource.FieldDefinition;
import org.structr.web.entity.ComponentConfiguration;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

public class DataAdapterTraitWrapper extends AbstractNodeTraitWrapper implements DataAdapter {

	private Map<String, DataAdapterField> fields;

	public DataAdapterTraitWrapper(final Traits traits, final NodeInterface node) {

		super(traits, node);
	}

	@Override
	public Map<String, DataField> augmentFields(final RenderContext renderContext, final Channel channel, boolean loadOptions) throws FrameworkException {

		final Map<String, DataField> augmentedFields = new TreeMap<>();

		if (channel != null) {

			final Map<String, FieldDefinition> sourceFields = channel.getFields(renderContext);
			final Map<String, DataAdapterField> augmentationFields = getFields();

			// augment fields from data source with fields from adapter
			for (final String name : sourceFields.keySet()) {

				final FieldDefinition sourceField = sourceFields.get(name);
				final DataAdapterField augmentationField = augmentationFields.get(name);

				augmentedFields.put(name, DataField.from(renderContext, this, name, sourceField, augmentationField, loadOptions));
			}

			// add adapter fields that are not present in the data source
			for (final String name : augmentationFields.keySet()) {

				// don't overwrite existing fields, should have been processed already
				if (!augmentedFields.containsKey(name)) {

					augmentedFields.put(name, DataField.from(renderContext, this, name, null, augmentationFields.get(name), loadOptions));
				}
			}
		}

		return augmentedFields;
	}

	@Override
	public Map<String, DataAdapterField> getFields() {

		if (fields == null) {

			fields = new LinkedHashMap<>();

			for (final NodeInterface field : (Iterable<NodeInterface>) wrappedObject.getProperty(traits.key(DataAdapterTraitDefinition.FIELDS_PROPERTY))) {

				final DataAdapterField adapterField = field.as(DataAdapterField.class);

				fields.put(adapterField.getName(), adapterField);
			}
		}

		return fields;
	}

	@Override
	public String getDataKey() {

		return wrappedObject.getProperty(traits.key(DataAdapterTraitDefinition.DATA_KEY_PROPERTY));
	}

	@Override
	public ComponentConfiguration getComponentConfiguration() {

		final NodeInterface node = wrappedObject.getProperty(traits.key(DataAdapterTraitDefinition.CONFIGURATION_PROPERTY));
		if (node != null) {

			return node.as(ComponentConfiguration.class);
		}

		return null;
	}
}