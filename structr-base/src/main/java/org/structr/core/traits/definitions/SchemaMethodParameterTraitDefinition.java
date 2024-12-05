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
package org.structr.core.traits.definitions;

import org.structr.core.entity.Relation;
import org.structr.core.entity.SchemaMethodParameter;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.RelationshipTraitFactory;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.core.traits.wrappers.SchemaMethodParameterTraitWrapper;

import java.util.Map;
import java.util.Set;

/**
 * The typed parameter of a schema method.
 */
public class SchemaMethodParameterTraitDefinition extends AbstractTraitDefinition {

	private static final Property<NodeInterface> schemaMethod = new StartNode("schemaMethod", "SchemaMethodParameters");
	private static final Property<String> parameterType       = new StringProperty("parameterType");
	private static final Property<Integer> index              = new IntProperty("index").defaultValue(0);
	private static final Property<String> description         = new StringProperty("description");
	private static final Property<String> exampleValue        = new StringProperty("exampleValue");

	public SchemaMethodParameterTraitDefinition() {
		super("SchemaMethodParameter");
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

			SchemaMethodParameter.class, (traits, node) -> new SchemaMethodParameterTraitWrapper(traits, node)
		);
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		return Set.of(
			schemaMethod,
			parameterType,
			index,
			description,
			exampleValue
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
