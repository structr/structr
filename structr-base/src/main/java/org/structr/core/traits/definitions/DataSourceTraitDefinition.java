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

import org.slf4j.LoggerFactory;
import org.structr.api.util.Iterables;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.datasources.Channel;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.DataAdapter;
import org.structr.core.entity.DataSource;
import org.structr.core.entity.Relation;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.graphobject.Evaluate;
import org.structr.core.traits.wrappers.DataSourceTraitWrapper;
import org.structr.schema.action.ActionContext;
import org.structr.web.common.RenderContext;
import org.structr.web.entity.ComponentConfiguration;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.event.ActionMapping;

import java.util.Map;
import java.util.Set;

public class DataSourceTraitDefinition extends AbstractNodeTraitDefinition {

	public DataSourceTraitDefinition() {
		super(StructrTraits.DATA_SOURCE);
	}

	@Override
	public Map<Class, FrameworkMethod> getFrameworkMethods() {

		return Map.of(
			Evaluate.class,
			new Evaluate() {

				@Override
				public Object evaluate(final AbstractNode node, final ActionContext actionContext, final String key, final String defaultValue, final GraphObject contextObject, final int row, final int column) throws FrameworkException {

					final RenderContext renderContext = (RenderContext) actionContext;
					final DataSource dataSource       = node.as(DataSource.class);

					switch (key) {

						case "values":
							return dataSource.getValues(renderContext, null);

						case "dataType":
							return dataSource.getDataType(renderContext);

						case "currentValue":
							return getCurrentValue(renderContext, contextObject);
					}

					return getSuper().evaluate(node, actionContext, key, defaultValue, contextObject, row, column);
				}
			}
		);
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			DataSource.class, (traits, node) -> new DataSourceTraitWrapper(traits, node)
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			"dataSource", newSet(GraphObjectTraitDefinition.ID_PROPERTY, NodeInterfaceTraitDefinition.NAME_PROPERTY)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}

	// ----- private methods -----
	private GraphObject getCurrentValue(final RenderContext renderContext, final GraphObject contextObject) throws FrameworkException {

		if (contextObject != null) {

			DOMNode domNode = null;

			if (contextObject.is(StructrTraits.ACTION_MAPPING)) {

				domNode = Iterables.first(contextObject.as(ActionMapping.class).getTriggerElements());

			} else if (contextObject.is(StructrTraits.DOM_NODE)) {

				domNode = contextObject.as(DOMNode.class);
			}

			if (domNode != null) {

				final DOMNode component = domNode.as(DOMNode.class).getClosestComponent();
				if (component != null) {

					final ComponentConfiguration config = component.getComponentConfiguration();
					final DataAdapter dataAdapter       = config.getDataAdapter();
					final Channel sourceChannel         = config.getDataSource();
					final String role                   = domNode.getRoleForComponent();

					if ("subscriber".equals(role)) {

						final String channelName   = config.getSelectionChannel();
						final String selectedValue = renderContext.getChannelValue(channelName);

						if (selectedValue != null) {

							return StructrApp.getInstance(renderContext.getSecurityContext()).getNodeById(selectedValue);
						}
					}
				}
			}

			// next try: use the data key of the current data adapter to fetch the value
			final DataAdapter dataAdapter = renderContext.getCurrentAdapter();
			if (dataAdapter != null) {

				final String dataKey = dataAdapter.getDataKey();
				if (dataKey != null) {

					final GraphObject currentValue = renderContext.getDataNode(dataKey);
					if (currentValue != null) {

						return currentValue;
					}
				}
			}

			LoggerFactory.getLogger(DataSourceTraitDefinition.class).info("{}: unable to resolve keyword currentValue.", getName());
		}

		return null;
	}
}






















