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

public class ActionMappingTraitWrapper extends AbstractNodeTraitWrapper implements ActionMapping {

	public ActionMappingTraitWrapper(final Traits traits, final NodeInterface node) {
		super(traits, node);
	}

	@Override
	public String getOptions() {
		return wrappedObject.getProperty(traits.key("options"));
	}

	@Override
	public String getEvent() {
		return wrappedObject.getProperty(traits.key("event"));
	}

	@Override
	public String getAction() {
		return wrappedObject.getProperty(traits.key("action"));
	}

	@Override
	public void setAction(final String action) throws FrameworkException {
		wrappedObject.setProperty(traits.key("action"), action);
	}

	@Override
	public String getMethod() {
		return wrappedObject.getProperty(traits.key("method"));
	}

	@Override
	public void setMethod(final String method) throws FrameworkException {
		wrappedObject.setProperty(traits.key("method"), method);
	}

	@Override
	public String getDataType() {
		return wrappedObject.getProperty(traits.key("dataType"));
	}

	@Override
	public String getIdExpression() {
		return wrappedObject.getProperty(traits.key("idExpression"));
	}

	@Override
	public String getDialogType() {
		return wrappedObject.getProperty(traits.key("dialogType"));
	}

	@Override
	public String getDialogTitle() {
		return wrappedObject.getProperty(traits.key("dialogTitle"));
	}

	@Override
	public String getDialogText() {
		return wrappedObject.getProperty(traits.key("dialogText"));
	}

	@Override
	public Iterable<ParameterMapping> getParameterMappings() {

		final PropertyKey<Iterable<NodeInterface>> key = traits.key("parameterMappings");

		return Iterables.map(n -> n.as(ParameterMapping.class), wrappedObject.getProperty(key));
	}

	@Override
	public Iterable<DOMElement> getTriggerElements() {

		final PropertyKey<Iterable<NodeInterface>> key = traits.key("triggerElements");

		return Iterables.map(n -> n.as(DOMElement.class), wrappedObject.getProperty(key));
	}

	@Override
	public Iterable<DOMNode> getSuccessTargets() {

		final PropertyKey<Iterable<NodeInterface>> key = traits.key("successTargets");

		return Iterables.map(n -> n.as(DOMNode.class), wrappedObject.getProperty(key));
	}

	@Override
	public Iterable<DOMNode> getFailureTargets() {

		final PropertyKey<Iterable<NodeInterface>> key = traits.key("failureTargets");

		return Iterables.map(n -> n.as(DOMNode.class), wrappedObject.getProperty(key));
	}

	@Override
	public Iterable<DOMNode> getSuccessNotificationElements() {

		final PropertyKey<Iterable<NodeInterface>> key = traits.key("successNotificationElements");

		return Iterables.map(n -> n.as(DOMNode.class), wrappedObject.getProperty(key));
	}

	@Override
	public Iterable<DOMNode> getFailureNotificationElements() {

		final PropertyKey<Iterable<NodeInterface>> key = traits.key("failureNotificationElements");

		return Iterables.map(n -> n.as(DOMNode.class), wrappedObject.getProperty(key));
	}

	@Override
	public String getSuccessNotifications() {
		return wrappedObject.getProperty(traits.key("successNotifications"));
	}

	@Override
	public String getSuccessBehaviour() {
		return wrappedObject.getProperty(traits.key("successBehaviour"));
	}

	@Override
	public String getSuccessPartial() {
		return wrappedObject.getProperty(traits.key("successPartial"));
	}

	@Override
	public String getSuccessURL() {
		return wrappedObject.getProperty(traits.key("successURL"));
	}

	@Override
	public String getSuccessEvent() {
		return wrappedObject.getProperty(traits.key("successEvent"));
	}

	@Override
	public String getSuccessNotificationsPartial() {
		return wrappedObject.getProperty(traits.key("successNotificationsPartial"));
	}

	@Override
	public String getSuccessNotificationsEvent() {
		return wrappedObject.getProperty(traits.key("successNotificationsEvent"));
	}

	@Override
	public Integer getSuccessNotificationsDelay() {
		return wrappedObject.getProperty(traits.key("successNotificationsDelay"));
	}

	@Override
	public String getFailureNotifications() {
		return wrappedObject.getProperty(traits.key("failureNotifications"));
	}

	@Override
	public String getFailureBehaviour() {
		return wrappedObject.getProperty(traits.key("failureBehaviour"));
	}

	@Override
	public String getFailurePartial() {
		return wrappedObject.getProperty(traits.key("failurePartial"));
	}

	@Override
	public String getFailureURL() {
		return wrappedObject.getProperty(traits.key("failureURL"));
	}

	@Override
	public String getFailureEvent() {
		return wrappedObject.getProperty(traits.key("failureEvent"));
	}

	@Override
	public String getFailureNotificationsPartial() {
		return wrappedObject.getProperty(traits.key("failureNotificationsPartial"));
	}

	@Override
	public String getFailureNotificationsEvent() {
		return wrappedObject.getProperty(traits.key("failureNotificationsEvent"));
	}

	@Override
	public Integer getFailureNotificationsDelay() {
		return wrappedObject.getProperty(traits.key("failureNotificationsDelay"));
	}
}
