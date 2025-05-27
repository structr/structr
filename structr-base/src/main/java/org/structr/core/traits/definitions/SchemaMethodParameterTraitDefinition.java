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
package org.structr.core.traits.definitions;

import org.structr.common.PropertyView;
import org.structr.core.entity.Relation;
import org.structr.core.entity.SchemaMethodParameter;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.wrappers.SchemaMethodParameterTraitWrapper;

import java.util.Map;
import java.util.Set;

/**
 * The typed parameter of a schema method.
 */
public class SchemaMethodParameterTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String SCHEMA_METHOD_PROPERTY  = "schemaMethod";
	public static final String PARAMETER_TYPE_PROPERTY = "parameterType";
	public static final String INDEX_PROPERTY          = "index";
	public static final String DESCRIPTION_PROPERTY    = "description";
	public static final String EXAMPLE_VALUE_PROPERTY  = "exampleValue";

	public SchemaMethodParameterTraitDefinition() {
		super(StructrTraits.SCHEMA_METHOD_PARAMETER);
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			SchemaMethodParameter.class, (traits, node) -> new SchemaMethodParameterTraitWrapper(traits, node)
		);
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final Property<NodeInterface> schemaMethod = new StartNode(SCHEMA_METHOD_PROPERTY, StructrTraits.SCHEMA_METHOD_PARAMETERS);
		final Property<String> parameterType       = new StringProperty(PARAMETER_TYPE_PROPERTY);
		final Property<Integer> index              = new IntProperty(INDEX_PROPERTY).defaultValue(0);
		final Property<String> description         = new StringProperty(DESCRIPTION_PROPERTY);
		final Property<String> exampleValue        = new StringProperty(EXAMPLE_VALUE_PROPERTY);

		return newSet(
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

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(

				PropertyView.Public,
				newSet(
						GraphObjectTraitDefinition.ID_PROPERTY, GraphObjectTraitDefinition.TYPE_PROPERTY, NodeInterfaceTraitDefinition.NAME_PROPERTY,
						SCHEMA_METHOD_PROPERTY, PARAMETER_TYPE_PROPERTY, INDEX_PROPERTY, DESCRIPTION_PROPERTY, EXAMPLE_VALUE_PROPERTY
				),

				PropertyView.Ui,
				newSet(
						GraphObjectTraitDefinition.ID_PROPERTY, GraphObjectTraitDefinition.TYPE_PROPERTY, NodeInterfaceTraitDefinition.NAME_PROPERTY,
						SCHEMA_METHOD_PROPERTY, PARAMETER_TYPE_PROPERTY, INDEX_PROPERTY, DESCRIPTION_PROPERTY, EXAMPLE_VALUE_PROPERTY
				),

				PropertyView.Schema,
				newSet(
						GraphObjectTraitDefinition.ID_PROPERTY, GraphObjectTraitDefinition.TYPE_PROPERTY, NodeInterfaceTraitDefinition.NAME_PROPERTY,
						SCHEMA_METHOD_PROPERTY, PARAMETER_TYPE_PROPERTY, INDEX_PROPERTY, DESCRIPTION_PROPERTY, EXAMPLE_VALUE_PROPERTY
				)
		);
	}
}
