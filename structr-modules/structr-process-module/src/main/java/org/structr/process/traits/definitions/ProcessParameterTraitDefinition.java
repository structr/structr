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
package org.structr.process.traits.definitions;

import org.structr.common.PropertyView;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.TraitsInstance;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;

import java.util.Map;
import java.util.Set;

/**
 * Definition-layer type: defines a named parameter that a process step
 * expects or produces. Linked to one or more BpmnElement nodes.
 *
 * At runtime, ProcessParameterValue nodes hold the actual values
 * for each ProcessInstance.
 */
public class ProcessParameterTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String PARAMETER_NAME_PROPERTY    = "parameterName";
	public static final String PARAMETER_TYPE_PROPERTY    = "parameterType";
	public static final String DIRECTION_PROPERTY         = "direction";
	public static final String REQUIRED_PROPERTY          = "required";
	public static final String ELEMENT_PROPERTY           = "element";

	// Parameter types
	public static final String TYPE_STRING  = "String";
	public static final String TYPE_BOOLEAN = "Boolean";
	public static final String TYPE_INTEGER = "Integer";
	public static final String TYPE_DOUBLE  = "Double";
	public static final String TYPE_DATE    = "Date";

	// Direction values
	public static final String DIRECTION_INPUT  = "input";
	public static final String DIRECTION_OUTPUT = "output";
	public static final String DIRECTION_BOTH   = "both";

	public ProcessParameterTraitDefinition() {
		super(StructrTraits.PROCESS_PARAMETER);
	}

	@Override
	public Set<PropertyKey> createPropertyKeys(final TraitsInstance traitsInstance) {

		final Property<String> parameterName   = new StringProperty(PARAMETER_NAME_PROPERTY).indexed();
		final Property<String> parameterType   = new StringProperty(PARAMETER_TYPE_PROPERTY);
		final Property<String> direction       = new StringProperty(DIRECTION_PROPERTY);
		final Property<Boolean> required       = new BooleanProperty(REQUIRED_PROPERTY);
		final Property<NodeInterface> element  = new StartNode(traitsInstance, ELEMENT_PROPERTY, StructrTraits.BPMN_ELEMENT_HAS_PARAMETER);

		return newSet(parameterName, parameterType, direction, required, element);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public, newSet(PARAMETER_NAME_PROPERTY, PARAMETER_TYPE_PROPERTY, DIRECTION_PROPERTY, REQUIRED_PROPERTY),
			PropertyView.Ui, newSet(PARAMETER_NAME_PROPERTY, PARAMETER_TYPE_PROPERTY, DIRECTION_PROPERTY, REQUIRED_PROPERTY, ELEMENT_PROPERTY)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
