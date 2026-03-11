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

import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.DataSource;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.StartNodes;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.TraitsInstance;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.graphobject.Evaluate;
import org.structr.core.traits.wrappers.DataSourceTraitWrapper;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.EvaluationHints;
import org.structr.web.common.RenderContext;
import org.structr.web.entity.dom.DOMNode;

import java.util.Map;
import java.util.Set;

public class DataSourceTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String DOM_NODES_PROPERTY = "domNodes";

	public DataSourceTraitDefinition() {
		super(StructrTraits.DATA_SOURCE);
	}

	@Override
	public Map<Class, FrameworkMethod> getFrameworkMethods() {

		return Map.of(
			Evaluate.class,
			new Evaluate() {

				@Override
				public Object evaluate(final AbstractNode node, final ActionContext actionContext, final String key, final String defaultValue, final EvaluationHints hints, final int row, final int column) throws FrameworkException {

					final RenderContext renderContext = (RenderContext) actionContext;
					final DataSource dataSource       = node.as(DataSource.class);

					switch (key) {

						case "values":
							return dataSource.getValues(renderContext, null);

						case "dataType":
							return dataSource.getDataType(renderContext);

						case "currentValue":
							return getCurrentValue(renderContext);
					}

					return getSuper().evaluate(node, actionContext, key, defaultValue, hints, row, column);
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

	public Set<PropertyKey> createPropertyKeys(TraitsInstance traitsInstance) {

		final Property<Iterable<NodeInterface>> domNodesProperty = new StartNodes(traitsInstance, DOM_NODES_PROPERTY, StructrTraits.DOM_NODE_HAS_DATA_SOURCE).category(DOMNode.WIDGETS_CATEGORY);

		return newSet(
			domNodesProperty
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			"dataSource", newSet(GraphObjectTraitDefinition.ID_PROPERTY, NodeInterfaceTraitDefinition.NAME_PROPERTY, DOM_NODES_PROPERTY)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}

	// ----- private methods -----
	private GraphObject getCurrentValue(final RenderContext renderContext) {

		return null;
	}
}
