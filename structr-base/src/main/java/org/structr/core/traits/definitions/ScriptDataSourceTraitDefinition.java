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
package org.structr.core.traits.definitions;

import org.apache.commons.lang3.StringUtils;
import org.structr.api.util.PagingIterable;
import org.structr.api.util.ResultStream;
import org.structr.common.ChannelInput;
import org.structr.common.PropertyView;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.DataSource;
import org.structr.core.entity.Relation;
import org.structr.core.entity.ScriptDataSource;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.StringProperty;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.TraitsInstance;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.datasource.DataSourceOperations;
import org.structr.core.traits.wrappers.ScriptDataSourceTraitWrapper;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Actions;
import org.structr.web.datasource.FieldDefinition;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ScriptDataSourceTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String VALUES_SCRIPT_PROPERTY = "valuesScript";
	public static final String FIELDS_SCRIPT_PROPERTY = "fieldsScript";
	public static final String DATA_TYPE_PROPERTY     = "dataType";

	public ScriptDataSourceTraitDefinition() {
		super(StructrTraits.SCRIPT_DATA_SOURCE);
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			ScriptDataSource.class, (traits, node) -> new ScriptDataSourceTraitWrapper(traits, node)
		);
	}

	@Override
	public Map<Class, FrameworkMethod> getFrameworkMethods() {

		return Map.of(

			DataSourceOperations.class, new DataSourceOperations<NodeInterface>() {

				@Override
				public ResultStream<NodeInterface> getValues(final ActionContext actionContext, final DataSource provider, final ChannelInput input) throws FrameworkException {

					final ScriptDataSource<NodeInterface> source = provider.as(ScriptDataSource.class);
					final Map<String, Object> parameters         = new LinkedHashMap<>();
					final String valuesScript                    = source.getValuesScript();

					if (input != null) {
						parameters.put("input", input);
					}

					if (StringUtils.isNotBlank(valuesScript)) {

						final Object result = Actions.execute(actionContext.getSecurityContext(), source, valuesScript, parameters, "getValues", source.getUuid());
						if (result instanceof Iterable iterable) {

							return new PagingIterable<>("ScriptDataSource " + getName(), iterable, input.pageSize(), input.page());
						}

						if (result != null) {

							throw new FrameworkException(422, "ScriptDataSource.valuesScript produces non-iterable result of type " + result.getClass() + ", please make sure that your code returns a collection.");
						}

						throw new FrameworkException(422, "ScriptDataSource.valuesScript produces null result, please make sure that your code returns a collection.");
					}

					// no source => empty result
					return new PagingIterable<>("empty result", List.of());
				}

				@Override
				public Map<String, FieldDefinition> getFields(final ActionContext actionContext, final DataSource provider) throws FrameworkException {

					// if the data source has a data type set, return the fields of that data type
					final String dataType = provider.as(ScriptDataSource.class).getDataType();
					if (dataType != null) {

						final NodeInterface node = StructrApp.getInstance(actionContext.getSecurityContext()).nodeQuery(StructrTraits.SCHEMA_NODE).name(dataType).getFirst();
						if (node != null) {

							final DataSource schemaNode = node.as(DataSource.class);
							return schemaNode.getFields(actionContext);
						}
					}

					// no fields in script data source
					return Map.of();
				}

				@Override
				public String getDataType(final ActionContext actionContext, final DataSource provider) throws FrameworkException {
					return provider.as(ScriptDataSource.class).getDataType();
				}
			}
		);
	}

	@Override
	public Set<PropertyKey> createPropertyKeys(TraitsInstance traitsInstance) {

		final PropertyKey<String> valuesScriptProperty = new StringProperty(VALUES_SCRIPT_PROPERTY);
		final PropertyKey<String> fieldsScriptProperty = new StringProperty(FIELDS_SCRIPT_PROPERTY);
		final PropertyKey<String> dataTypeProperty     = new StringProperty(DATA_TYPE_PROPERTY);

		return newSet(
			valuesScriptProperty,
			fieldsScriptProperty,
			dataTypeProperty
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(

			PropertyView.Public,
			newSet(
				VALUES_SCRIPT_PROPERTY,
				FIELDS_SCRIPT_PROPERTY,
				DATA_TYPE_PROPERTY
			),

			PropertyView.Ui,
			newSet(
				VALUES_SCRIPT_PROPERTY,
				FIELDS_SCRIPT_PROPERTY,
				DATA_TYPE_PROPERTY
			)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
