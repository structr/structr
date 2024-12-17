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
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.Traits;
import org.structr.core.traits.wrappers.AbstractTraitWrapper;
import org.structr.web.entity.event.ActionMapping;
import org.structr.web.entity.event.ParameterMapping;

public class ActionMappingTraitWrapper extends AbstractTraitWrapper<NodeInterface> implements ActionMapping {

	public ActionMappingTraitWrapper(final Traits traits, final NodeInterface node) {
		super(traits, node);
	}

	@Override
	public String getOptions() {
		return wrappedObject.getProperty(traits.key("options"));
	}

	@Override
	public String getAction() {
		return wrappedObject.getProperty(traits.key("action"));
	}

	@Override
	public String getDialogType() {
		return wrappedObject.getProperty(traits.key("dialogType"));
	}

	@Override
	public Iterable<ParameterMapping> getParameterMappings() {

		final PropertyKey<Iterable<NodeInterface>> key = traits.key("parameterMappings");

		return Iterables.map(n -> n.as(ParameterMapping.class), wrappedObject.getProperty(key));
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
