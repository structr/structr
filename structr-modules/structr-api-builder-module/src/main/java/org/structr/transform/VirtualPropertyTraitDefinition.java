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

import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractNode;
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
public class VirtualPropertyTraitDefinition extends AbstractTraitDefinition {

	public static final Property<NodeInterface> virtualType = new StartNode("virtualType", "VirtualTypevirtualPropertyVirtualProperty");
	public static final Property<Integer> position          = new IntProperty("position").indexed();
	public static final Property<String> sourceName         = new StringProperty("sourceName");
	public static final Property<String> targetName         = new StringProperty("targetName");
	public static final Property<String> inputFunction      = new StringProperty("inputFunction");
	public static final Property<String> outputFunction     = new StringProperty("outputFunction");

	public VirtualPropertyTraitDefinition() {
		super("VirtualProperty");
	}

	/*
	public static final View defaultView = new View(VirtualPropertyTraitDefinition.class, PropertyView.Public,
		virtualType, sourceName, targetName, inputFunction, outputFunction, position
	);

	public static final View uiView = new View(VirtualPropertyTraitDefinition.class, PropertyView.Ui,
		virtualType, sourceName, targetName, inputFunction, outputFunction, position
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
			VirtualProperty.class, (traits, node) -> new VirtualPropertyTraitWrapper(traits, node)
		);
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		return Set.of(
			virtualType,
			position,
			sourceName,
			targetName,
			inputFunction,
			outputFunction
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
