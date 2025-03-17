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

import org.structr.common.PropertyView;
import org.structr.core.api.AbstractMethod;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.RelationshipTraitFactory;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.web.entity.event.ActionMapping;
import org.structr.web.traits.wrappers.ActionMappingTraitWrapper;

import java.util.Map;
import java.util.Set;

public class ActionMappingTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String TRIGGER_ELEMENTS_PROPERTY              = "triggerElements";
	public static final String SUCCESS_TARGETS_PROPERTY               = "successTargets";
	public static final String FAILURE_TARGETS_PROPERTY               = "failureTargets";
	public static final String PARAMETER_MAPPINGS_PROPERTY            = "parameterMappings";
	public static final String SUCCESS_NOTIFICATION_ELEMENTS_PROPERTY = "successNotificationElements";
	public static final String FAILURE_NOTIFICATION_ELEMENTS_PROPERTY = "failureNotificationElements";
	public static final String EVENT_PROPERTY                         = "event";
	public static final String ACTION_PROPERTY                        = "action";
	public static final String METHOD_PROPERTY                        = "method";
	public static final String DATA_TYPE_PROPERTY                     = "dataType";
	public static final String ID_EXPRESSION_PROPERTY                 = "idExpression";
	public static final String OPTIONS_PROPERTY                       = "options";
	public static final String DIALOG_TYPE_PROPERTY                   = "dialogType";
	public static final String DIALOG_TITLE_PROPERTY                  = "dialogTitle";
	public static final String DIALOG_TEXT_PROPERTY                   = "dialogText";
	public static final String SUCCESS_NOTIFICATIONS_PROPERTY         = "successNotifications";
	public static final String SUCCESS_NOTIFICATIONS_PARTIAL_PROPERTY = "successNotificationsPartial";
	public static final String SUCCESS_NOTIFICATIONS_EVENT_PROPERTY   = "successNotificationsEvent";
	public static final String SUCCESS_NOTIFICATIONS_DELAY_PROPERTY   = "successNotificationsDelay";
	public static final String FAILURE_NOTIFICATIONS_PROPERTY         = "failureNotifications";
	public static final String FAILURE_NOTIFICATIONS_PARTIAL_PROPERTY = "failureNotificationsPartial";
	public static final String FAILURE_NOTIFICATIONS_EVENT_PROPERTY   = "failureNotificationsEvent";
	public static final String FAILURE_NOTIFICATIONS_DELAY_PROPERTY   = "failureNotificationsDelay";
	public static final String SUCCESS_BEHAVIOUR_PROPERTY             = "successBehaviour";
	public static final String SUCCESS_PARTIAL_PROPERTY               = "successPartial";
	public static final String SUCCESS_URL_PROPERTY                   = "successURL";
	public static final String SUCCESS_EVENT_PROPERTY                 = "successEvent";
	public static final String FAILURE_BEHAVIOUR_PROPERTY             = "failureBehaviour";
	public static final String FAILURE_PARTIAL_PROPERTY               = "failurePartial";
	public static final String FAILURE_URL_PROPERTY                   = "failureURL";
	public static final String FAILURE_EVENT_PROPERTY                 = "failureEvent";

	public ActionMappingTraitDefinition() {
		super(StructrTraits.ACTION_MAPPING);
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
			ActionMapping.class, (traits, node) -> new ActionMappingTraitWrapper(traits, node)
		);
	}

	@Override
	public Set<AbstractMethod> getDynamicMethods() {
		return Set.of();
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final Property<Iterable<NodeInterface>> triggerElements              = new StartNodes(TRIGGER_ELEMENTS_PROPERTY, StructrTraits.DOM_ELEMENT_TRIGGERED_BY_ACTION_MAPPING);
		final Property<Iterable<NodeInterface>> successTargets               = new StartNodes(SUCCESS_TARGETS_PROPERTY, StructrTraits.DOM_NODE_SUCCESS_TARGET_ACTION_MAPPING);
		final Property<Iterable<NodeInterface>> failureTargets               = new StartNodes(FAILURE_TARGETS_PROPERTY, StructrTraits.DOM_NODE_FAILURE_TARGET_ACTION_MAPPING);
		final Property<Iterable<NodeInterface>> parameterMappings            = new EndNodes(PARAMETER_MAPPINGS_PROPERTY, StructrTraits.ACTION_MAPPING_PARAMETER_PARAMETER_MAPPING);
		final Property<Iterable<NodeInterface>> successNotificationElements  = new StartNodes(SUCCESS_NOTIFICATION_ELEMENTS_PROPERTY, StructrTraits.DOM_NODE_SUCCESS_NOTIFICATION_ELEMENT_ACTION_MAPPING);
		final Property<Iterable<NodeInterface>> failureNotificationElements  = new StartNodes(FAILURE_NOTIFICATION_ELEMENTS_PROPERTY, StructrTraits.DOM_NODE_FAILURE_NOTIFICATION_ELEMENT_ACTION_MAPPING);

		final Property<String> eventProperty                                 = new StringProperty(EVENT_PROPERTY).hint("DOM event which triggers the action");
		final Property<String> actionProperty                                = new StringProperty(ACTION_PROPERTY).hint("Action which will be triggered");
		final Property<String> methodProperty                                = new StringProperty(METHOD_PROPERTY).hint("Name of method to execute when triggered action is 'method'");
		final Property<String> dataTypeProperty                              = new StringProperty(DATA_TYPE_PROPERTY).hint("Data type for create action");
		final Property<String> idExpressionProperty                          = new StringProperty(ID_EXPRESSION_PROPERTY).hint("Script expression that evaluates to the id of the object the method should be executed on");
		final Property<String> optionsProperty                               = new StringProperty(OPTIONS_PROPERTY).hint("JSON string with that contains configuration options for this action mapping");

		final Property<String> dialogTypeProperty                            = new StringProperty(DIALOG_TYPE_PROPERTY).hint("Type of dialog to confirm a destructive / update action");
		final Property<String> dialogTitleProperty                           = new StringProperty(DIALOG_TITLE_PROPERTY).hint("Dialog Title");
		final Property<String> dialogTextProperty                            = new StringProperty(DIALOG_TEXT_PROPERTY).hint("Dialog Text");

		final Property<String> successNotificationsProperty                  = new StringProperty(SUCCESS_NOTIFICATIONS_PROPERTY).hint("Notifications after successful execution of action");
		final Property<String> successNotificationsPartialProperty           = new StringProperty(SUCCESS_NOTIFICATIONS_PARTIAL_PROPERTY).hint("CSS selector for partial to display as success notification");
		final Property<String> successNotificationsEventProperty             = new StringProperty(SUCCESS_NOTIFICATIONS_EVENT_PROPERTY).hint("Event to raise for success notifications");
		final Property<Integer> successNotificationsDelayProperty            = new IntProperty(SUCCESS_NOTIFICATIONS_DELAY_PROPERTY).hint("Delay before hiding success notifications").defaultValue(5000);

		final Property<String> failureNotificationsProperty                  = new StringProperty(FAILURE_NOTIFICATIONS_PROPERTY).hint("Notifications after failed execution of action");
		final Property<String> failureNotificationsPartialProperty           = new StringProperty(FAILURE_NOTIFICATIONS_PARTIAL_PROPERTY).hint("CSS selector for partial to display as failure notification");
		final Property<String> failureNotificationsEventProperty             = new StringProperty(FAILURE_NOTIFICATIONS_EVENT_PROPERTY).hint("Event to raise for failure notifications");
		final Property<Integer> failureNotificationsDelayProperty            = new IntProperty(FAILURE_NOTIFICATIONS_DELAY_PROPERTY).hint("Delay before hiding failure notifications").defaultValue(5000);

		final Property<String> successBehaviourProperty                      = new StringProperty(SUCCESS_BEHAVIOUR_PROPERTY).hint("Behaviour after successful execution of action");
		final Property<String> successPartialProperty                        = new StringProperty(SUCCESS_PARTIAL_PROPERTY).hint("CSS selector for partial to refresh on success");
		final Property<String> successURLProperty                            = new StringProperty(SUCCESS_URL_PROPERTY).hint("URL to navigate to on success");
		final Property<String> successEventProperty                          = new StringProperty(SUCCESS_EVENT_PROPERTY).hint("Event to raise on success");

		final Property<String> failureBehaviourProperty                      = new StringProperty(FAILURE_BEHAVIOUR_PROPERTY).hint("Behaviour after failed execution of action");
		final Property<String> failurePartialProperty                        = new StringProperty(FAILURE_PARTIAL_PROPERTY).hint("CSS selector for partial to refresh on failure");
		final Property<String> failureURLProperty                            = new StringProperty(FAILURE_URL_PROPERTY).hint("URL to navigate to on failure");
		final Property<String> failureEventProperty                          = new StringProperty(FAILURE_EVENT_PROPERTY).hint("Event to raise on failure");

		return Set.of(
			triggerElements,
			successTargets,
			failureTargets,
			parameterMappings,
			successNotificationElements,
			failureNotificationElements,

			eventProperty,
			actionProperty,
			methodProperty,
			dataTypeProperty,
			idExpressionProperty,
			optionsProperty,

			dialogTypeProperty,
			dialogTitleProperty,
			dialogTextProperty,

			successNotificationsProperty,
			successNotificationsPartialProperty,
			successNotificationsEventProperty,
			successNotificationsDelayProperty,

			failureNotificationsProperty,
			failureNotificationsPartialProperty,
			failureNotificationsEventProperty,
			failureNotificationsDelayProperty,

			successBehaviourProperty,
			successPartialProperty,
			successURLProperty,
			successEventProperty,

			failureBehaviourProperty,
			failurePartialProperty,
			failureURLProperty,
			failureEventProperty
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public,
			newSet(

			),

			PropertyView.Ui,
			newSet(
					EVENT_PROPERTY, ACTION_PROPERTY, METHOD_PROPERTY, DATA_TYPE_PROPERTY, ID_EXPRESSION_PROPERTY, OPTIONS_PROPERTY,
					DIALOG_TYPE_PROPERTY, DIALOG_TITLE_PROPERTY, DIALOG_TEXT_PROPERTY, SUCCESS_NOTIFICATIONS_PROPERTY, SUCCESS_NOTIFICATIONS_PARTIAL_PROPERTY,
					SUCCESS_NOTIFICATIONS_EVENT_PROPERTY, FAILURE_NOTIFICATIONS_PROPERTY, FAILURE_NOTIFICATIONS_PARTIAL_PROPERTY,
					FAILURE_NOTIFICATIONS_EVENT_PROPERTY, SUCCESS_BEHAVIOUR_PROPERTY, SUCCESS_PARTIAL_PROPERTY, SUCCESS_URL_PROPERTY,
					SUCCESS_EVENT_PROPERTY, SUCCESS_NOTIFICATIONS_DELAY_PROPERTY, FAILURE_BEHAVIOUR_PROPERTY, FAILURE_PARTIAL_PROPERTY,
					FAILURE_URL_PROPERTY, FAILURE_EVENT_PROPERTY, FAILURE_NOTIFICATIONS_DELAY_PROPERTY, TRIGGER_ELEMENTS_PROPERTY,
					SUCCESS_TARGETS_PROPERTY, FAILURE_TARGETS_PROPERTY, SUCCESS_NOTIFICATION_ELEMENTS_PROPERTY,
					FAILURE_NOTIFICATION_ELEMENTS_PROPERTY, PARAMETER_MAPPINGS_PROPERTY
			)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
