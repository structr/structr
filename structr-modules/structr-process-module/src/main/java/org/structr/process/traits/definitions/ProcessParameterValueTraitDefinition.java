/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.process.traits.definitions;

import org.structr.common.PropertyView;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.core.traits.TraitsInstance;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.process.ProcessTraits;

import java.util.Date;
import java.util.Map;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.process.entity.ProcessParameterValue;
import org.structr.process.traits.wrappers.ProcessParameterValueTraitWrapper;
import java.util.Set;

/**
 * Self-describing record of a single process-parameter write. Each step that
 * sets a parameter creates a new {@code ProcessParameterValue} node, preserving
 * the full audit trail of how values changed over time.
 *
 * <p>The engine reads the most recent value (by {@code setAt} timestamp) for
 * each parameter name when evaluating gateway conditions or building listener
 * argument maps.</p>
 *
 * <p>{@code parameterType} is optional. When set, {@link
 * org.structr.process.engine.ProcessEngine}'s value-loading converts
 * {@code stringValue} back to a typed Java value (Boolean / Integer / Double).
 * When unset (the default in form-driven processes), the value is returned as a
 * String. The field is the forward-compat slot for typed parameters; today
 * nothing populates it.</p>
 */
public class ProcessParameterValueTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String PARAMETER_NAME_PROPERTY      = "parameterName";
	public static final String PARAMETER_TYPE_PROPERTY      = "parameterType";
	public static final String STRING_VALUE_PROPERTY        = "stringValue";
	public static final String SET_AT_PROPERTY              = "setAt";
	public static final String PROCESS_INSTANCE_PROPERTY    = "processInstance";
	public static final String SET_BY_ELEMENT_PROPERTY      = "setByElement";

	// Parameter type vocabulary -- forward-compat slot. Anything else is treated
	// as String. Used by ProcessEngine.convertParameterValue at load time.
	public static final String TYPE_STRING  = "String";
	public static final String TYPE_BOOLEAN = "Boolean";
	public static final String TYPE_INTEGER = "Integer";
	public static final String TYPE_DOUBLE  = "Double";
	public static final String TYPE_DATE    = "Date";

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(ProcessParameterValue.class, (traits, node) -> new ProcessParameterValueTraitWrapper(traits, node));
	}

	public ProcessParameterValueTraitDefinition() {

		super(ProcessTraits.PROCESS_PARAMETER_VALUE);
	}

	@Override
	public Set<PropertyKey> createPropertyKeys(final TraitsInstance traitsInstance) {

		final Property<String> parameterName       = new StringProperty(PARAMETER_NAME_PROPERTY).indexed();
		final Property<String> parameterType       = new StringProperty(PARAMETER_TYPE_PROPERTY);
		final Property<String> stringValue         = new StringProperty(STRING_VALUE_PROPERTY).indexed();
		final Property<Date> setAt                 = new DateProperty(SET_AT_PROPERTY);
		final Property<NodeInterface> processInst  = new StartNode(traitsInstance, PROCESS_INSTANCE_PROPERTY, ProcessTraits.PROCESS_INSTANCE_HAS_PARAMETER_VALUE);
		final Property<NodeInterface> setByElement = new EndNode(traitsInstance, SET_BY_ELEMENT_PROPERTY, ProcessTraits.PROCESS_PARAMETER_VALUE_SET_BY_ELEMENT);

		return newSet(parameterName, parameterType, stringValue, setAt, processInst, setByElement);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public, newSet(PARAMETER_NAME_PROPERTY, PARAMETER_TYPE_PROPERTY, STRING_VALUE_PROPERTY, SET_AT_PROPERTY),
			PropertyView.Ui, newSet(PARAMETER_NAME_PROPERTY, PARAMETER_TYPE_PROPERTY, STRING_VALUE_PROPERTY, SET_AT_PROPERTY, PROCESS_INSTANCE_PROPERTY, SET_BY_ELEMENT_PROPERTY)
		);
	}

	@Override
	public Relation getRelation() {

		return null;
	}
}
