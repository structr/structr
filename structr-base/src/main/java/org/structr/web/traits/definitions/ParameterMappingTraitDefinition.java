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

import org.structr.core.api.AbstractMethod;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.StartNode;
import org.structr.core.property.StringProperty;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.RelationshipTraitFactory;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.web.entity.event.ParameterMapping;
import org.structr.web.traits.wrappers.ParameterMappingTraitWrapper;

import java.util.Map;
import java.util.Set;

public class ParameterMappingTraitDefinition extends AbstractNodeTraitDefinition {

	/*
	public static final View uiView = new View(ParameterMapping.class, PropertyView.Ui,
		parameterType, parameterName, constantValue, scriptExpression, methodResult, flowResult, inputElement
	);
	*/

	public ParameterMappingTraitDefinition() {
		super(StructrTraits.PARAMETER_MAPPING);
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
			ParameterMapping.class, (traits, node) -> new ParameterMappingTraitWrapper(traits, node)
		);
	}

	@Override
	public Set<AbstractMethod> getDynamicMethods() {
		return Set.of();
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final Property<NodeInterface> actionMappingProperty = new StartNode("actionMapping", StructrTraits.ACTION_MAPPING_PARAMETER_PARAMETER_MAPPING);
		final Property<NodeInterface> inputElement          = new StartNode("inputElement", StructrTraits.DOM_ELEMENT_INPUT_ELEMENT_PARAMETER_MAPPING);

		// user-input, constant-value, page-param, pagesize-param, script-expression, method-result, flow-result
		final Property<String> parameterType    = new StringProperty("parameterType").hint("Type of this parameter, e.g. user input, constant value, page-param, pagesize-param, result of a script expression, method call or flow...");
		final Property<String> parameterName    = new StringProperty("parameterName").hint("Parameter name");
		final Property<String> constantValue    = new StringProperty("constantValue").hint("Constant value");
		final Property<String> scriptExpression = new StringProperty("scriptExpression").hint("Script expression to be evaluated to result value");
		final Property<String> methodResult     = new StringProperty("methodResult").hint("Method to be evaluated to result value");
		final Property<String> flowResult       = new StringProperty("flowResult").hint("Flow to be evaluated to result value");

		return Set.of(
			actionMappingProperty,
			inputElement,
			parameterType,
			parameterName,
			constantValue,
			scriptExpression,
			methodResult,
			flowResult
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
