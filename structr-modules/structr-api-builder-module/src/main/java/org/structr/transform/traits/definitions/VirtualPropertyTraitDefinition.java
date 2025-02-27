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
package org.structr.transform.traits.definitions;

import org.structr.common.PropertyView;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.RelationshipTraitFactory;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.transform.VirtualProperty;
import org.structr.transform.traits.wrappers.VirtualPropertyTraitWrapper;

import java.util.Map;
import java.util.Set;

/**
 *
 */
public class VirtualPropertyTraitDefinition extends AbstractNodeTraitDefinition {

	public VirtualPropertyTraitDefinition() {
		super("VirtualProperty");
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
			VirtualProperty.class, (traits, node) -> new VirtualPropertyTraitWrapper(traits, node)
		);
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final Property<NodeInterface> virtualType = new StartNode("virtualType", "VirtualTypevirtualPropertyVirtualProperty");
		final Property<Integer> position          = new IntProperty("position").indexed();
		final Property<String> sourceName         = new StringProperty("sourceName");
		final Property<String> targetName         = new StringProperty("targetName");
		final Property<String> inputFunction      = new StringProperty("inputFunction");
		final Property<String> outputFunction     = new StringProperty("outputFunction");

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
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public,
			newSet(
				"virtualType", "sourceName", "targetName", "inputFunction", "outputFunction", "position"
			),
			PropertyView.Ui,
			newSet(
				"virtualType", "sourceName", "targetName", "inputFunction", "outputFunction", "position"
			)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
