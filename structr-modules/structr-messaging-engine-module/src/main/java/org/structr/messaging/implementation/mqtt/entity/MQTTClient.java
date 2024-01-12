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
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyMap;
import org.structr.messaging.engine.entities.MessageClient;
import org.structr.messaging.engine.entities.MessageSubscriber;
import org.structr.messaging.implementation.mqtt.MQTTClientConnection;
import org.structr.messaging.implementation.mqtt.MQTTContext;
import org.structr.messaging.implementation.mqtt.MQTTInfo;
import org.structr.rest.RestMethodResult;
import org.structr.schema.SchemaService;

import java.net.URI;
import java.util.List;

public interface MQTTClient extends MessageClient, MQTTInfo {

	class Impl {
		static {

			final JsonSchema schema   = SchemaService.getDynamicSchema();
			final JsonObjectType type = schema.addType("MQTTClient");

			type.setImplements(URI.create("https://structr.org/v1.1/definitions/MQTTClient"));

			type.setExtends(URI.create("#/definitions/MessageClient"));

			type.addStringProperty("mainBrokerURL",             PropertyView.Public, PropertyView.Ui).setRequired(true);
			type.addStringArrayProperty("fallbackBrokerURLs",   PropertyView.Public, PropertyView.Ui);
			type.addIntegerProperty("qos",                      PropertyView.Public, PropertyView.Ui).setDefaultValue("0");
			type.addBooleanProperty("isEnabled",                PropertyView.Public, PropertyView.Ui);
			type.addBooleanProperty("isConnected",              PropertyView.Public, PropertyView.Ui).setReadOnly(true);
			type.addStringProperty("username",                  PropertyView.Public, PropertyView.Ui);
			type.addStringProperty("password",                  PropertyView.Public, PropertyView.Ui);

			type.addPropertyGetter("isConnected",         Boolean.TYPE);
			type.addPropertyGetter("isEnabled",           Boolean.TYPE);
			type.addPropertyGetter("qos",                 Integer.TYPE);
			type.addPropertyGetter("subscribers",         Iterable.class);
			type.addPropertyGetter("username",            String.class);
			type.addPropertyGetter("password",            String.class);
			type.addPropertyGetter("mainBrokerURL",       String.class);
			type.addPropertyGetter("fallbackBrokerURLs",  String[].class);

			type.addPropertySetter("isConnected", Boolean.TYPE);

			type.overrideMethod("onCreation",     true, MQTTClient.class.getName() + ".onCreation(this, arg0, arg1);");
			type.overrideMethod("onModification", true, MQTTClient.class.getName() + ".onModification(this, arg0, arg1, arg2);");
			type.overrideMethod("onDeletion",     true, MQTTClient.class.getName() + ".onDeletion(this, arg0, arg1, arg2);");

			type.overrideMethod("messageCallback",          false, MessageClient.class.getName() + ".sendMessage(this, arg0, arg1, this.getSecurityContext());");
			type.overrideMethod("connectionStatusCallback", false, MQTTClient.class.getName() + ".connectionStatusCallback(this, arg0);");
			type.overrideMethod("getTopics",                false, "return " + MQTTClient.class.getName() + ".getTopics(this);");

			type.overrideMethod("sendMessage", false, "return " + MQTTClient.class.getName() + ".sendMessage(this, topic, message, this.getSecurityContext());");
			type.overrideMethod("subscribeTopic", false, "return " + MQTTClient.class.getName() + ".subscribeTopic(this, topic);");
			type.overrideMethod("unsubscribeTopic", false, "return " + MQTTClient.class.getName() + ".unsubscribeTopic(this, topic);");


		}
	}

	boolean getIsConnected();
	int getQos();
	String getUsername();
	String getPassword();

	void setIsConnected(boolean connected) throws FrameworkException;

	static void onCreation(MQTTClient thisClient, final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

		if (thisClient.getIsEnabled()) {

			try {

				MQTTContext.connect(thisClient);

			} catch (FrameworkException ex) {

				thisClient.setProperty(StructrApp.key(MQTTClient.class, "isEnabled"), false);
				thisClient.setProperty(StructrApp.key(MQTTClient.class, "isConnected"), false);
			}
		}
	}

	static void onModification(MQTTClient thisClient, final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

		if (modificationQueue.isPropertyModified(thisClient,StructrApp.key(MQTTClient.class,"mainBrokerURL")) || modificationQueue.isPropertyModified(thisClient,StructrApp.key(MQTTClient.class,"fallbackBrokerURLs"))) {

			MQTTContext.disconnect(thisClient);
		}

		if (modificationQueue.isPropertyModified(thisClient,StructrApp.key(MQTTClient.class,"isEnabled")) || modificationQueue.isPropertyModified(thisClient,StructrApp.key(MQTTClient.class,"mainBrokerURL")) || modificationQueue.isPropertyModified(thisClient,StructrApp.key(MQTTClient.class,"fallbackBrokerURLs"))){

			MQTTClientConnection connection = MQTTContext.getClientForId(thisClient.getUuid());
			boolean enabled                 = thisClient.getIsEnabled();
			if (!enabled) {

				if (connection != null && connection.isConnected()) {

					MQTTContext.disconnect(thisClient);
					thisClient.setProperties(securityContext, new PropertyMap(StructrApp.key(MQTTClient.class,"isConnected"), false));
				}

			} else {

				if (connection == null || !connection.isConnected()) {

					MQTTContext.connect(thisClient);
					MQTTContext.subscribeAllTopics(thisClient);
				}

				connection = MQTTContext.getClientForId(thisClient.getUuid());
				if (connection != null) {

					if (connection.isConnected()) {

						thisClient.setProperties(securityContext, new PropertyMap(StructrApp.key(MQTTClient.class,"isConnected"), true));

					} else {

						thisClient.setProperties(securityContext, new PropertyMap(StructrApp.key(MQTTClient.class,"isConnected"), false));
					}
				}
			}
		}
	}

	static void onDeletion(MQTTClient thisClient, final SecurityContext securityContext, final ErrorBuffer errorBuffer, final PropertyMap properties) throws FrameworkException {

		final String uuid = properties.get(id);
		if (uuid != null) {

			MQTTContext.delete(uuid);
			final MQTTClientConnection connection = MQTTContext.getClientForId(uuid);
			if (connection != null) {

				connection.delete();
			}
		}
	}


	static void connectionStatusCallback(MQTTClient thisClient, boolean connected) {

		final App app = StructrApp.getInstance();
		try (final Tx tx = app.tx()) {

			if (!thisClient.getIsConnected() && connected) {

				MQTTContext.subscribeAllTopics(thisClient);
			}

			thisClient.setIsConnected(connected);
			tx.success();

		} catch (FrameworkException ex) {

			final Logger logger = LoggerFactory.getLogger(MQTTClient.class);
			logger.warn("Error in connection status callback for MQTTClient.");
		}
	}

	static String[] getTopics(MQTTClient thisClient) {

		final App app = StructrApp.getInstance();
		try (final Tx tx = app.tx()) {

			List<MessageSubscriber> subs = Iterables.toList(thisClient.getSubscribers());
			String[] topics = new String[subs.size()];

			for (int i = 0; i < subs.size(); i++) {

				topics[i] = subs.get(i).getTopic();
			}

			return topics;

		} catch (FrameworkException ex ) {

			final Logger logger = LoggerFactory.getLogger(MQTTClient.class);
			logger.error("Couldn't retrieve client topics for MQTT subscription.");
			return null;
		}
	}

	static RestMethodResult sendMessage(MQTTClient thisClient, final String topic, final String message, final SecurityContext ctx) throws FrameworkException {

		if (thisClient.getIsEnabled()) {

			final MQTTClientConnection connection = MQTTContext.getClientForId(thisClient.getUuid());
			if (connection != null && connection.isConnected()) {

				connection.sendMessage(topic, message);

			} else {

				throw new FrameworkException(422, "Not connected.");
			}
		}

		return new RestMethodResult(200);
	}

	static RestMethodResult subscribeTopic(MQTTClient thisClient, final String topic) throws FrameworkException {

		if (thisClient.getIsEnabled()) {

			final MQTTClientConnection connection = MQTTContext.getClientForId(thisClient.getUuid());
			if (connection != null && connection.isConnected()) {

				connection.subscribeTopic(topic);
			}
		}

		return new RestMethodResult(200);
	}


	static  RestMethodResult unsubscribeTopic(MQTTClient thisClient, final String topic) throws FrameworkException {

		if (thisClient.getIsEnabled()) {

			final MQTTClientConnection connection = MQTTContext.getClientForId(thisClient.getUuid());
			if (connection != null && connection.isConnected()) {

				connection.unsubscribeTopic(topic);
			}
		}

		return new RestMethodResult(200);
	}
}