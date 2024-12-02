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
package org.structr.messaging.implementation.mqtt.entity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonSchema;
import org.structr.api.util.Iterables;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.View;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.helper.ValidationHelper;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.graph.Tx;
import org.structr.core.property.*;
import org.structr.messaging.engine.entities.MessageClient;
import org.structr.messaging.engine.entities.MessageSubscriber;
import org.structr.messaging.implementation.mqtt.MQTTClientConnection;
import org.structr.messaging.implementation.mqtt.MQTTContext;
import org.structr.messaging.implementation.mqtt.MQTTInfo;
import org.structr.rest.RestMethodResult;
import org.structr.schema.SchemaService;

import java.net.URI;
import java.util.List;

public class MQTTClient extends MessageClient implements MQTTInfo {

	public static final Property<String> mainBrokerURLProperty        = new StringProperty("mainBrokerURL").notNull();
	public static final Property<String[]> fallbackBrokerURLsProperty = new ArrayProperty<>("fallbackBrokerURLs", String.class);
	public static final Property<Integer> qosProperty                 = new IntProperty("qos").defaultValue(0);
	public static final Property<Boolean> isEnabledProperty           = new BooleanProperty("isEnabled");
	public static final Property<Boolean> isConnectedProperty         = new BooleanProperty("isConnected").readOnly();
	public static final Property<String> usernameProperty             = new StringProperty("username");
	public static final Property<String> passwordProperty             = new StringProperty("password");

	public static final View defaultView = new View(MQTTClient.class, PropertyView.Public,
		mainBrokerURLProperty, fallbackBrokerURLsProperty, qosProperty, isEnabledProperty, isConnectedProperty, usernameProperty, passwordProperty
	);

	public static final View uiView = new View(MQTTClient.class, PropertyView.Ui,
		mainBrokerURLProperty, fallbackBrokerURLsProperty, qosProperty, isEnabledProperty, isConnectedProperty, usernameProperty, passwordProperty
	);

	@Override
	public boolean isValid(final ErrorBuffer errorBuffer) {

		boolean valid = super.isValid(errorBuffer);

		valid &= ValidationHelper.isValidPropertyNotNull(this, MQTTClient.mainBrokerURLProperty, errorBuffer);

		return valid;
	}

	public boolean getIsEnabled() {
		return getProperty(isEnabledProperty);
	}

	public boolean getIsConnected() {
		return getProperty(isConnectedProperty);
	}

	public int getQos() {
		return getProperty(qosProperty);
	}

	public String getUsername() {
		return getProperty(usernameProperty);
	}

	public String getPassword() {
		return getProperty(passwordProperty);
	}

	void setIsConnected(boolean connected) throws FrameworkException {
		setProperty(isConnectedProperty, connected);
	}

	public String getMainBrokerURL() {
		return getProperty(mainBrokerURLProperty);
	}

	public String[] getFallbackBrokerURLs() {
		return getProperty(fallbackBrokerURLsProperty);
	}

	@Override
	public void onCreation(final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

		super.onCreation(securityContext, errorBuffer);

		if (this.getIsEnabled()) {

			try {

				MQTTContext.connect(this);

			} catch (FrameworkException ex) {

				this.setProperty(StructrApp.key(MQTTClient.class, "isEnabled"), false);
				this.setProperty(StructrApp.key(MQTTClient.class, "isConnected"), false);
			}
		}
	}

	@Override
	public void onModification(final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

		super.onModification(securityContext, errorBuffer, modificationQueue);

		if (modificationQueue.isPropertyModified(this,StructrApp.key(MQTTClient.class,"mainBrokerURL")) || modificationQueue.isPropertyModified(this,StructrApp.key(MQTTClient.class,"fallbackBrokerURLs"))) {

			MQTTContext.disconnect(this);
		}

		if (modificationQueue.isPropertyModified(this,StructrApp.key(MQTTClient.class,"isEnabled")) || modificationQueue.isPropertyModified(this,StructrApp.key(MQTTClient.class,"mainBrokerURL")) || modificationQueue.isPropertyModified(this,StructrApp.key(MQTTClient.class,"fallbackBrokerURLs"))){

			MQTTClientConnection connection = MQTTContext.getClientForId(this.getUuid());
			boolean enabled                 = this.getIsEnabled();
			if (!enabled) {

				if (connection != null && connection.isConnected()) {

					MQTTContext.disconnect(this);
					this.setProperties(securityContext, new PropertyMap(StructrApp.key(MQTTClient.class,"isConnected"), false));
				}

			} else {

				if (connection == null || !connection.isConnected()) {

					MQTTContext.connect(this);
					MQTTContext.subscribeAllTopics(this);
				}

				connection = MQTTContext.getClientForId(this.getUuid());
				if (connection != null) {

					if (connection.isConnected()) {

						this.setProperties(securityContext, new PropertyMap(StructrApp.key(MQTTClient.class,"isConnected"), true));

					} else {

						this.setProperties(securityContext, new PropertyMap(StructrApp.key(MQTTClient.class,"isConnected"), false));
					}
				}
			}
		}
	}

	@Override
	public void onDeletion(final SecurityContext securityContext, final ErrorBuffer errorBuffer, final PropertyMap properties) throws FrameworkException {

		super.onDeletion(securityContext, errorBuffer, properties);

		final String uuid = properties.get(id);
		if (uuid != null) {

			MQTTContext.delete(uuid);
			final MQTTClientConnection connection = MQTTContext.getClientForId(uuid);
			if (connection != null) {

				connection.delete();
			}
		}
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

			final Logger logger = LoggerFactory.getLogger(MQTTClient.class);
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

			final Logger logger = LoggerFactory.getLogger(MQTTClient.class);
			logger.error("Couldn't retrieve client topics for MQTT subscription.");
			return null;
		}
	}

	@Override
	public void messageCallback(String topic, String message) throws FrameworkException {
		sendMessage(getSecurityContext(), topic, message);
	}

	@Override
	public RestMethodResult sendMessage(final SecurityContext securityContext, final String topic, final String message) throws FrameworkException {

		if (this.getIsEnabled()) {

			final MQTTClientConnection connection = MQTTContext.getClientForId(this.getUuid());
			if (connection != null && connection.isConnected()) {

				connection.sendMessage(topic, message);

			} else {

				throw new FrameworkException(422, "Not connected.");
			}
		}

		return new RestMethodResult(200);
	}

	@Override
	public RestMethodResult subscribeTopic(final SecurityContext securityContext, final String topic) throws FrameworkException {

		if (this.getIsEnabled()) {

			final MQTTClientConnection connection = MQTTContext.getClientForId(this.getUuid());
			if (connection != null && connection.isConnected()) {

				connection.subscribeTopic(topic);
			}
		}

		return new RestMethodResult(200);
	}


	@Override
	public  RestMethodResult unsubscribeTopic(final SecurityContext securityContext, final String topic) throws FrameworkException {

		if (this.getIsEnabled()) {

			final MQTTClientConnection connection = MQTTContext.getClientForId(this.getUuid());
			if (connection != null && connection.isConnected()) {

				connection.unsubscribeTopic(topic);
			}
		}

		return new RestMethodResult(200);
	}
}