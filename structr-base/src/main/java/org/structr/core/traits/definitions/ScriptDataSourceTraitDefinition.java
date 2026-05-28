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
import org.structr.core.GraphObject;
import org.structr.core.GraphObjectMap;
import org.structr.core.datasources.ChannelResult;
import org.structr.core.entity.DataSource;
import org.structr.core.entity.Relation;
import org.structr.core.entity.ScriptDataSource;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.StringProperty;
import org.structr.core.script.Scripting;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.TraitsInstance;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.datasource.DataSourceOperations;
import org.structr.core.traits.wrappers.ScriptDataSourceTraitWrapper;
import org.structr.schema.action.ActionContext;
import org.structr.web.datasource.FieldDefinition;
import org.structr.web.datasource.FunctionDataSource;

import java.util.*;

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

						final Object result = Scripting.evaluate(actionContext, null, "${" + valuesScript.trim() + "}", source.getName(), source.getUuid());
						if (result instanceof Iterable iterable) {

							final Iterable mapped = FunctionDataSource.map(iterable);

							return new PagingIterable<>("ScriptDataSource " + getName(), mapped, input.pageSize(), input.page());
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

					// fetch first object and look at the data
					final Map<String, FieldDefinition> output = new LinkedHashMap<>();
					final ChannelResult                result = provider.getResult(actionContext, ChannelInput.firstElement(), null);

					if (result != null) {

						final Object first = result.getFirst();
						if (first != null) {

							if (first instanceof GraphObjectMap map) {

								// look at actual fields
								for (final Object key : map.keySet()) {

									if (key instanceof Property property) {

										output.put(property.jsonName(), property);
									}
								}

							} else if (first instanceof GraphObject o && Traits.exists(o.getType())) {

								final Traits traits = Traits.of(o.getType());

								// transform input
								for (final PropertyKey key : traits.getPropertyKeysForView(PropertyView.All)) {

									// hide some internal properties
									if (!SchemaNodeTraitDefinition.PROPERTY_KEY_BLACKLIST_FOR_COMPONENTS.contains(key.jsonName())) {
										output.put(key.jsonName(), key.getFieldDefinition());
									}
								}

							} else {

								throw new IllegalArgumentException("Don't know how to handle data of type " + first.getClass());
							}
						}
					}

					return output;
				}

				@Override
				public String getDataType(final ActionContext actionContext, final DataSource provider) throws FrameworkException {
					return provider.as(ScriptDataSource.class).getDataType();
				}

				@Override
				public int getDimension(final DataSource provider) {
					// FIXME
					return 0;
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
