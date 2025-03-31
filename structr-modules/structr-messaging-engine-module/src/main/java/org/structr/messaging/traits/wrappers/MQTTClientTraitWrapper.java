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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.util.Iterables;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.Traits;
import org.structr.messaging.engine.entities.MessageSubscriber;
import org.structr.messaging.implementation.mqtt.MQTTContext;
import org.structr.messaging.implementation.mqtt.entity.MQTTClient;
import org.structr.messaging.traits.definitions.MQTTClientTraitDefinition;

import java.util.List;

public class MQTTClientTraitWrapper extends MessageClientTraitWrapper implements MQTTClient {

	public MQTTClientTraitWrapper(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	@Override
	public boolean getEnabled() {
		return wrappedObject.getProperty(traits.key(MQTTClientTraitDefinition.IS_ENABLED_PROPERTY));
	}

	@Override
	public void setEnabled(final boolean enabled) throws FrameworkException {
		wrappedObject.setProperty(traits.key(MQTTClientTraitDefinition.IS_ENABLED_PROPERTY), enabled);
	}

	public boolean getIsConnected() {
		return getProperty(traits.key(MQTTClientTraitDefinition.IS_CONNECTED_PROPERTY));
	}

	public int getQos() {
		return getProperty(traits.key(MQTTClientTraitDefinition.QOS_PROPERTY));
	}

	public String getUsername() {
		return getProperty(traits.key(MQTTClientTraitDefinition.USERNAME_PROPERTY));
	}

	public String getPassword() {
		return getProperty(traits.key(MQTTClientTraitDefinition.PASSWORD_PROPERTY));
	}

	public void setIsConnected(boolean connected) throws FrameworkException {
		setProperty(traits.key(MQTTClientTraitDefinition.IS_CONNECTED_PROPERTY), connected);
	}

	public String getMainBrokerURL() {
		return getProperty(traits.key(MQTTClientTraitDefinition.MAIN_BROKER_URL_PROPERTY));
	}

	public String[] getFallbackBrokerURLs() {
		return getProperty(traits.key(MQTTClientTraitDefinition.FALLBACK_BROKER_URLS_PROPERTY));
	}

	@Override
	public void connectionStatusCallback(boolean connected) {

		final App app = StructrApp.getInstance();
		try (final Tx tx = app.tx()) {

			if (!this.getIsConnected() && connected) {

				MQTTContext.subscribeAllTopics(this);
			}

			this.setIsConnected(connected);
			tx.success();

		} catch (FrameworkException ex) {

			final Logger logger = LoggerFactory.getLogger(MQTTClientTraitWrapper.class);
			logger.warn("Error in connection status callback for MQTTClient.");
		}
	}

	public String[] getTopics() {

		final App app = StructrApp.getInstance();
		try (final Tx tx = app.tx()) {

			List<MessageSubscriber> subs = Iterables.toList(this.getSubscribers());
			String[] topics = new String[subs.size()];

			for (int i = 0; i < subs.size(); i++) {

				topics[i] = subs.get(i).getTopic();
			}

			tx.success();

			return topics;

		} catch (FrameworkException ex ) {

			final Logger logger = LoggerFactory.getLogger(MQTTClientTraitWrapper.class);
			logger.error("Couldn't retrieve client topics for MQTT subscription.");
			return null;
		}
	}

	@Override
	public void messageCallback(String topic, String message) throws FrameworkException {
		sendMessage(getSecurityContext(), topic, message);
	}
}