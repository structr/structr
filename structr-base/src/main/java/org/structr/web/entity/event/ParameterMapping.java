/*
 * Copyright (C) 2010-2023 Structr GmbH
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

import org.structr.api.graph.Cardinality;
import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonSchema;
import org.structr.common.PropertyView;
import org.structr.core.graph.NodeInterface;
import org.structr.schema.SchemaService;

import java.net.URI;

public interface ParameterMapping extends NodeInterface {

	static class Impl { static {

		final JsonSchema schema       = SchemaService.getDynamicSchema();
		final JsonObjectType type     = schema.addType("ParameterMapping");
		final JsonObjectType elem     = schema.addType("DOMElement");

		//type.setIsAbstract();
		type.setImplements(URI.create("https://structr.org/v1.1/definitions/ParameterMapping"));
		type.setExtends(URI.create("#/definitions/NodeInterface"));

		type.addStringProperty("parameterType",        PropertyView.Ui).setHint("Type of this parameter, e.g. user input, constant value, page-param, pagesize-param, result of a script expression, method call or flow...");
		// user-input, constant-value, page-param, pagesize-param, script-expression, method-result, flow-result

		type.addStringProperty("parameterName",        PropertyView.Ui).setHint("Parameter name");
		type.addStringProperty("constantValue",        PropertyView.Ui).setHint("Constant value");
		type.addStringProperty("scriptExpression",     PropertyView.Ui).setHint("Script expression to be evaluated to result value");
		type.addStringProperty("methodResult",         PropertyView.Ui).setHint("Method to be evaluated to result value");
		type.addStringProperty("flowResult",           PropertyView.Ui).setHint("Flow to be evaluated to result value");

		type.relate(elem, "INPUT_ELEMENT",   Cardinality.ManyToOne,"parameterMappings",  "inputElement");

		type.addViewProperty(PropertyView.Ui, "inputElement");

	}}


}
