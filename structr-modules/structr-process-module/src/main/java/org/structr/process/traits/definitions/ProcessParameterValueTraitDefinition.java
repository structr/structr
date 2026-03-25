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

import java.util.Date;
import java.util.Map;
import java.util.Set;

/**
 * Instance-layer type: holds the actual value of a ProcessParameter
 * for a specific ProcessInstance, set at a specific step and time.
 *
 * Each step that modifies a parameter creates a new ProcessParameterValue
 * node, preserving the full audit trail of how values changed over time.
 *
 * The engine reads the most recent value (by setAt timestamp) for each
 * parameter name when evaluating conditions.
 */
public class ProcessParameterValueTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String STRING_VALUE_PROPERTY        = "stringValue";
	public static final String SET_AT_PROPERTY              = "setAt";
	public static final String PROCESS_INSTANCE_PROPERTY    = "processInstance";
	public static final String PARAMETER_PROPERTY           = "parameter";
	public static final String SET_BY_ELEMENT_PROPERTY      = "setByElement";

	public ProcessParameterValueTraitDefinition() {
		super(StructrTraits.PROCESS_PARAMETER_VALUE);
	}

	@Override
	public Set<PropertyKey> createPropertyKeys(final TraitsInstance traitsInstance) {

		final Property<String> stringValue         = new StringProperty(STRING_VALUE_PROPERTY).indexed();
		final Property<Date> setAt                 = new DateProperty(SET_AT_PROPERTY);
		final Property<NodeInterface> processInst  = new StartNode(traitsInstance, PROCESS_INSTANCE_PROPERTY, StructrTraits.PROCESS_INSTANCE_HAS_PARAMETER_VALUE);
		final Property<NodeInterface> parameter    = new EndNode(traitsInstance, PARAMETER_PROPERTY, StructrTraits.PROCESS_PARAMETER_VALUE_OF_PARAMETER);
		final Property<NodeInterface> setByElement = new EndNode(traitsInstance, SET_BY_ELEMENT_PROPERTY, StructrTraits.PROCESS_PARAMETER_VALUE_SET_BY_ELEMENT);

		return newSet(stringValue, setAt, processInst, parameter, setByElement);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public, newSet(STRING_VALUE_PROPERTY, SET_AT_PROPERTY, PARAMETER_PROPERTY),
			PropertyView.Ui, newSet(STRING_VALUE_PROPERTY, SET_AT_PROPERTY, PROCESS_INSTANCE_PROPERTY, PARAMETER_PROPERTY, SET_BY_ELEMENT_PROPERTY)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
