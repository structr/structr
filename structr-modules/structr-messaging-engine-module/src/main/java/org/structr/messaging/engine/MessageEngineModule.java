/*
 * Copyright (C) 2010-2025 Structr GmbH
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
package org.structr.messaging.engine;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.service.LicenseManager;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractSchemaNode;
import org.structr.core.function.Functions;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.messaging.engine.entities.MessageClient;
import org.structr.messaging.engine.entities.MessageSubscriber;
import org.structr.messaging.engine.relation.MessageClientHASMessageSubscriber;
import org.structr.messaging.implementation.kafka.entity.KafkaClient;
import org.structr.messaging.implementation.mqtt.entity.MQTTClient;
import org.structr.messaging.implementation.mqtt.function.MQTTPublishFunction;
import org.structr.messaging.implementation.mqtt.function.MQTTSubscribeTopicFunction;
import org.structr.messaging.implementation.mqtt.function.MQTTUnsubscribeTopicFunction;
import org.structr.messaging.implementation.pulsar.PulsarClient;
import org.structr.messaging.traits.definitions.*;
import org.structr.module.StructrModule;
import org.structr.schema.SourceFile;
import org.structr.schema.action.Actions;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class MessageEngineModule implements StructrModule {

	private static final Logger logger = LoggerFactory.getLogger(MessageEngineModule.class.getName());

	@Override
	public void onLoad() {

		StructrTraits.registerRelationshipType(StructrTraits.MESSAGE_CLIENT_HAS_MESSAGE_SUBSCRIBER, new MessageClientHASMessageSubscriber());

		StructrTraits.registerNodeType(StructrTraits.MESSAGE_CLIENT,     new MessageClientTraitDefinition());
		StructrTraits.registerNodeType(StructrTraits.MESSAGE_SUBSCRIBER, new MessageSubscriberTraitDefinition());
		StructrTraits.registerNodeType(StructrTraits.KAFKA_CLIENT,       new MessageClientTraitDefinition(), new KafkaClientTraitDefinition());
		StructrTraits.registerNodeType(StructrTraits.MQTT_CLIENT,        new MessageClientTraitDefinition(), new MQTTClientTraitDefinition());
		StructrTraits.registerNodeType(StructrTraits.PULSAR_CLIENT,      new MessageClientTraitDefinition(), new PulsarClientTraitDefinition());
	}

	@Override
	public void registerModuleFunctions(final LicenseManager licenseManager) {

		Functions.put(licenseManager, new MQTTPublishFunction());
		Functions.put(licenseManager, new MQTTSubscribeTopicFunction());
		Functions.put(licenseManager, new MQTTUnsubscribeTopicFunction());
	}

	@Override
	public String getName() {
		return "messaging-module";
	}

	@Override
	public Set<String> getDependencies() {
		return Set.of("ui");
	}

	@Override
	public Set<String> getFeatures() {
		return null;
	}

	@Override
	public void insertImportStatements(final AbstractSchemaNode schemaNode, final SourceFile buf) {
		// nothing to do
	}

	@Override
	public void insertSourceCode(final AbstractSchemaNode schemaNode, final SourceFile buf) {
		// nothing to do
	}

	@Override
	public Set<String> getInterfacesForType(final AbstractSchemaNode schemaNode) {
		return null;
	}

	@Override
	public void insertSaveAction(final AbstractSchemaNode schemaNode, final SourceFile buf, final Actions.Type type) {
		// nothing to do
	}

	@Override
	public boolean hasDeploymentData() {
		return true;
	}

	@Override
	public void exportDeploymentData(final Path target, final Gson gson) throws FrameworkException {

		final Path messagingEngineFile    = target.resolve("messaging-engine.json");
		final Traits subscriberTraits     = Traits.of(StructrTraits.MESSAGE_SUBSCRIBER);
		final Traits clientTraits         = Traits.of(StructrTraits.MESSAGE_CLIENT);
		final PropertyKey<String> nameKey = Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY);
		final App app                     = StructrApp.getInstance();


		final List<Map<String, Object>> entities = new LinkedList();
		try (final Tx tx = app.tx()) {

			for (final NodeInterface subscriberNode : app.nodeQuery(StructrTraits.MESSAGE_SUBSCRIBER).sort(nameKey).getAsList()) {

				final MessageSubscriber sub     = subscriberNode.as(MessageSubscriber.class);
				final Map<String, Object> entry = new TreeMap<>();

				entry.put(GraphObjectTraitDefinition.TYPE_PROPERTY, sub.getType());
				entry.put(GraphObjectTraitDefinition.ID_PROPERTY, sub.getUuid());
				entry.put(NodeInterfaceTraitDefinition.NAME_PROPERTY, sub.getName());
				entry.put(MessageSubscriberTraitDefinition.TOPIC_PROPERTY, sub.getTopic());
				entry.put(MessageSubscriberTraitDefinition.CALLBACK_PROPERTY, sub.getCallback());

				entities.add(entry);
			}


			for (final NodeInterface clientNode : app.nodeQuery(StructrTraits.MESSAGE_CLIENT).type(StructrTraits.MESSAGE_CLIENT).sort(nameKey).getAsList()) {

				final MessageClient client      = clientNode.as(MessageClient.class);
				final Map<String, Object> entry = new TreeMap<>();

				entry.put(GraphObjectTraitDefinition.TYPE_PROPERTY,   client.getType());
				entry.put(NodeInterfaceTraitDefinition.NAME_PROPERTY, client.getName());

				List<String> subIds = new ArrayList<>();

				for (MessageSubscriber sub : client.getSubscribers()) {
					subIds.add(sub.getUuid());
				}

				entry.put(MessageClientTraitDefinition.SUBSCRIBERS_PROPERTY, subIds);

				entities.add(entry);

			}

			for (final NodeInterface clientNode : app.nodeQuery(StructrTraits.MQTT_CLIENT).sort(nameKey).getAsList()) {

				final MQTTClient client = clientNode.as(MQTTClient.class);
				final Map<String, Object> entry = new TreeMap<>();

				entry.put(GraphObjectTraitDefinition.TYPE_PROPERTY, client.getType());
				entry.put(NodeInterfaceTraitDefinition.NAME_PROPERTY, client.getName());

				entry.put(MQTTClientTraitDefinition.MAIN_BROKER_URL_PROPERTY, client.getMainBrokerURL());
				entry.put(MQTTClientTraitDefinition.FALLBACK_BROKER_URLS_PROPERTY, client.getFallbackBrokerURLs());
				entry.put(MQTTClientTraitDefinition.USERNAME_PROPERTY, client.getUsername());
				entry.put(MQTTClientTraitDefinition.PASSWORD_PROPERTY, client.getPassword());
				entry.put(MQTTClientTraitDefinition.QOS_PROPERTY, client.getQos());
				entry.put(MQTTClientTraitDefinition.IS_ENABLED_PROPERTY, client.getIsEnabled());

				List<String> subIds = new ArrayList<>();
				for (MessageSubscriber sub : client.getSubscribers()) {
					subIds.add(sub.getUuid());
				}

				entry.put(MessageClientTraitDefinition.SUBSCRIBERS_PROPERTY, subIds);

				entities.add(entry);

			}

			for (final NodeInterface clientNode : app.nodeQuery(StructrTraits.KAFKA_CLIENT).sort(nameKey).getAsList()) {

				final KafkaClient client = clientNode.as(KafkaClient.class);
				final Map<String, Object> entry = new TreeMap<>();

				entry.put(GraphObjectTraitDefinition.TYPE_PROPERTY, client.getType());
				entry.put(NodeInterfaceTraitDefinition.NAME_PROPERTY, client.getName());

				entry.put(KafkaClientTraitDefinition.SERVERS_PROPERTY, client.getServers());
				entry.put(KafkaClientTraitDefinition.GROUP_ID_PROPERTY, client.getGroupId());
				entry.put(KafkaClientTraitDefinition.ENABLED_PROPERTY, client.getIsEnabled());

				List<String> subIds = new ArrayList<>();
				for (MessageSubscriber sub : client.getSubscribers()) {
					subIds.add(sub.getUuid());
				}

				entry.put(MessageClientTraitDefinition.SUBSCRIBERS_PROPERTY, subIds);

				entities.add(entry);

			}

			for (final NodeInterface clientNode : app.nodeQuery(StructrTraits.PULSAR_CLIENT).sort(nameKey).getAsList()) {

				final PulsarClient client = clientNode.as(PulsarClient.class);
				final Map<String, Object> entry = new TreeMap<>();

				entry.put(GraphObjectTraitDefinition.TYPE_PROPERTY, client.getType());
				entry.put(NodeInterfaceTraitDefinition.NAME_PROPERTY, client.getName());

				entry.put(PulsarClientTraitDefinition.SERVERS_PROPERTY, client.getServers());
				entry.put(PulsarClientTraitDefinition.ENABLED_PROPERTY, client.getEnabled());

				List<String> subIds = new ArrayList<>();
				for (MessageSubscriber sub : client.getSubscribers()) {
					subIds.add(sub.getUuid());
				}

				entry.put(MessageClientTraitDefinition.SUBSCRIBERS_PROPERTY, subIds);

				entities.add(entry);

			}

			tx.success();
		}

		try (final Writer fos = new OutputStreamWriter(new FileOutputStream(messagingEngineFile.toFile()))) {

			gson.toJson(entities, fos);

		} catch (IOException ioex) {
			logger.warn("", ioex);
		}
	}

	@Override
	public void importDeploymentData(final Path source, final Gson gson) throws FrameworkException {

		final Path messagingEngineConf = source.resolve("messaging-engine.json");
		if (Files.exists(messagingEngineConf)) {

			logger.info("Reading {}..", messagingEngineConf);

			try (final Reader reader = Files.newBufferedReader(messagingEngineConf, Charset.forName("utf-8"))) {

				final List<Map<String, Object>> entities = gson.fromJson(reader, List.class);

				final SecurityContext context = SecurityContext.getSuperUserInstance();
				context.setDoTransactionNotifications(false);

				final App app = StructrApp.getInstance(context);

				try (final Tx tx = app.tx()) {

					for (final NodeInterface toDelete : app.nodeQuery(StructrTraits.MESSAGE_CLIENT).getAsList()) {
						app.delete(toDelete);
					}

					for (final NodeInterface toDelete : app.nodeQuery(StructrTraits.KAFKA_CLIENT).getAsList()) {
						app.delete(toDelete);
					}

					for (final NodeInterface toDelete : app.nodeQuery(StructrTraits.PULSAR_CLIENT).getAsList()) {
						app.delete(toDelete);
					}

					for (final NodeInterface toDelete : app.nodeQuery(StructrTraits.MQTT_CLIENT).getAsList()) {
						app.delete(toDelete);
					}

					for (final NodeInterface toDelete : app.nodeQuery(StructrTraits.MESSAGE_SUBSCRIBER).getAsList()) {
						app.delete(toDelete);
					}

					for (final Map<String, Object> entry : entities) {

						List<String> subIds = null;
						if (entry.containsKey(MessageClientTraitDefinition.SUBSCRIBERS_PROPERTY)) {
							subIds = (List<String>) entry.get(MessageClientTraitDefinition.SUBSCRIBERS_PROPERTY);
							entry.remove(MessageClientTraitDefinition.SUBSCRIBERS_PROPERTY);
						}

						final PropertyMap map;
						MessageClient client;
						switch ((String) entry.get(GraphObjectTraitDefinition.TYPE_PROPERTY)) {
							case StructrTraits.MESSAGE_CLIENT:
								map = PropertyMap.inputTypeToJavaType(context, StructrTraits.MESSAGE_CLIENT, entry);
								client = app.create(StructrTraits.MESSAGE_CLIENT, map).as(MessageClient.class);
								client.setSubscribers(getSubscribersByIds(subIds));
								break;
							case StructrTraits.KAFKA_CLIENT:
								map = PropertyMap.inputTypeToJavaType(context, StructrTraits.KAFKA_CLIENT, entry);
								client = app.create(StructrTraits.KAFKA_CLIENT, map).as(MessageClient.class);
								client.setSubscribers(getSubscribersByIds(subIds));
								break;
							case StructrTraits.PULSAR_CLIENT:
								map = PropertyMap.inputTypeToJavaType(context, StructrTraits.PULSAR_CLIENT, entry);
								client = app.create(StructrTraits.PULSAR_CLIENT, map).as(MessageClient.class);
								client.setSubscribers(getSubscribersByIds(subIds));
								break;
							case StructrTraits.MQTT_CLIENT:
								map = PropertyMap.inputTypeToJavaType(context, StructrTraits.MQTT_CLIENT, entry);
								client = app.create(StructrTraits.MQTT_CLIENT, map).as(MessageClient.class);
								client.setSubscribers(getSubscribersByIds(subIds));
								break;
							case StructrTraits.MESSAGE_SUBSCRIBER:
								map = PropertyMap.inputTypeToJavaType(context, StructrTraits.MESSAGE_SUBSCRIBER, entry);
								app.create(StructrTraits.MESSAGE_SUBSCRIBER, map);
								break;
						}

					}

					tx.success();
				}

			} catch (IOException ioex) {
				logger.warn("", ioex);
			}
		}

	}

	private List<MessageSubscriber> getSubscribersByIds(List<String> ids) {

		final List<MessageSubscriber> result = new ArrayList<>();

		if (ids != null && ids.size() > 0) {

			final App app = StructrApp.getInstance();
			try (Tx tx = app.tx()) {

				for (final String id : ids) {
					MessageSubscriber sub = (MessageSubscriber) app.getNodeById(StructrTraits.MESSAGE_SUBSCRIBER, id);
					result.add(sub);
				}

				tx.success();

			} catch (FrameworkException ex) {

				logger.error("MessageEngineModule: getSubscribersById error while getting subs by id: " + ex.getMessage());
			}

		}

		return result;
	}

}
