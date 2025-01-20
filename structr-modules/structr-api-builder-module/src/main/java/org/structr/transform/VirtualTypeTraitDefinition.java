/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.transform;

import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.RelationshipTraitFactory;
import org.structr.core.traits.definitions.AbstractTraitDefinition;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.LifecycleMethod;

import java.util.Map;
import java.util.Set;

/**
 *
 */
public class VirtualTypeTraitDefinition extends AbstractNodeTraitDefinition {

	private static final Property<Iterable<NodeInterface>> propertiesProperty = new EndNodes("properties", "VirtualTypevirtualPropertyVirtualProperty");
	private static final Property<String> filterExpressionProperty            = new StringProperty("filterExpression");
	private static final Property<String> sourceTypeProperty                  = new StringProperty("sourceType");
	private static final Property<Integer> positionProperty                   = new IntProperty("position").indexed();

	public VirtualTypeTraitDefinition() {
		super("VirtualType");
	}

	/*
	public static final View defaultView = new View(VirtualType.class, PropertyView.Public,
		name, filterExpressionProperty, sourceTypeProperty, positionProperty, propertiesProperty
	);

	public static final View uiView = new View(VirtualType.class, PropertyView.Ui,
		filterExpressionProperty, sourceTypeProperty, positionProperty, propertiesProperty
	);
	*/

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
			VirtualType.class, (traits, node) -> new VirtualTypeTraitWrapper(traits, node)
		);
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		return Set.of(
			propertiesProperty,
			filterExpressionProperty,
			sourceTypeProperty,
			positionProperty
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
