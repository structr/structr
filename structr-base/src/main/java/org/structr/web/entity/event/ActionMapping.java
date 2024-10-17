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

		domElement.relate(type,       "TRIGGERED_BY",   Cardinality.ManyToMany,   "triggerElements", "triggeredActions");
		domNode.relate(type,          "SUCCESS_TARGET", Cardinality.ManyToMany,"successTargets","reloadingActions");
		domNode.relate(type,          "FAILURE_TARGET", Cardinality.ManyToMany,  "failureTargets","failureActions");
		type.relate(parameterMapping, "PARAMETER",      Cardinality.OneToMany, "actionMapping",   "parameterMappings");
		domNode.relate(type,          "SUCCESS_NOTIFICATION_ELEMENT", Cardinality.ManyToMany,   "successNotificationElements","successNotificationActions");
		domNode.relate(type,          "FAILURE_NOTIFICATION_ELEMENT", Cardinality.ManyToMany, "failureNotificationElements","failureNotificationActions");

		type.addViewProperty(PropertyView.Ui, "triggerElements");
		type.addViewProperty(PropertyView.Ui, "successTargets");
		type.addViewProperty(PropertyView.Ui, "failureTargets");
		type.addViewProperty(PropertyView.Ui, "parameterMappings");
		type.addViewProperty(PropertyView.Ui, "successNotificationElements");
		type.addViewProperty(PropertyView.Ui, "failureNotificationElements");

		type.addStringProperty("event",            PropertyView.Ui).setHint("DOM event which triggers the action");
		type.addStringProperty("action",           PropertyView.Ui).setHint("Action which will be triggered");
		type.addStringProperty("method",           PropertyView.Ui).setHint("Name of method to execute when triggered action is 'method'");
		type.addStringProperty("dataType",         PropertyView.Ui).setHint("Data type for create action");
		type.addStringProperty("idExpression",     PropertyView.Ui).setHint("Script expression that evaluates to the id of the object the method should be executed on");
		type.addStringProperty("options",          PropertyView.Ui).setHint("JSON string with that contains configuration options for this action mapping");

		type.addStringProperty("dialogType",        	PropertyView.Ui).setHint("Type of dialog to confirm a destructive / update action");
		type.addStringProperty("dialogTitle", 		PropertyView.Ui).setHint("Dialog Title");
		type.addStringProperty("dialogText",   		PropertyView.Ui).setHint("Dialog Text");

		type.addStringProperty("successNotifications",        PropertyView.Ui).setHint("Notifications after successful execution of action");
		type.addStringProperty("successNotificationsPartial", PropertyView.Ui).setHint("CSS selector for partial to display as success notification");
		type.addStringProperty("successNotificationsEvent",   PropertyView.Ui).setHint("Event to raise for success notifications");
		type.addIntegerProperty("successNotificationsDelay",  PropertyView.Ui).setHint("Delay before hiding success notifications").setDefaultValue("5000");

		type.addStringProperty("failureNotifications",        PropertyView.Ui).setHint("Notifications after failed execution of action");
		type.addStringProperty("failureNotificationsPartial", PropertyView.Ui).setHint("CSS selector for partial to display as failure notification");
		type.addStringProperty("failureNotificationsEvent",   PropertyView.Ui).setHint("Event to raise for failure notifications");
		type.addIntegerProperty("failureNotificationsDelay",  PropertyView.Ui).setHint("Delay before hiding failure notifications").setDefaultValue("5000");

		type.addStringProperty("successBehaviour", PropertyView.Ui).setHint("Behaviour after successful execution of action");
		type.addStringProperty("successPartial",   PropertyView.Ui).setHint("CSS selector for partial to refresh on success");
		type.addStringProperty("successURL",       PropertyView.Ui).setHint("URL to navigate to on success");
		type.addStringProperty("successEvent",     PropertyView.Ui).setHint("Event to raise on success");

		
		type.addStringProperty("failureBehaviour", PropertyView.Ui).setHint("Behaviour after failed execution of action");
		type.addStringProperty("failurePartial",   PropertyView.Ui).setHint("CSS selector for partial to refresh on failure");
		type.addStringProperty("failureURL",       PropertyView.Ui).setHint("URL to navigate to on failure");
		type.addStringProperty("failureEvent",     PropertyView.Ui).setHint("Event to raise on failure");

	}}


}
