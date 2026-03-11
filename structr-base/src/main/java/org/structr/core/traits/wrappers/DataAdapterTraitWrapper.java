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
import org.structr.core.entity.DataAdapter;
import org.structr.core.entity.DataSource;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.DataAdapterTraitDefinition;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.EvaluationHints;
import org.structr.web.datasource.DataField;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DataAdapterTraitWrapper extends AbstractNodeTraitWrapper implements DataAdapter {

	private final Gson gson = new GsonBuilder().create();
	private Map<String, DataField> fields = null;
	private Map<String, List<String>> fieldSets = null;
	private DataSource dataSource = null;

	public DataAdapterTraitWrapper(final Traits traits, final NodeInterface node) {
		super(traits, node);
	}

	public void initialize(final DataSource dataSource) {
		this.dataSource = dataSource;
	}

	@Override
	public Map<String, DataField> getFields(final SecurityContext securityContext) throws FrameworkException {

		if (fields == null) {

			final String mappingSource = wrappedObject.getProperty(traits.key(DataAdapterTraitDefinition.MAPPING_PROPERTY));
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

			final String source = wrappedObject.getProperty(traits.key(DataAdapterTraitDefinition.FIELD_SETS_PROPERTY));
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

	@Override
	public String getDataKey() {
		return wrappedObject.getProperty(traits.key(DataAdapterTraitDefinition.DATA_KEY_PROPERTY));
	}

	@Override
	public Object evaluate(final ActionContext actionContext, final GraphObject entity, final String key) throws FrameworkException {

		final SecurityContext securityContext = actionContext.getSecurityContext();

		switch (key) {

			case "fields":
				return getFields(securityContext);

			case "dataKey":
				return getDataKey();
		}

		return this.evaluate(actionContext, key, null, new EvaluationHints(), 0, 0);
	}
}