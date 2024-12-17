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
import org.structr.core.property.*;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.RelationshipTraitFactory;
import org.structr.core.traits.definitions.AbstractTraitDefinition;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.web.entity.event.ActionMapping;
import org.structr.web.traits.wrappers.ActionMappingTraitWrapper;

import java.util.Map;
import java.util.Set;

public class ActionMappingTraitDefinition extends AbstractTraitDefinition {

	private static final Property<Iterable<NodeInterface>> triggerElements              = new StartNodes("triggerElements", "DOMElementTRIGGERED_BYActionMapping").partOfBuiltInSchema();
	private static final Property<Iterable<NodeInterface>> successTargets               = new StartNodes("successTargets", "DOMNodeSUCCESS_TARGETActionMapping").partOfBuiltInSchema();
	private static final Property<Iterable<NodeInterface>> failureTargets               = new StartNodes("failureTargets", "DOMNodeFAILURE_TARGETActionMapping").partOfBuiltInSchema();
	private static final Property<Iterable<NodeInterface>> parameterMappings            = new EndNodes("parameterMappings", "ActionMappingPARAMETERParameterMapping").partOfBuiltInSchema();
	private static final Property<Iterable<NodeInterface>> successNotificationElements  = new StartNodes("successNotificationElements", "DOMNodeSUCCESS_NOTIFICATION_ELEMENTActionMapping").partOfBuiltInSchema();
	private static final Property<Iterable<NodeInterface>> failureNotificationElements  = new StartNodes("failureNotificationElements", "DOMNodeFAILURE_NOTIFICATION_ELEMENTActionMapping").partOfBuiltInSchema();

	private static final Property<String> eventProperty        = new StringProperty("event").hint("DOM event which triggers the action").partOfBuiltInSchema();
	private static final Property<String> actionProperty       = new StringProperty("action").hint("Action which will be triggered").partOfBuiltInSchema();
	private static final Property<String> methodProperty       = new StringProperty("method").hint("Name of method to execute when triggered action is 'method'").partOfBuiltInSchema();
	private static final Property<String> dataTypeProperty     = new StringProperty("dataType").hint("Data type for create action").partOfBuiltInSchema();
	private static final Property<String> idExpressionProperty = new StringProperty("idExpression").hint("Script expression that evaluates to the id of the object the method should be executed on").partOfBuiltInSchema();
	private static final Property<String> optionsProperty      = new StringProperty("options").hint("JSON string with that contains configuration options for this action mapping").partOfBuiltInSchema();

	private static final Property<String> dialogTypeProperty  = new StringProperty("dialogType").hint("Type of dialog to confirm a destructive / update action").partOfBuiltInSchema();
	private static final Property<String> dialogTitleProperty = new StringProperty("dialogTitle").hint("Dialog Title").partOfBuiltInSchema();
	private static final Property<String> dialogTextProperty  = new StringProperty("dialogText").hint("Dialog Text").partOfBuiltInSchema();

	private static final Property<String> successNotificationsProperty        = new StringProperty("successNotifications").hint("Notifications after successful execution of action").partOfBuiltInSchema();
	private static final Property<String> successNotificationsPartialProperty = new StringProperty("successNotificationsPartial").hint("CSS selector for partial to display as success notification").partOfBuiltInSchema();
	private static final Property<String> successNotificationsEventProperty   = new StringProperty("successNotificationsEvent").hint("Event to raise for success notifications").partOfBuiltInSchema();
	private static final Property<Integer> successNotificationsDelayProperty  = new IntProperty("successNotificationsDelay").hint("Delay before hiding success notifications").defaultValue(5000).partOfBuiltInSchema();

	private static final Property<String> failureNotificationsProperty        = new StringProperty("failureNotifications").hint("Notifications after failed execution of action").partOfBuiltInSchema();
	private static final Property<String> failureNotificationsPartialProperty = new StringProperty("failureNotificationsPartial").hint("CSS selector for partial to display as failure notification").partOfBuiltInSchema();
	private static final Property<String> failureNotificationsEventProperty   = new StringProperty("failureNotificationsEvent").hint("Event to raise for failure notifications").partOfBuiltInSchema();
	private static final Property<Integer> failureNotificationsDelayProperty  = new IntProperty("failureNotificationsDelay").hint("Delay before hiding failure notifications").defaultValue(5000).partOfBuiltInSchema();

	private static final Property<String> successBehaviourProperty = new StringProperty("successBehaviour").hint("Behaviour after successful execution of action").partOfBuiltInSchema();
	private static final Property<String> successPartialProperty   = new StringProperty("successPartial").hint("CSS selector for partial to refresh on success").partOfBuiltInSchema();
	private static final Property<String> successURLProperty       = new StringProperty("successURL").hint("URL to navigate to on success").partOfBuiltInSchema();
	private static final Property<String> successEventProperty     = new StringProperty("successEvent").hint("Event to raise on success").partOfBuiltInSchema();

	private static final Property<String> failureBehaviourProperty = new StringProperty("failureBehaviour").hint("Behaviour after failed execution of action").partOfBuiltInSchema();
	private static final Property<String> failurePartialProperty   = new StringProperty("failurePartial").hint("CSS selector for partial to refresh on failure").partOfBuiltInSchema();
	private static final Property<String> failureURLProperty       = new StringProperty("failureURL").hint("URL to navigate to on failure").partOfBuiltInSchema();
	private static final Property<String> failureEventProperty     = new StringProperty("failureEvent").hint("Event to raise on failure").partOfBuiltInSchema();

	public ActionMappingTraitDefinition() {
		super("ActionMapping");
	}

	/*
	public static final View uiView = new View(ActionMapping.class, PropertyView.Ui,
		eventProperty, actionProperty, methodProperty, dataTypeProperty, idExpressionProperty, optionsProperty, dialogTypeProperty, dialogTitleProperty, dialogTextProperty,
		successNotificationsProperty, successNotificationsPartialProperty, successNotificationsEventProperty,
		failureNotificationsProperty, failureNotificationsPartialProperty, failureNotificationsEventProperty,
		successBehaviourProperty, successPartialProperty, successURLProperty, successEventProperty, successNotificationsDelayProperty,
		failureBehaviourProperty, failurePartialProperty, failureURLProperty, failureEventProperty, failureNotificationsDelayProperty,

		triggerElements, successTargets, failureTargets, successNotificationElements, failureNotificationElements, parameterMappings
	);
	*/

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
	public Relation getRelation() {
		return null;
	}
}
