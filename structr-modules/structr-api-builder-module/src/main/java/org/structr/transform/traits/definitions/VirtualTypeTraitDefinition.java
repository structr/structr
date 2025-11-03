/*
 * Copyright (C) 2010-2025 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.transform.traits.definitions;

import org.structr.common.PropertyView;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.RelationshipTraitFactory;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.TraitsInstance;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.transform.VirtualType;
import org.structr.transform.traits.wrappers.VirtualTypeTraitWrapper;

import java.util.Map;
import java.util.Set;

/**
 *
 */
public class VirtualTypeTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String PROPERTIES_PROPERTY        = "properties";
	public static final String FILTER_EXPRESSION_PROPERTY = "filterExpression";
	public static final String SOURCE_TYPE_PROPERTY       = "sourceType";
	public static final String POSITION_PROPERTY          = "position";

	public VirtualTypeTraitDefinition() {
		super(StructrTraits.VIRTUAL_TYPE);
	}

	@Override
	public Map<Class, LifecycleMethod> createLifecycleMethods(TraitsInstance traitsInstance) {
		return Map.of();
	}

	@Override
	public Map<Class, FrameworkMethod> getFrameworkMethods() {
		return Map.of();
	}

	@Override
	public Map<Class, RelationshipTraitFactory> getRelationshipTraitFactories() {
		return Map.of();
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			VirtualType.class, (traits, node) -> new VirtualTypeTraitWrapper(traits, node)
		);
	}

	@Override
	public Set<PropertyKey> createPropertyKeys(TraitsInstance traitsInstance) {

		final Property<Iterable<NodeInterface>> propertiesProperty = new EndNodes(traitsInstance, PROPERTIES_PROPERTY, StructrTraits.VIRTUAL_TYPE_VIRTUAL_PROPERTY_VIRTUAL_PROPERTY);
		final Property<String> filterExpressionProperty            = new StringProperty(FILTER_EXPRESSION_PROPERTY);
		final Property<String> sourceTypeProperty                  = new StringProperty(SOURCE_TYPE_PROPERTY);
		final Property<Integer> positionProperty                   = new IntProperty(POSITION_PROPERTY).indexed();

		return Set.of(
			propertiesProperty,
			filterExpressionProperty,
			sourceTypeProperty,
			positionProperty
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public,
			newSet(
					FILTER_EXPRESSION_PROPERTY, SOURCE_TYPE_PROPERTY, POSITION_PROPERTY, PROPERTIES_PROPERTY
			),
			PropertyView.Ui,
			newSet(
					FILTER_EXPRESSION_PROPERTY, SOURCE_TYPE_PROPERTY, POSITION_PROPERTY, PROPERTIES_PROPERTY
			)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
