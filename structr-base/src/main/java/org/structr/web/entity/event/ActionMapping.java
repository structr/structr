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
import org.structr.core.property.*;
import org.structr.process.entity.Process;
import org.structr.process.entity.ProcessStep;
import org.structr.process.entity.relationship.*;
import org.structr.web.entity.dom.DOMElement;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.relationship.*;

public class ActionMapping extends AbstractNode {

	public static final Property<Iterable<DOMElement>> triggerElements           = new StartNodes<>("triggerElements", DOMElementTRIGGERED_BYActionMapping.class).partOfBuiltInSchema();
	public static final Property<Iterable<DOMNode>> successTargets               = new StartNodes<>("successTargets", DOMNodeSUCCESS_TARGETActionMapping.class).partOfBuiltInSchema();
	public static final Property<Iterable<DOMNode>> failureTargets               = new StartNodes<>("failureTargets", DOMNodeFAILURE_TARGETActionMapping.class).partOfBuiltInSchema();
	public static final Property<Iterable<ParameterMapping>> parameterMappings   = new EndNodes<>("parameterMappings", ActionMappingPARAMETERParameterMapping.class).partOfBuiltInSchema();
	public static final Property<Iterable<DOMNode>> successNotificationElements  = new StartNodes<>("successNotificationElements", DOMNodeSUCCESS_NOTIFICATION_ELEMENTActionMapping.class).partOfBuiltInSchema();
	public static final Property<Iterable<DOMNode>> failureNotificationElements  = new StartNodes<>("failureNotificationElements", DOMNodeFAILURE_NOTIFICATION_ELEMENTActionMapping.class).partOfBuiltInSchema();
	public static final Property<Process>                               process  = new EndNode<>("process", ActionMappingCONTROLSProcess.class).partOfBuiltInSchema();
	public static final Property<ProcessStep>                       processStep  = new EndNode<>("processStep", ActionMappingTRIGGERSProcessStep.class).partOfBuiltInSchema();
	public static final Property<Iterable<DOMNode>> processSuccessShowElements   = new StartNodes<>("processSuccessShowElements", DOMNodePROCESS_SHOW_ELEMENT_ON_SUCCESSActionMapping.class).partOfBuiltInSchema();
	public static final Property<Iterable<DOMNode>> processSuccessHideElements   = new StartNodes<>("processSuccessHideElements", DOMNodePROCESS_HIDE_ELEMENT_ON_SUCCESSActionMapping.class).partOfBuiltInSchema();
	public static final Property<Iterable<DOMNode>> processFailureShowElements   = new StartNodes<>("processFailureShowElements", DOMNodePROCESS_SHOW_ELEMENT_ON_FAILUREActionMapping.class).partOfBuiltInSchema();
	public static final Property<Iterable<DOMNode>> processFailureHideElements   = new StartNodes<>("processFailureHideElements", DOMNodePROCESS_HIDE_ELEMENT_ON_FAILUREActionMapping.class).partOfBuiltInSchema();

	public static final Property<String> eventProperty        = new StringProperty("event").hint("DOM event which triggers the action").partOfBuiltInSchema();
	public static final Property<String> actionProperty       = new StringProperty("action").hint("Action which will be triggered").partOfBuiltInSchema();
	public static final Property<String> methodProperty       = new StringProperty("method").hint("Name of method to execute when triggered action is 'method'").partOfBuiltInSchema();
	public static final Property<String> flowProperty         = new StringProperty("flow").hint("Name of flow to execute when triggered action is 'flow'").partOfBuiltInSchema();
	public static final Property<String> dataTypeProperty     = new StringProperty("dataType").hint("Data type for create action").partOfBuiltInSchema();
	public static final Property<String> idExpressionProperty = new StringProperty("idExpression").hint("Script expression that evaluates to the id of the object the method should be executed on").partOfBuiltInSchema();
	public static final Property<String> optionsProperty      = new StringProperty("options").hint("JSON string with that contains configuration options for this action mapping").partOfBuiltInSchema();

	public static final Property<String> dialogTypeProperty  = new StringProperty("dialogType").hint("Type of dialog to confirm a destructive / update action").partOfBuiltInSchema();
	public static final Property<String> dialogTitleProperty = new StringProperty("dialogTitle").hint("Dialog Title").partOfBuiltInSchema();
	public static final Property<String> dialogTextProperty  = new StringProperty("dialogText").hint("Dialog Text").partOfBuiltInSchema();

	public static final Property<String> successNotificationsProperty        = new StringProperty("successNotifications").hint("Notifications after successful execution of action").partOfBuiltInSchema();
	public static final Property<String> successNotificationsPartialProperty = new StringProperty("successNotificationsPartial").hint("CSS selector for partial to display as success notification").partOfBuiltInSchema();
	public static final Property<String> successNotificationsEventProperty   = new StringProperty("successNotificationsEvent").hint("Event to raise for success notifications").partOfBuiltInSchema();
	public static final Property<Integer> successNotificationsDelayProperty  = new IntProperty("successNotificationsDelay").hint("Delay before hiding success notifications").defaultValue(5000).partOfBuiltInSchema();

	public static final Property<String> failureNotificationsProperty        = new StringProperty("failureNotifications").hint("Notifications after failed execution of action").partOfBuiltInSchema();
	public static final Property<String> failureNotificationsPartialProperty = new StringProperty("failureNotificationsPartial").hint("CSS selector for partial to display as failure notification").partOfBuiltInSchema();
	public static final Property<String> failureNotificationsEventProperty   = new StringProperty("failureNotificationsEvent").hint("Event to raise for failure notifications").partOfBuiltInSchema();
	public static final Property<Integer> failureNotificationsDelayProperty  = new IntProperty("failureNotificationsDelay").hint("Delay before hiding failure notifications").defaultValue(5000).partOfBuiltInSchema();

	public static final Property<String> successBehaviourProperty = new StringProperty("successBehaviour").hint("Behaviour after successful execution of action").partOfBuiltInSchema();
	public static final Property<String> successPartialProperty   = new StringProperty("successPartial").hint("CSS selector for partial to refresh on success").partOfBuiltInSchema();
	public static final Property<String> successURLProperty       = new StringProperty("successURL").hint("URL to navigate to on success").partOfBuiltInSchema();
	public static final Property<String> successEventProperty     = new StringProperty("successEvent").hint("Event to raise on success").partOfBuiltInSchema();

	public static final Property<String> failureBehaviourProperty = new StringProperty("failureBehaviour").hint("Behaviour after failed execution of action").partOfBuiltInSchema();
	public static final Property<String> failurePartialProperty   = new StringProperty("failurePartial").hint("CSS selector for partial to refresh on failure").partOfBuiltInSchema();
	public static final Property<String> failureURLProperty       = new StringProperty("failureURL").hint("URL to navigate to on failure").partOfBuiltInSchema();
	public static final Property<String> failureEventProperty     = new StringProperty("failureEvent").hint("Event to raise on failure").partOfBuiltInSchema();

	public static final View uiView = new View(ActionMapping.class, PropertyView.Ui,
		eventProperty, actionProperty, methodProperty, flowProperty, dataTypeProperty, idExpressionProperty, optionsProperty, dialogTypeProperty, dialogTitleProperty, dialogTextProperty,
		successNotificationsProperty, successNotificationsPartialProperty, successNotificationsEventProperty,
		failureNotificationsProperty, failureNotificationsPartialProperty, failureNotificationsEventProperty,
		successBehaviourProperty, successPartialProperty, successURLProperty, successEventProperty, successNotificationsDelayProperty,
		failureBehaviourProperty, failurePartialProperty, failureURLProperty, failureEventProperty, failureNotificationsDelayProperty,
		triggerElements, successTargets, failureTargets, successNotificationElements, failureNotificationElements, parameterMappings, process, processStep, processSuccessShowElements, processSuccessHideElements
	);
}
