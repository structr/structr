/*
 * Copyright (C) 2010-2024 Structr GmbH
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

	/*
	public static final View defaultView = new View(PagePathParameter.class, PropertyView.Public,
		positionProperty, valueTypeProperty, defaultValueProperty, isOptionalProperty
	);

	public static final View uiView = new View(PagePathParameter.class, PropertyView.Ui,
		positionProperty, valueTypeProperty, defaultValueProperty, isOptionalProperty
	);
	*/

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

		final Property<NodeInterface> pathProperty  = new StartNode("path", "PagePathHAS_PARAMETERPagePathParameter");
		final Property<Integer> positionProperty    = new IntProperty("position").indexed();
		final Property<String> valueTypeProperty    = new StringProperty("valueType");
		final Property<String> defaultValueProperty = new StringProperty("defaultValue");
		final Property<Boolean> isOptionalProperty  = new BooleanProperty("isOptional");

		return Set.of(
			pathProperty,
			positionProperty,
			valueTypeProperty,
			defaultValueProperty,
			isOptionalProperty
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
