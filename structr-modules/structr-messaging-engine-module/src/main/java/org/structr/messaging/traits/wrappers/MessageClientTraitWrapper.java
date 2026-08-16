/*
 * Copyright (C) 2010-2026 Structr GmbH
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
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.Traits;
import org.structr.core.traits.wrappers.AbstractNodeTraitWrapper;
import org.structr.messaging.engine.entities.MessageClient;
import org.structr.messaging.engine.entities.MessageSubscriber;
import org.structr.messaging.traits.definitions.MessageClientTraitDefinition;
import org.structr.messaging.traits.operations.MessageClientOperations;
import org.structr.rest.RestMethodResult;
import org.structr.schema.action.ActionContext;

public class MessageClientTraitWrapper extends AbstractNodeTraitWrapper implements MessageClient {

	// the two spellings of the enabled flag used by the concrete client types
	private static final String IS_ENABLED_PROPERTY = "isEnabled";
	private static final String ENABLED_PROPERTY    = "enabled";

	public MessageClientTraitWrapper(final Traits traits, final NodeInterface wrappedObject) {

		super(traits, wrappedObject);
	}

	/**
	 * Whether this client is enabled. The concrete client types spell the flag differently - MQTTClient
	 * and XMPPClient declare "isEnabled", KafkaClient and PulsarClient declare "enabled" - so whichever
	 * one the type actually declares is read here. A type declaring neither, or a value that was never
	 * set, counts as not enabled rather than failing: this is called from the message engine and from
	 * the Kafka consumer loop, where reading the flag must not throw.
	 */
	public boolean getIsEnabled() {

		final PropertyKey<Boolean> key = getEnabledKey();

		return key != null && Boolean.TRUE.equals(wrappedObject.getProperty(key));
	}

	private PropertyKey<Boolean> getEnabledKey() {

		if (traits.hasKey(IS_ENABLED_PROPERTY)) {

			return traits.key(IS_ENABLED_PROPERTY);
		}

		if (traits.hasKey(ENABLED_PROPERTY)) {

			return traits.key(ENABLED_PROPERTY);
		}

		return null;
	}

	public Iterable<MessageSubscriber> getSubscribers() {

		final Iterable<NodeInterface> nodes = wrappedObject.getProperty(traits.key(MessageClientTraitDefinition.SUBSCRIBERS_PROPERTY));

		return Iterables.map(n -> n.as(MessageSubscriber.class), nodes);
	}

	public void setSubscribers(final Iterable<MessageSubscriber> subscribers) throws FrameworkException {

		wrappedObject.setProperty(traits.key(MessageClientTraitDefinition.SUBSCRIBERS_PROPERTY), subscribers);
	}

	@Override
	public final RestMethodResult sendMessage(final ActionContext actionContext, final String topic, final String message) throws FrameworkException {

		final MessageClientOperations operations = traits.getMethod(MessageClientOperations.class);
		if (operations != null) {

			return operations.sendMessage(actionContext, this, topic, message);
		}

		return null;
	}

	@Override
	public final RestMethodResult subscribeTopic(final ActionContext actionContext, final String topic) throws FrameworkException {

		final MessageClientOperations operations = traits.getMethod(MessageClientOperations.class);
		if (operations != null) {

			return operations.subscribeTopic(actionContext, this, topic);
		}

		return null;
	}

	@Override
	public final RestMethodResult unsubscribeTopic(final ActionContext actionContext, final String topic) throws FrameworkException {

		final MessageClientOperations operations = traits.getMethod(MessageClientOperations.class);
		if (operations != null) {

			return operations.unsubscribeTopic(actionContext, this, topic);
		}

		return null;
	}
}
