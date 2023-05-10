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

public interface ActionMapping extends NodeInterface {

	static class Impl { static {

		final JsonSchema schema               = SchemaService.getDynamicSchema();
		final JsonObjectType type             = schema.addType("ActionMapping");
		final JsonObjectType parameterMapping = schema.addType("ParameterMapping");
		final JsonObjectType domNode          = schema.addType("DOMNode");
		final JsonObjectType domElement       = schema.addType("DOMElement");

		//type.setIsAbstract();
		type.setImplements(URI.create("https://structr.org/v1.1/definitions/ActionMapping"));
		type.setExtends(URI.create("#/definitions/NodeInterface"));

		type.relate(domElement,       "TRIGGERED_BY",   Cardinality.ManyToMany, "triggeredActions",   "triggerElements");
		type.relate(domNode,          "SUCCESS_TARGET", Cardinality.ManyToMany,"reloadingActions",   "successTargets");
		type.relate(domNode,          "FAILURE_TARGET", Cardinality.ManyToMany,"redirectingActions", "failureTargets");
		type.relate(parameterMapping, "PARAMETER",      Cardinality.OneToMany, "actionMapping",      "parameterMappings");

		type.addViewProperty(PropertyView.Ui, "triggerElements");
		type.addViewProperty(PropertyView.Ui, "successTargets");
		type.addViewProperty(PropertyView.Ui, "failureTargets");
		type.addViewProperty(PropertyView.Ui, "parameterMappings");

		type.addStringProperty("event",            PropertyView.Ui).setHint("DOM event which triggers the action");
		type.addStringProperty("action",           PropertyView.Ui).setHint("Action which will be triggered");
		type.addStringProperty("method",           PropertyView.Ui).setHint("Name of method to execute when triggered action is 'method'");
		type.addStringProperty("dataType",         PropertyView.Ui).setHint("Data type for create action");
		type.addStringProperty("idExpression",     PropertyView.Ui).setHint("Script expression that evaluates to the id of the object the method should be executed on");

		type.addStringProperty("successBehaviour", PropertyView.Ui).setHint("Behaviour after successful execution of action");
		type.addStringProperty("successPartial",   PropertyView.Ui).setHint("CSS selector for partial to refresh on success");
		type.addStringProperty("successURL",       PropertyView.Ui).setHint("URL to navigate to on success");
		type.addStringProperty("successEvent",     PropertyView.Ui).setHint("Event to raise on success");


		type.addStringProperty("failureBehaviour", PropertyView.Ui).setHint("Behaviour after failure execution of action");
		type.addStringProperty("failurePartial",   PropertyView.Ui).setHint("CSS selector for partial to refresh on failure");
		type.addStringProperty("failureURL",       PropertyView.Ui).setHint("URL to navigate to on failure");
		type.addStringProperty("failureEvent",     PropertyView.Ui).setHint("Event to raise on failure");

	}}


}
