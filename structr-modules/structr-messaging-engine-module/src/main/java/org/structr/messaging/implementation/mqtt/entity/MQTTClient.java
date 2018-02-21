/**
 * Copyright (C) 2010-2018 Structr GmbH
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

<<<<<<< HEAD:structr-modules/structr-mqtt-module/src/main/java/org/structr/mqtt/entity/MQTTClient.java
<<<<<<< HEAD
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.cxf.common.util.StringUtils;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import static org.structr.core.GraphObject.id;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Relation.Cardinality;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
=======
import org.apache.cxf.common.util.StringUtils;
=======
>>>>>>> a6e9d11... Streamlines MQTT module into the messaging module.:structr-modules/structr-messaging-engine-module/src/main/java/org/structr/messaging/engine/implementation/mqtt/entity/MQTTClient.java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.View;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.Export;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.graph.Tx;
import org.structr.core.property.*;
import org.structr.messaging.engine.entities.MessageClient;
import org.structr.messaging.engine.entities.MessageSubscriber;
<<<<<<< HEAD:structr-modules/structr-messaging-engine-module/src/main/java/org/structr/messaging/engine/implementation/mqtt/entity/MQTTClient.java
<<<<<<< HEAD:structr-modules/structr-mqtt-module/src/main/java/org/structr/mqtt/entity/MQTTClient.java
>>>>>>> e50de8c... Fixes transaction context in MessageClient.
import org.structr.mqtt.MQTTClientConnection;
import org.structr.mqtt.MQTTContext;
import org.structr.mqtt.MQTTInfo;
=======
import org.structr.messaging.engine.implementation.mqtt.MQTTContext;
import org.structr.messaging.engine.implementation.mqtt.MQTTClientConnection;
import org.structr.messaging.engine.implementation.mqtt.MQTTInfo;
>>>>>>> a6e9d11... Streamlines MQTT module into the messaging module.:structr-modules/structr-messaging-engine-module/src/main/java/org/structr/messaging/engine/implementation/mqtt/entity/MQTTClient.java
=======
import org.structr.messaging.implementation.mqtt.MQTTContext;
import org.structr.messaging.implementation.mqtt.MQTTClientConnection;
import org.structr.messaging.implementation.mqtt.MQTTInfo;
>>>>>>> 0a011a7... Adds working proof of concept for KafkaClient implementation.:structr-modules/structr-messaging-engine-module/src/main/java/org/structr/messaging/implementation/mqtt/entity/MQTTClient.java
import org.structr.rest.RestMethodResult;
import org.structr.schema.SchemaService;
<<<<<<< HEAD
import org.structr.schema.json.JsonObjectType;
import org.structr.schema.json.JsonSchema;

public interface MQTTClient extends NodeInterface, MQTTInfo {

	static class Impl { static {

		final JsonSchema schema   = SchemaService.getDynamicSchema();
		final JsonObjectType type = schema.addType("MQTTClient");
		final JsonObjectType sub  = schema.addType("MQTTSubscriber");

		type.setImplements(URI.create("https://structr.org/v1.1/definitions/MQTTClient"));

		type.addStringProperty("protocol",     PropertyView.Public, PropertyView.Ui).setDefaultValue("tcp://");
		type.addStringProperty("url",          PropertyView.Public, PropertyView.Ui);
		type.addIntegerProperty("port",        PropertyView.Public, PropertyView.Ui);
		type.addIntegerProperty("qos",         PropertyView.Public, PropertyView.Ui).setDefaultValue("0");
		type.addBooleanProperty("isEnabled",   PropertyView.Public, PropertyView.Ui);
		type.addBooleanProperty("isConnected", PropertyView.Public, PropertyView.Ui);

		type.addPropertyGetter("isConnected", Boolean.TYPE);
		type.addPropertyGetter("isEnabled",   Boolean.TYPE);
		type.addPropertyGetter("protocol",    String.class);
		type.addPropertyGetter("url",         String.class);
		type.addPropertyGetter("port",        Integer.TYPE);
		type.addPropertyGetter("qos",         Integer.TYPE);

		type.addPropertySetter("isConnected", Boolean.TYPE);

		type.overrideMethod("onCreation",     true, "if (getProperty(isEnabledProperty)) { " + MQTTContext.class.getName() + ".connect(this); }");
		type.overrideMethod("onModification", true, MQTTClient.class.getName() + ".onModification(this, arg0, arg1, arg2);");
		type.overrideMethod("onDeletion",     true, MQTTClient.class.getName() + ".onDeletion(this, arg0, arg1, arg2);");

		type.overrideMethod("messageCallback",          false, MQTTClient.class.getName() + ".messageCallback(this, arg0, arg1);");
		type.overrideMethod("connectionStatusCallback", false, MQTTClient.class.getName() + ".connectionStatusCallback(this, arg0);");
		type.overrideMethod("getTopics",                false, "return " + MQTTClient.class.getName() + ".getTopics(this);");

		type.overrideMethod("sendMessage",      false, "return "+ MQTTClient.class.getName() + ".sendMessage(this, arg0, arg1);").setDoExport(true);
		type.overrideMethod("subscribeTopic",   false, "return "+ MQTTClient.class.getName() + ".subscribeTopic(this, arg0);").setDoExport(true);
		type.overrideMethod("unsubscribeTopic", false, "return "+ MQTTClient.class.getName() + ".unsubscribeTopic(this, arg0);").setDoExport(true);

		type.relate(sub, "HAS_SUBSCRIBER", Cardinality.OneToMany, "client", "subscribers");

		type.addViewProperty(PropertyView.Public, "subscribers");
		type.addViewProperty(PropertyView.Ui, "subscribers");

	}}

	void setIsConnected(final boolean value) throws FrameworkException;
	boolean getIsConnected();
	boolean getIsEnabled();
	RestMethodResult sendMessage(final String topic, final String message) throws FrameworkException;
	RestMethodResult subscribeTopic(final String topic) throws FrameworkException;
	RestMethodResult unsubscribeTopic(final String topic) throws FrameworkException;

	/*

	private static final Logger logger = LoggerFactory.getLogger(MQTTClient.class.getName());

	public static final Property<List<MQTTSubscriber>> subscribers= new EndNodes<>("subscribers", MQTTClientHAS_SUBSCRIBERMQTTSubscriber.class);
	public static final Property<String>               protocol    = new StringProperty("protocol").defaultValue("tcp://");
	public static final Property<String>               url         = new StringProperty("url");
	public static final Property<Integer>              port        = new IntProperty("port");
	public static final Property<Integer>              qos         = new IntProperty("qos").defaultValue(0);
	public static final Property<Boolean>              isEnabled   = new BooleanProperty("isEnabled");
	public static final Property<Boolean>              isConnected = new BooleanProperty("isConnected");
=======

import java.util.List;

public class MQTTClient extends MessageClient implements MQTTInfo{

	private static final Logger logger = LoggerFactory.getLogger(MQTTClient.class.getName());

	public static final Property<String>				protocol			= new StringProperty("protocol").defaultValue("tcp://");
	public static final Property<String>				url					= new StringProperty("url");
	public static final Property<Integer>				port				= new IntProperty("port");
	public static final Property<Integer>				qos					= new IntProperty("qos").defaultValue(0);
	public static final Property<Boolean>				isEnabled			= new BooleanProperty("isEnabled");
	public static final Property<Boolean>				isConnected			= new BooleanProperty("isConnected");
>>>>>>> e50de8c... Fixes transaction context in MessageClient.

	public static final View defaultView = new View(MQTTClient.class, PropertyView.Public, id, type, subscribers, protocol, url, port, qos, isEnabled, isConnected);

	public static final View uiView = new View(MQTTClient.class, PropertyView.Ui,
		id, name, owner, type, createdBy, deleted, hidden, createdDate, lastModifiedDate, visibleToPublicUsers, visibleToAuthenticatedUsers, visibilityStartDate, visibilityEndDate,
        subscribers, protocol, url, port, qos, isEnabled, isConnected
	);

	static {

		SchemaService.registerBuiltinTypeOverride("MQTTClient", MQTTClient.class.getName());
	}

	@Override
	public boolean onCreation(final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

		if (getProperty(isEnabled)) {

			MQTTContext.connect(this);
		}

		return super.onCreation(securityContext, errorBuffer);
	}
<<<<<<< HEAD
	*/

	static void onModification(final MQTTClient thisClient, final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

		final PropertyKey<Boolean> isEnabled = StructrApp.key(MQTTClient.class, "isEnabled");
		final PropertyKey<Integer> port      = StructrApp.key(MQTTClient.class, "port");
		final PropertyKey<String> protocol   = StructrApp.key(MQTTClient.class, "protocol");
		final PropertyKey<String> url        = StructrApp.key(MQTTClient.class, "url");

		if (modificationQueue.isPropertyModified(thisClient, protocol) || modificationQueue.isPropertyModified(thisClient, url) || modificationQueue.isPropertyModified(thisClient, port)) {

			MQTTContext.disconnect(thisClient);
		}

		if(modificationQueue.isPropertyModified(thisClient, isEnabled) || modificationQueue.isPropertyModified(thisClient, protocol) || modificationQueue.isPropertyModified(thisClient, url) || modificationQueue.isPropertyModified(thisClient, port)){

			MQTTClientConnection connection = MQTTContext.getClientForId(getUuid());
			boolean enabled                 = thisClient.getProperty(isEnabled);

=======

	@Override
	public boolean onModification(final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

		if (modificationQueue.isPropertyModified(this,protocol) || modificationQueue.isPropertyModified(this,url) || modificationQueue.isPropertyModified(this,port)) {

			MQTTContext.disconnect(this);
		}

		if(modificationQueue.isPropertyModified(this,isEnabled) || modificationQueue.isPropertyModified(this,protocol) || modificationQueue.isPropertyModified(this,url) || modificationQueue.isPropertyModified(this,port)){

			MQTTClientConnection connection = MQTTContext.getClientForId(getUuid());
			boolean enabled                 = getProperty(isEnabled);
>>>>>>> e50de8c... Fixes transaction context in MessageClient.
			if (!enabled) {

				if (connection != null && connection.isConnected()) {

<<<<<<< HEAD
					MQTTContext.disconnect(thisClient);

					thisClient.setIsConnected(false);
=======
					MQTTContext.disconnect(this);
					setProperties(securityContext, new PropertyMap(isConnected, false));
>>>>>>> e50de8c... Fixes transaction context in MessageClient.
				}

			} else {

				if (connection == null || !connection.isConnected()) {

<<<<<<< HEAD
					MQTTContext.connect(thisClient);
					MQTTContext.subscribeAllTopics(thisClient);
=======
					MQTTContext.connect(this);
					MQTTContext.subscribeAllTopics(this);
>>>>>>> e50de8c... Fixes transaction context in MessageClient.
				}

				connection = MQTTContext.getClientForId(getUuid());
				if (connection != null) {

					if (connection.isConnected()) {

<<<<<<< HEAD
						thisClient.setIsConnected(true);

					} else {

						thisClient.setIsConnected(false);
=======
						setProperties(securityContext, new PropertyMap(isConnected, true));
					} else {

						setProperties(securityContext, new PropertyMap(isConnected, false));
>>>>>>> e50de8c... Fixes transaction context in MessageClient.
					}
				}
			}
		}
<<<<<<< HEAD
	}

	static void onDeletion(final MQTTClient thisClient, final SecurityContext securityContext, final ErrorBuffer errorBuffer, final PropertyMap properties) throws FrameworkException {
=======

		return super.onModification(securityContext, errorBuffer, modificationQueue);
	}

	@Override
	public boolean onDeletion(final SecurityContext securityContext, final ErrorBuffer errorBuffer, final PropertyMap properties) throws FrameworkException {
>>>>>>> e50de8c... Fixes transaction context in MessageClient.

		final String uuid = properties.get(id);
		if (uuid != null) {

			final MQTTClientConnection connection = MQTTContext.getClientForId(uuid);
			if (connection != null) {

				connection.disconnect();
			}
		}
<<<<<<< HEAD
	}

	static void messageCallback(final MQTTClient thisClient, final String topic, final String message) {


		final App app = StructrApp.getInstance();
		try (final Tx tx = app.tx()) {

			final List<MQTTSubscriber> subs = thisClient.getProperty(StructrApp.key(MQTTClient.class, "subscribers"));

			for(MQTTSubscriber sub : subs) {

				final String subTopic = sub.getTopic();
				if(!StringUtils.isEmpty(subTopic)) {

					if(subTopic.equals(topic)){

						Map<String,Object> params = new HashMap<>();
						params.put("topic", topic);
						params.put("message", message);

						try {

							sub.invokeMethod("onMessage", params, false);
						} catch (FrameworkException ex) {

							logger.warn("Error while calling onMessage callback for MQTT subscriber.");
						}
					}
				}
			}

			tx.success();
		} catch (FrameworkException ex) {

			logger.error("Could not handle message callback for MQTT subscription.");
		}

	}

	static void connectionStatusCallback(final MQTTClient thisClient, final boolean connected) {
=======

		return super.onDeletion(securityContext, errorBuffer, properties);
	}

	@Override
	public String getProtocol() {
		return getProperty(MQTTClient.protocol);
	}

	@Override
	public String getUrl() {
		return getProperty(MQTTClient.url);
	}

	@Override
	public int getPort() {
		return getProperty(MQTTClient.port);
	}

	@Override
	public int getQoS() {
		return getProperty(MQTTClient.qos);
	}

	@Override
	public void messageCallback(String topic, String message) throws FrameworkException{
		super.sendMessage(topic, message);
	}

	@Override
	public void connectionStatusCallback(boolean connected) {
>>>>>>> e50de8c... Fixes transaction context in MessageClient.

		final App app = StructrApp.getInstance();
		try(final Tx tx = app.tx()) {

<<<<<<< HEAD
			thisClient.setIsConnected(connected);
			tx.success();

=======
			setProperties(securityContext, new PropertyMap(isConnected, connected));
			tx.success();
>>>>>>> e50de8c... Fixes transaction context in MessageClient.
		} catch (FrameworkException ex) {

			logger.warn("Error in connection status callback for MQTTClient.");
		}

	}

<<<<<<< HEAD
	static String[] getTopics(final MQTTClient thisClient) {
=======
	@Override
	public String[] getTopics() {
>>>>>>> e50de8c... Fixes transaction context in MessageClient.

		final App app = StructrApp.getInstance();
		try (final Tx tx = app.tx()) {

<<<<<<< HEAD
			final List<MQTTSubscriber> subs = thisClient.getProperty(StructrApp.key(MQTTClient.class, "subscribers"));
=======
			List<MessageSubscriber> subs = getProperty(subscribers);
>>>>>>> e50de8c... Fixes transaction context in MessageClient.
			String[] topics = new String[subs.size()];

			for(int i = 0; i < subs.size(); i++) {

<<<<<<< HEAD
				topics[i] = subs.get(i).getTopic();
			}

			tx.success();

			return topics;

=======
				topics[i] = subs.get(i).getProperty(MessageSubscriber.topic);
			}

			return topics;
>>>>>>> e50de8c... Fixes transaction context in MessageClient.
		} catch (FrameworkException ex ) {

			logger.error("Couldn't retrieve client topics for MQTT subscription.");
			return null;
		}

	}

<<<<<<< HEAD
	static RestMethodResult sendMessage(final MQTTClient thisClient, final String topic, final String message) throws FrameworkException {

		if (thisClient.getIsEnabled()) {

			final MQTTClientConnection connection = MQTTContext.getClientForId(thisClient.getUuid());
=======
	@Export
	public RestMethodResult sendMessage(final String topic, final String message) throws FrameworkException {

		if (getProperty(isEnabled)) {

			final MQTTClientConnection connection = MQTTContext.getClientForId(getUuid());
>>>>>>> e50de8c... Fixes transaction context in MessageClient.
			if (connection.isConnected()) {

				connection.sendMessage(topic, message);

			} else {

				throw new FrameworkException(422, "Not connected.");
			}
		}

		return new RestMethodResult(200);
	}

<<<<<<< HEAD
	static RestMethodResult subscribeTopic(final MQTTClient thisClient, final String topic) throws FrameworkException {

		if (thisClient.getIsEnabled()) {

			final MQTTClientConnection connection = MQTTContext.getClientForId(thisClient.getUuid());
=======
	@Export
	public RestMethodResult subscribeTopic(final String topic) throws FrameworkException {

		if (getProperty(isEnabled)) {

			final MQTTClientConnection connection = MQTTContext.getClientForId(getUuid());
<<<<<<< HEAD:structr-modules/structr-messaging-engine-module/src/main/java/org/structr/messaging/engine/implementation/mqtt/entity/MQTTClient.java
>>>>>>> e50de8c... Fixes transaction context in MessageClient.
			if (connection.isConnected()) {
=======
			if (connection != null && connection.isConnected()) {
>>>>>>> 0a011a7... Adds working proof of concept for KafkaClient implementation.:structr-modules/structr-messaging-engine-module/src/main/java/org/structr/messaging/implementation/mqtt/entity/MQTTClient.java

				connection.subscribeTopic(topic);

			}

		}

		return new RestMethodResult(200);
	}

<<<<<<< HEAD
	static RestMethodResult unsubscribeTopic(final MQTTClient thisClient, final String topic) throws FrameworkException {

		if (thisClient.getIsEnabled()) {

			final MQTTClientConnection connection = MQTTContext.getClientForId(thisClient.getUuid());
=======
	@Export
	public RestMethodResult unsubscribeTopic(final String topic) throws FrameworkException {

		if (getProperty(isEnabled)) {

			final MQTTClientConnection connection = MQTTContext.getClientForId(getUuid());
<<<<<<< HEAD:structr-modules/structr-messaging-engine-module/src/main/java/org/structr/messaging/engine/implementation/mqtt/entity/MQTTClient.java
>>>>>>> e50de8c... Fixes transaction context in MessageClient.
			if (connection.isConnected()) {
=======
			if (connection != null && connection.isConnected()) {
>>>>>>> 0a011a7... Adds working proof of concept for KafkaClient implementation.:structr-modules/structr-messaging-engine-module/src/main/java/org/structr/messaging/implementation/mqtt/entity/MQTTClient.java

				connection.unsubscribeTopic(topic);

			}

		}

		return new RestMethodResult(200);
	}
<<<<<<< HEAD
=======

>>>>>>> e50de8c... Fixes transaction context in MessageClient.
}
