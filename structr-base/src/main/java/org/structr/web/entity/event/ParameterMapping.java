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
package org.structr.web.entity.event;

import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.core.entity.AbstractNode;
import org.structr.core.property.Property;
import org.structr.core.property.StartNode;
import org.structr.core.property.StringProperty;
import org.structr.web.entity.dom.DOMElement;
import org.structr.web.entity.dom.relationship.ActionMappingPARAMETERParameterMapping;
import org.structr.web.entity.dom.relationship.DOMElementINPUT_ELEMENTParameterMapping;

public class ParameterMapping extends AbstractNode {

	public static final Property<ActionMapping> actionMappingProperty = new StartNode<>("actionMapping", ActionMappingPARAMETERParameterMapping.class).partOfBuiltInSchema();
	public static final Property<DOMElement> inputElement             = new StartNode<>("inputElement", DOMElementINPUT_ELEMENTParameterMapping.class).partOfBuiltInSchema();

	// user-input, constant-value, page-param, pagesize-param, script-expression, method-result, flow-result
	public static final Property<String> parameterType    = new StringProperty("parameterType").hint("Type of this parameter, e.g. user input, constant value, page-param, pagesize-param, result of a script expression, method call or flow...").partOfBuiltInSchema();
	public static final Property<String> parameterName    = new StringProperty("parameterName").hint("Parameter name").partOfBuiltInSchema();
	public static final Property<String> constantValue    = new StringProperty("constantValue").hint("Constant value").partOfBuiltInSchema();
	public static final Property<String> scriptExpression = new StringProperty("scriptExpression").hint("Script expression to be evaluated to result value").partOfBuiltInSchema();
	public static final Property<String> methodResult     = new StringProperty("methodResult").hint("Method to be evaluated to result value").partOfBuiltInSchema();
	public static final Property<String> flowResult       = new StringProperty("flowResult").hint("Flow to be evaluated to result value").partOfBuiltInSchema();

	public static final View uiView = new View(ParameterMapping.class, PropertyView.Ui,
		parameterType, parameterName, constantValue, scriptExpression, methodResult, flowResult, inputElement
	);
}
