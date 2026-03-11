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

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.DataAdapter;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.TraitsInstance;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.graphobject.Evaluate;
import org.structr.core.traits.wrappers.DataAdapterTraitWrapper;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.EvaluationHints;
import org.structr.web.common.RenderContext;
import org.structr.web.entity.dom.DOMNode;

import java.util.Map;
import java.util.Set;

public class DataAdapterTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String DOM_NODES_PROPERTY  = "domNodes";
	public static final String MAPPING_PROPERTY    = "mapping";
	public static final String FIELD_SETS_PROPERTY = "fieldSets";
	public static final String DATA_KEY_PROPERTY   = "dataKey";

	public DataAdapterTraitDefinition() {
		super(StructrTraits.DATA_ADAPTER);
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			DataAdapter.class, (traits, node) -> new DataAdapterTraitWrapper(traits, node)
		);
	}

	@Override
	public Map<Class, FrameworkMethod> getFrameworkMethods() {

		return Map.of(
			Evaluate.class,
			new Evaluate() {
				@Override
				public Object evaluate(final AbstractNode node, final ActionContext actionContext, final String key, final String defaultValue, final EvaluationHints hints, final int row, final int column) throws FrameworkException {

					final RenderContext renderContext     = (RenderContext) actionContext;
					final SecurityContext securityContext = renderContext.getSecurityContext();
					final DataAdapter dataAdapter         = node.as(DataAdapter.class);

					switch (key) {

						case "fields":
							return dataAdapter.getFields(securityContext);

						case "dataKey":
							return dataAdapter.getDataKey();
					}

					return getSuper().evaluate(node, actionContext, key, defaultValue, hints, row, column);
				}
			}
		);
	}

	@Override
	public Set<PropertyKey> createPropertyKeys(TraitsInstance traitsInstance) {

		final Property<Iterable<NodeInterface>> domNodesProperty = new StartNodes(traitsInstance, DOM_NODES_PROPERTY, StructrTraits.COMPONENT_CONFIGURATION_HAS_DATA_ADAPTER).category(DOMNode.WIDGETS_CATEGORY);
		final Property<String> mappingProperty                   = new StringProperty(MAPPING_PROPERTY).category(DOMNode.WIDGETS_CATEGORY);
		final Property<String> fieldSetsProperty                 = new StringProperty(FIELD_SETS_PROPERTY).category(DOMNode.WIDGETS_CATEGORY);
		final Property<String> dataKeyProperty                   = new StringProperty(DATA_KEY_PROPERTY).category(DOMNode.WIDGETS_CATEGORY);

		return newSet(
			domNodesProperty,
			mappingProperty,
			fieldSetsProperty,
			dataKeyProperty
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			"adapter", newSet(
				GraphObjectTraitDefinition.ID_PROPERTY,
				NodeInterfaceTraitDefinition.NAME_PROPERTY,
				MAPPING_PROPERTY,
				FIELD_SETS_PROPERTY,
				DATA_KEY_PROPERTY
			)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
