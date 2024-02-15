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
package org.structr.messaging.implementation.pulsar;

import org.apache.pulsar.client.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonSchema;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyMap;
import org.structr.messaging.engine.entities.MessageClient;
import org.structr.messaging.engine.entities.MessageSubscriber;
import org.structr.rest.RestMethodResult;
import org.structr.schema.SchemaService;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public interface PulsarClient extends MessageClient {

	class Impl {

		static {

			final JsonSchema schema = SchemaService.getDynamicSchema();
			final JsonObjectType type = schema.addType("PulsarClient");

			type.setImplements(URI.create("https://structr.org/v1.1/definitions/PulsarClient"));

			type.setExtends(URI.create("#/definitions/MessageClient"));

			type.addStringArrayProperty("servers", PropertyView.Public, PropertyView.Ui);
			type.addBooleanProperty("enabled", PropertyView.Public, PropertyView.Ui).setDefaultValue("false");

			type.addPropertyGetter("subscribers", Iterable.class);
			type.addPropertyGetter("enabled", Boolean.class);

			type.addMethod("setServers")
				.setReturnType("void")
				.addParameter("servers", "List<String>")
				.setSource("setProperty(serversProperty, servers)")
				.addException("FrameworkException");

			type.addMethod("getServers")
				.setReturnType("List<String>")
				.setSource("return getProperty(serversProperty);");

			type.overrideMethod("onCreation", true, PulsarClient.class.getName() + ".onCreation(this, arg0, arg1);");
			type.overrideMethod("onModification", true, PulsarClient.class.getName() + ".onModification(this, arg0, arg1, arg2);");
			type.overrideMethod("onDeletion", true, PulsarClient.class.getName() + ".onDeletion(this, arg0, arg1, arg2);");

			type.overrideMethod("sendMessage", false, "return " + PulsarClient.class.getName() + ".sendMessage(this,topic,message);");
			type.overrideMethod("subscribeTopic", false, "return " + PulsarClient.class.getName() + ".subscribeTopic(this,topic);");
			type.overrideMethod("unsubscribeTopic", false, "return " + PulsarClient.class.getName() + ".unsubscribeTopic(this,topic);");

			Services.getInstance().registerInitializationCallback(() -> {

				final App app = StructrApp.getInstance();

				try (final Tx tx = app.tx()) {

					for (final PulsarClient client : app.nodeQuery(PulsarClient.class).getAsList()) {
						setup(client);
					}

					tx.success();

				} catch (Throwable t) {
					final Logger logger = LoggerFactory.getLogger(PulsarClient.class);
					logger.error("Unable to initialize Pulsar clients. " + t);
				}
			});

		}
	}

	List<String> getServers();

	Boolean getEnabled();

	void setServers(final List<String> servers) throws FrameworkException;

	Map<String, PulsarClient.ConsumerWorker> consumerWorkerMap = new ConcurrentHashMap<>();

	static void onCreation(final PulsarClient thisClient, final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {
		setup(thisClient);
	}

	static void onModification(final PulsarClient thisClient, final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

		if (modificationQueue.isPropertyModified(thisClient, StructrApp.key(PulsarClient.class, "servers"))) {
			ConsumerWorker cw = consumerWorkerMap.get(thisClient.getUuid());
			if (cw != null) {
				cw.invalidateConsumer();
			}
		}

	}

	static void onDeletion(final PulsarClient thisClient, final SecurityContext securityContext, final ErrorBuffer errorBuffer, final PropertyMap properties) throws FrameworkException {
		close(thisClient);
	}

	static RestMethodResult sendMessage(PulsarClient thisClient, final String topic, final String message) throws FrameworkException {

		if (thisClient.getServers() == null || thisClient.getServers().isEmpty()) {
			return new RestMethodResult(400, "PulsarClient " + thisClient.getUuid() + " has no servers specified");
		}

		try (org.apache.pulsar.client.api.PulsarClient pulsarClient = org.apache.pulsar.client.api.PulsarClient.builder()
			.serviceUrl(String.join(",", thisClient.getServers()))
			.build()) {

			try (Producer<byte[]> producer = pulsarClient.newProducer()
				.topic(topic)
				.messageRoutingMode(MessageRoutingMode.SinglePartition)
				.create()) {

				producer.send(message.getBytes());

				return new RestMethodResult(200);
			}

		} catch (PulsarClientException ex) {

			LoggerFactory.getLogger(PulsarClient.class).error("Exception in PulsarClient.sendMessage.", ex);
			return new RestMethodResult(500);
		}
	}

	static RestMethodResult subscribeTopic(PulsarClient thisClient, final String topic) throws FrameworkException {

		return new RestMethodResult(200);
	}

	static RestMethodResult unsubscribeTopic(PulsarClient thisClient, final String topic) throws FrameworkException {

		return new RestMethodResult(200);
	}

	static void setup(PulsarClient thisClient) {
		PulsarClient.ConsumerWorker cw = new PulsarClient.ConsumerWorker(thisClient);
		Thread t = new Thread(cw);
		consumerWorkerMap.put(thisClient.getUuid(), cw);
		t.start();
	}

	static void close(PulsarClient thisClient) {
		PulsarClient.ConsumerWorker cw = consumerWorkerMap.get(thisClient.getUuid());

		if (cw != null) {
			cw.stop();
		}
	}

	static void forwardReceivedMessage(PulsarClient thisClient, String topic, String message) throws FrameworkException {
		MessageClient.sendMessage(thisClient, topic, message, thisClient.getSecurityContext());
	}

	class ConsumerWorker implements Runnable {

		private final static Logger logger = LoggerFactory.getLogger(ConsumerWorker.class);
		private final PulsarClient thisClient;
		private org.apache.pulsar.client.api.PulsarClient pulsarClient = null;
		private Consumer consumer = null;
		private boolean running = true;
		private List<String> subbedTopics = null;

		public ConsumerWorker(PulsarClient thisClient) {

			this.thisClient = thisClient;

		}

		@Override
		public void run() {

			// wait for service layer to be initialized
			while (!Services.getInstance().isInitialized()) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException iex) {
				}
			}

			while (running) {

				try (final Tx tx = StructrApp.getInstance().tx()) {

					if (this.thisClient == null || Thread.currentThread().isInterrupted()) {
						running = false;
						break;
					}

					if (thisClient.getServers() == null || thisClient.getServers().isEmpty() || !thisClient.getEnabled()) {
						continue;
					}

					if (consumer == null) {
						consumer = createConsumer();
					}

					if (thisClient.getEnabled()) {
						updateConsumerIfTopicsHaveChanged();
					}

				} catch (FrameworkException ex) {

					logger.error("Exception in PulsarClient.ConsumerWorker. " + ex);
				}

			}

		}

		public void stop() {

			this.running = false;
			this.invalidateConsumer();
		}

		public void invalidateConsumer() {
			try {

				if (this.consumer != null) {

					this.consumer.close();
					this.consumer = null;
				}
			} catch (PulsarClientException ex) {

				logger.error("Could not close pulsar consumer. " + ex);
			}
		}

		private void updateConsumerIfTopicsHaveChanged() {
			if (subbedTopics == null) {
				return;
			}

			if (!subbedTopics.equals(getSubTopics())) {

				invalidateConsumer();
				consumer = createConsumer();
			}
		}

		private List<String> getSubTopics() {
			List<String> aggregatedTopics = new ArrayList<>();

			if (thisClient == null) {

				return aggregatedTopics;
			}

			thisClient.getSubscribers().forEach((MessageSubscriber sub) -> {
				String topic = sub.getProperty(StructrApp.key(MessageSubscriber.class, "topic"));
				if (topic != null) {
					aggregatedTopics.add(topic);
				}
			});

			return aggregatedTopics;
		}

		private Consumer createConsumer() {
			List<String> aggregatedTopics = getSubTopics();

			try {

				if (pulsarClient != null) {
					invalidateConsumer();
					pulsarClient.close();
					pulsarClient = null;
				}

				pulsarClient = org.apache.pulsar.client.api.PulsarClient.builder()
					.serviceUrl(String.join(",", thisClient.getServers()))
					.build();

				if (pulsarClient != null) {

					if (aggregatedTopics.size() > 0) {

						MessageListener messageListener = ((Consumer consumer, Message msg) -> {
							try {

								String[] topicFragments = msg.getTopicName().split("/");
								String topic = topicFragments[topicFragments.length - 1];
								forwardReceivedMessage(thisClient, topic, new String(msg.getData()));
								consumer.acknowledge(msg);
							} catch (Exception e) {

								consumer.negativeAcknowledge(msg);
							}
						});

						subbedTopics = aggregatedTopics;
						return pulsarClient.newConsumer()
							.subscriptionName("structr-pulsar-subscription")
							.topics(aggregatedTopics)
							.messageListener(messageListener)
							.subscribe();
					}

				}

			} catch (PulsarClientException ex) {

				LoggerFactory.getLogger(PulsarClient.class).error("Could not update consumer subscriptions for PulsarClient " + thisClient.getUuid() + ". " + ex);
			}

			return null;
		}
	}

}
