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
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyMap;
import org.structr.messaging.engine.entities.MessageClient;
import org.structr.messaging.engine.entities.MessageSubscriber;
import org.structr.messaging.implementation.kafka.entity.KafkaClient;
import org.structr.messaging.implementation.mqtt.entity.MQTTClient;
import org.structr.messaging.implementation.mqtt.function.MQTTPublishFunction;
import org.structr.messaging.implementation.mqtt.function.MQTTSubscribeTopicFunction;
import org.structr.messaging.implementation.mqtt.function.MQTTUnsubscribeTopicFunction;
import org.structr.messaging.implementation.pulsar.PulsarClient;
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
    public void onLoad(LicenseManager licenseManager) {
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
        return null;
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
    public boolean hasDeploymentData () {
        return true;
    }

    @Override
	public void exportDeploymentData (final Path target, final Gson gson) throws FrameworkException {
		final App app = StructrApp.getInstance();
		final Path messagingEngineFile = target.resolve("messaging-engine.json");


		final List<Map<String, Object>> entities = new LinkedList();
		try (final Tx tx = app.tx()) {

			for (final MessageSubscriber sub : app.nodeQuery(MessageSubscriber.class).sort(MessageSubscriber.name).getAsList()) {

				final Map<String, Object> entry = new TreeMap<>();

				entry.put("type",sub.getType());
				entry.put("id", sub.getUuid());
				entry.put("name", sub.getName());
				entry.put("topic", sub.getTopic());
				entry.put("callback", sub.getCallback());

				entities.add(entry);
			}


			for (final MessageClient client : app.nodeQuery(MessageClient.class).andType(MessageClient.class).and("type", "MessageClient").sort(MessageClient.name).getAsList()) {

				final Map<String, Object> entry = new TreeMap<>();

				entry.put("type",client.getType());
				entry.put("name", client.getName());

				List<String> subIds = new ArrayList<>();
				for (MessageSubscriber sub : client.getSubscribers()) {
					subIds.add(sub.getUuid());
				}

				entry.put("subscribers", subIds);

				entities.add(entry);

			}

			for (final MQTTClient client : app.nodeQuery(MQTTClient.class).andType(MQTTClient.class).sort(MQTTClient.name).getAsList()) {

				final Map<String, Object> entry = new TreeMap<>();

				entry.put("type",client.getType());
				entry.put("name", client.getName());

				entry.put("mainBrokerURL", client.getMainBrokerURL());
				entry.put("fallbackBrokerURLs", client.getFallbackBrokerURLs());
				entry.put("username", client.getUsername());
				entry.put("password", client.getPassword());
				entry.put("qos", client.getQos());
				entry.put("isEnabled", client.getIsEnabled());

				List<String> subIds = new ArrayList<>();
				for (MessageSubscriber sub : client.getSubscribers()) {
					subIds.add(sub.getUuid());
				}

				entry.put("subscribers", subIds);

				entities.add(entry);

			}

			for (final KafkaClient client : app.nodeQuery(KafkaClient.class).andType(KafkaClient.class).sort(KafkaClient.name).getAsList()) {

				final Map<String, Object> entry = new TreeMap<>();

				entry.put("type",client.getType());
				entry.put("name", client.getProperty(KafkaClient.name));

				entry.put("servers", client.getServers());
				entry.put("groupId", client.getGroupId());
				entry.put("enabled", client.getEnabled());

				List<String> subIds = new ArrayList<>();
				for (MessageSubscriber sub : client.getSubscribers()) {
					subIds.add(sub.getUuid());
				}

				entry.put("subscribers", subIds);

				entities.add(entry);

			}

			for (final PulsarClient client : app.nodeQuery(PulsarClient.class).andType(PulsarClient.class).sort(PulsarClient.name).getAsList()) {

				final Map<String, Object> entry = new TreeMap<>();

				entry.put("type",client.getType());
				entry.put("name", client.getProperty(KafkaClient.name));

				entry.put("servers", client.getServers());
				entry.put("enabled", client.getEnabled());

				List<String> subIds = new ArrayList<>();
				for (MessageSubscriber sub : client.getSubscribers()) {
					subIds.add(sub.getUuid());
				}

				entry.put("subscribers", subIds);

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
	public void importDeploymentData (final Path source, final Gson gson) throws FrameworkException {

		final Path messagingEngineConf = source.resolve("messaging-engine.json");
		if (Files.exists(messagingEngineConf)) {

			logger.info("Reading {}..", messagingEngineConf);

			try (final Reader reader = Files.newBufferedReader(messagingEngineConf, Charset.forName("utf-8"))) {

				final List<Map<String, Object>> entities = gson.fromJson(reader, List.class);

				final SecurityContext context = SecurityContext.getSuperUserInstance();
				context.setDoTransactionNotifications(false);

				final App app = StructrApp.getInstance(context);

				try (final Tx tx = app.tx()) {

					for (final MessageClient toDelete : app.nodeQuery(MessageClient.class).getAsList()) {
						app.delete(toDelete);
					}

					for (final KafkaClient toDelete : app.nodeQuery(KafkaClient.class).getAsList()) {
						app.delete(toDelete);
					}

					for (final PulsarClient toDelete : app.nodeQuery(PulsarClient.class).getAsList()) {
						app.delete(toDelete);
					}

					for (final MQTTClient toDelete : app.nodeQuery(MQTTClient.class).getAsList()) {
						app.delete(toDelete);
					}

					for (final MessageSubscriber toDelete : app.nodeQuery(MessageSubscriber.class).getAsList()) {
						app.delete(toDelete);
					}

					for (final Map<String, Object> entry : entities) {

						List<String> subIds = null;
						if (entry.containsKey("subscribers")) {
							subIds = (List<String>)entry.get("subscribers");
							entry.remove("subscribers");
						}

						final PropertyMap map;
						MessageClient client;
						switch ((String)entry.get("type")) {
							case "MessageClient":
								map = PropertyMap.inputTypeToJavaType(context, MessageClient.class, entry);
								client = app.create(MessageClient.class, map);
								client.setSubscribers(getSubscribersByIds(subIds));
								break;
							case "KafkaClient":
								map = PropertyMap.inputTypeToJavaType(context, KafkaClient.class, entry);
								client = app.create(KafkaClient.class, map);
								client.setSubscribers(getSubscribersByIds(subIds));
								break;
							case "PulsarClient":
								map = PropertyMap.inputTypeToJavaType(context, PulsarClient.class, entry);
								client = app.create(PulsarClient.class, map);
								client.setSubscribers(getSubscribersByIds(subIds));
								break;
							case "MQTTClient":
								map = PropertyMap.inputTypeToJavaType(context, MQTTClient.class, entry);
								client = app.create(MQTTClient.class, map);
								client.setSubscribers(getSubscribersByIds(subIds));
								break;
							case "MessageSubscriber":
								map = PropertyMap.inputTypeToJavaType(context, MessageSubscriber.class, entry);
								app.create(MessageSubscriber.class, map);
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
					MessageSubscriber sub = (MessageSubscriber) app.getNodeById(MessageSubscriber.class, id);
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
