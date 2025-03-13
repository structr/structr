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
package org.structr.web.traits.wrappers;

import org.structr.api.util.Iterables;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.Traits;
import org.structr.core.traits.wrappers.AbstractNodeTraitWrapper;
import org.structr.web.entity.dom.DOMElement;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.event.ActionMapping;
import org.structr.web.entity.event.ParameterMapping;
import org.structr.web.traits.definitions.ActionMappingTraitDefinition;

public class ActionMappingTraitWrapper extends AbstractNodeTraitWrapper implements ActionMapping {

	public ActionMappingTraitWrapper(final Traits traits, final NodeInterface node) {
		super(traits, node);
	}

	@Override
	public String getOptions() {
		return wrappedObject.getProperty(traits.key(ActionMappingTraitDefinition.OPTIONS_PROPERTY));
	}

	@Override
	public String getEvent() {
		return wrappedObject.getProperty(traits.key(ActionMappingTraitDefinition.EVENT_PROPERTY));
	}

	@Override
	public String getAction() {
		return wrappedObject.getProperty(traits.key(ActionMappingTraitDefinition.ACTION_PROPERTY));
	}

	@Override
	public void setAction(final String action) throws FrameworkException {
		wrappedObject.setProperty(traits.key(ActionMappingTraitDefinition.ACTION_PROPERTY), action);
	}

	@Override
	public String getMethod() {
		return wrappedObject.getProperty(traits.key(ActionMappingTraitDefinition.METHOD_PROPERTY));
	}

	@Override
	public void setMethod(final String method) throws FrameworkException {
		wrappedObject.setProperty(traits.key(ActionMappingTraitDefinition.METHOD_PROPERTY), method);
	}

	@Override
	public void setSuccessBehaviour(String successBehaviour) throws FrameworkException {
		wrappedObject.setProperty(traits.key(ActionMappingTraitDefinition.SUCCESS_BEHAVIOUR_PROPERTY), successBehaviour);
	}

	@Override
	public void setFailureBehaviour(String failureBehaviour) throws FrameworkException {
		wrappedObject.setProperty(traits.key(ActionMappingTraitDefinition.FAILURE_BEHAVIOUR_PROPERTY), failureBehaviour);
	}

	@Override
	public String getDataType() {
		return wrappedObject.getProperty(traits.key(ActionMappingTraitDefinition.DATA_TYPE_PROPERTY));
	}

	@Override
	public String getIdExpression() {
		return wrappedObject.getProperty(traits.key(ActionMappingTraitDefinition.ID_EXPRESSION_PROPERTY));
	}

	@Override
	public String getDialogType() {
		return wrappedObject.getProperty(traits.key(ActionMappingTraitDefinition.DIALOG_TYPE_PROPERTY));
	}

	@Override
	public String getDialogTitle() {
		return wrappedObject.getProperty(traits.key(ActionMappingTraitDefinition.DIALOG_TITLE_PROPERTY));
	}

	@Override
	public String getDialogText() {
		return wrappedObject.getProperty(traits.key(ActionMappingTraitDefinition.DIALOG_TEXT_PROPERTY));
	}

	@Override
	public Iterable<ParameterMapping> getParameterMappings() {

		final PropertyKey<Iterable<NodeInterface>> key = traits.key(ActionMappingTraitDefinition.PARAMETER_MAPPINGS_PROPERTY);

		return Iterables.map(n -> n.as(ParameterMapping.class), wrappedObject.getProperty(key));
	}

	@Override
	public Iterable<DOMElement> getTriggerElements() {

		final PropertyKey<Iterable<NodeInterface>> key = traits.key(ActionMappingTraitDefinition.TRIGGER_ELEMENTS_PROPERTY);

		return Iterables.map(n -> n.as(DOMElement.class), wrappedObject.getProperty(key));
	}

	@Override
	public Iterable<DOMNode> getSuccessTargets() {

		final PropertyKey<Iterable<NodeInterface>> key = traits.key(ActionMappingTraitDefinition.SUCCESS_TARGETS_PROPERTY);

		return Iterables.map(n -> n.as(DOMNode.class), wrappedObject.getProperty(key));
	}

	@Override
	public Iterable<DOMNode> getFailureTargets() {

		final PropertyKey<Iterable<NodeInterface>> key = traits.key(ActionMappingTraitDefinition.FAILURE_TARGETS_PROPERTY);

		return Iterables.map(n -> n.as(DOMNode.class), wrappedObject.getProperty(key));
	}

	@Override
	public Iterable<DOMNode> getSuccessNotificationElements() {

		final PropertyKey<Iterable<NodeInterface>> key = traits.key(ActionMappingTraitDefinition.SUCCESS_NOTIFICATION_ELEMENTS_PROPERTY);

		return Iterables.map(n -> n.as(DOMNode.class), wrappedObject.getProperty(key));
	}

	@Override
	public Iterable<DOMNode> getFailureNotificationElements() {

		final PropertyKey<Iterable<NodeInterface>> key = traits.key(ActionMappingTraitDefinition.FAILURE_NOTIFICATION_ELEMENTS_PROPERTY);

		return Iterables.map(n -> n.as(DOMNode.class), wrappedObject.getProperty(key));
	}

	@Override
	public String getSuccessNotifications() {
		return wrappedObject.getProperty(traits.key(ActionMappingTraitDefinition.SUCCESS_NOTIFICATIONS_PROPERTY));
	}

	@Override
	public String getSuccessBehaviour() {
		return wrappedObject.getProperty(traits.key(ActionMappingTraitDefinition.SUCCESS_BEHAVIOUR_PROPERTY));
	}

	@Override
	public String getSuccessPartial() {
		return wrappedObject.getProperty(traits.key(ActionMappingTraitDefinition.SUCCESS_PARTIAL_PROPERTY));
	}

	@Override
	public String getSuccessURL() {
		return wrappedObject.getProperty(traits.key(ActionMappingTraitDefinition.SUCCESS_URL_PROPERTY));
	}

	@Override
	public String getSuccessEvent() {
		return wrappedObject.getProperty(traits.key(ActionMappingTraitDefinition.SUCCESS_EVENT_PROPERTY));
	}

	@Override
	public String getSuccessNotificationsPartial() {
		return wrappedObject.getProperty(traits.key(ActionMappingTraitDefinition.SUCCESS_NOTIFICATIONS_PARTIAL_PROPERTY));
	}

	@Override
	public String getSuccessNotificationsEvent() {
		return wrappedObject.getProperty(traits.key(ActionMappingTraitDefinition.SUCCESS_NOTIFICATIONS_EVENT_PROPERTY));
	}

	@Override
	public Integer getSuccessNotificationsDelay() {
		return wrappedObject.getProperty(traits.key(ActionMappingTraitDefinition.SUCCESS_NOTIFICATIONS_DELAY_PROPERTY));
	}

	@Override
	public String getFailureNotifications() {
		return wrappedObject.getProperty(traits.key(ActionMappingTraitDefinition.FAILURE_NOTIFICATIONS_PROPERTY));
	}

	@Override
	public String getFailureBehaviour() {
		return wrappedObject.getProperty(traits.key(ActionMappingTraitDefinition.FAILURE_BEHAVIOUR_PROPERTY));
	}

	@Override
	public String getFailurePartial() {
		return wrappedObject.getProperty(traits.key(ActionMappingTraitDefinition.FAILURE_PARTIAL_PROPERTY));
	}

	@Override
	public String getFailureURL() {
		return wrappedObject.getProperty(traits.key(ActionMappingTraitDefinition.FAILURE_URL_PROPERTY));
	}

	@Override
	public String getFailureEvent() {
		return wrappedObject.getProperty(traits.key(ActionMappingTraitDefinition.FAILURE_EVENT_PROPERTY));
	}

	@Override
	public String getFailureNotificationsPartial() {
		return wrappedObject.getProperty(traits.key(ActionMappingTraitDefinition.FAILURE_NOTIFICATIONS_PARTIAL_PROPERTY));
	}

	@Override
	public String getFailureNotificationsEvent() {
		return wrappedObject.getProperty(traits.key(ActionMappingTraitDefinition.FAILURE_NOTIFICATIONS_EVENT_PROPERTY));
	}

	@Override
	public Integer getFailureNotificationsDelay() {
		return wrappedObject.getProperty(traits.key(ActionMappingTraitDefinition.FAILURE_NOTIFICATIONS_DELAY_PROPERTY));
	}
}
