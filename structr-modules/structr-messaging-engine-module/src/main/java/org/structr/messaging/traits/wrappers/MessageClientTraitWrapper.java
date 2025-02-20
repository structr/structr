/*
 * Copyright (C) 2010-2024 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.messaging.traits.wrappers;

import org.structr.api.util.Iterables;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.Traits;
import org.structr.core.traits.wrappers.AbstractNodeTraitWrapper;
import org.structr.messaging.engine.entities.MessageClient;
import org.structr.messaging.engine.entities.MessageSubscriber;
import org.structr.messaging.traits.operations.MessageClientOperations;
import org.structr.rest.RestMethodResult;

public class MessageClientTraitWrapper extends AbstractNodeTraitWrapper implements MessageClient {

	public MessageClientTraitWrapper(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	public boolean getIsEnabled() {
		return wrappedObject.getProperty(traits.key("isEnabled"));
	}

	public Iterable<MessageSubscriber> getSubscribers() {

		final Iterable<NodeInterface> nodes = wrappedObject.getProperty(traits.key("subscribers"));

		return Iterables.map(n -> n.as(MessageSubscriber.class), nodes);
	}

	public void setSubscribers(final Iterable<MessageSubscriber> subscribers) throws FrameworkException {
		wrappedObject.setProperty(traits.key("subscribers"), subscribers);
	}

	@Override
	public final RestMethodResult sendMessage(final SecurityContext securityContext, final String topic, final String message) throws FrameworkException {

		final MessageClientOperations operations = traits.getMethod(MessageClientOperations.class);
		if (operations != null) {

			return operations.sendMessage(securityContext, this, topic, message);
		}

		return null;
	}

	@Override
	public final RestMethodResult subscribeTopic(final SecurityContext securityContext, final String topic) throws FrameworkException {

		final MessageClientOperations operations = traits.getMethod(MessageClientOperations.class);
		if (operations != null) {

			return operations.subscribeTopic(securityContext, this, topic);
		}

		return null;
	}

	@Override
	public final RestMethodResult unsubscribeTopic(final SecurityContext securityContext, final String topic) throws FrameworkException {

		final MessageClientOperations operations = traits.getMethod(MessageClientOperations.class);
		if (operations != null) {

			return operations.unsubscribeTopic(securityContext, this, topic);
		}

		return null;
	}
}
