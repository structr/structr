/*
 * Copyright (C) 2010-2025 Structr GmbH
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
package org.structr.web.traits.definitions;

import org.structr.common.PropertyView;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.RelationshipTraitFactory;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.web.entity.path.PagePathParameter;
import org.structr.web.traits.wrappers.PagePathParameterTraitWrapper;

import java.util.Map;
import java.util.Set;

/**
 *
 */
public class PagePathParameterTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String PATH_PROPERTY          = "path";
	public static final String POSITION_PROPERTY      = "position";
	public static final String VALUE_TYPE_PROPERTY    = "valueType";
	public static final String DEFAULT_VALUE_PROPERTY = "defaultValue";
	public static final String IS_OPTIONAL_PROPERTY   = "isOptional";

	public PagePathParameterTraitDefinition() {
		super(StructrTraits.PAGE_PATH_PARAMETER);
	}

	@Override
	public Map<Class, LifecycleMethod> getLifecycleMethods() {
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
			PagePathParameter.class, (traits, node) -> new PagePathParameterTraitWrapper(traits, node)
		);
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final Property<NodeInterface> pathProperty  = new StartNode(PATH_PROPERTY, StructrTraits.PAGE_PATH_HAS_PARAMETER_PAGE_PATH_PARAMETER);
		final Property<Integer> positionProperty    = new IntProperty(POSITION_PROPERTY).indexed();
		final Property<String> valueTypeProperty    = new StringProperty(VALUE_TYPE_PROPERTY);
		final Property<String> defaultValueProperty = new StringProperty(DEFAULT_VALUE_PROPERTY);
		final Property<Boolean> isOptionalProperty  = new BooleanProperty(IS_OPTIONAL_PROPERTY);

		return Set.of(
			pathProperty,
			positionProperty,
			valueTypeProperty,
			defaultValueProperty,
			isOptionalProperty
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
				PropertyView.Public,
				newSet(POSITION_PROPERTY, VALUE_TYPE_PROPERTY, DEFAULT_VALUE_PROPERTY, IS_OPTIONAL_PROPERTY),

				PropertyView.Ui,
				newSet(POSITION_PROPERTY, VALUE_TYPE_PROPERTY, DEFAULT_VALUE_PROPERTY, IS_OPTIONAL_PROPERTY)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
