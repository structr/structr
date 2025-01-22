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
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.View;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.Export;
import org.structr.core.Services;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.graph.Tx;
import org.structr.core.property.ArrayProperty;
import org.structr.core.property.BooleanProperty;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyMap;
import org.structr.messaging.engine.entities.MessageClient;
import org.structr.messaging.engine.entities.MessageSubscriber;
import org.structr.rest.RestMethodResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PulsarClient extends MessageClient {

	public static final Property<String[]> serversProperty = new ArrayProperty("servers", String.class);
	public static final Property<Boolean> enabledProperty = new BooleanProperty("enabled").defaultValue(false);

	public static final View defaultView = new View(PulsarClient.class, PropertyView.Public,
		serversProperty, enabledProperty, subscribersProperty
	);

	public static final View uiView = new View(PulsarClient.class, PropertyView.Ui,
		serversProperty, enabledProperty, subscribersProperty
	);

	public String[] getServers() {
		return getProperty(serversProperty);
	}

	public Boolean getEnabled() {
		return getProperty(enabledProperty);
	}

	public void setServers(final String[] servers) throws FrameworkException {
		setProperty(serversProperty, servers);
	}

	public Iterable<MessageSubscriber> getSubscribers() {
		return getProperty(subscribersProperty);
	}

	static {

		Services.getInstance().registerInitializationCallback(() -> {

			final App app = StructrApp.getInstance();

			try (final Tx tx = app.tx()) {

				for (final PulsarClient client : app.nodeQuery("PulsarClient").getAsList()) {
					client.setup();
				}

				tx.success();

			} catch (Throwable t) {
				final Logger logger = LoggerFactory.getLogger(PulsarClient.class);
				logger.error("Unable to initialize Pulsar clients. " + t);
			}
		});

	}

	private final Map<String, PulsarClient.ConsumerWorker> consumerWorkerMap = new ConcurrentHashMap<>();

	@Override
	public void onCreation(final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

		super.onCreation(securityContext, errorBuffer);

		setup();
	}

	@Override
	public void onModification(final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

		super.onModification(securityContext, errorBuffer, modificationQueue);

		if (modificationQueue.isPropertyModified(this, StructrApp.key(PulsarClient.class, "servers"))) {
			ConsumerWorker cw = consumerWorkerMap.get(this.getUuid());
			if (cw != null) {
				cw.invalidateConsumer();
			}
		}

	}

	public void onDeletion(final SecurityContext securityContext, final ErrorBuffer errorBuffer, final PropertyMap properties) throws FrameworkException {

		super.onDeletion(securityContext, errorBuffer, properties);

		close();
	}

	@Export
	@Override
	public RestMethodResult sendMessage(final SecurityContext securityContext, final String topic, final String message) throws FrameworkException {

		if (this.getServers() == null || this.getServers().length == 0) {
			return new RestMethodResult(400, "PulsarClient " + this.getUuid() + " has no servers specified");
		}

		try (org.apache.pulsar.client.api.PulsarClient pulsarClient = org.apache.pulsar.client.api.PulsarClient.builder()
			.serviceUrl(String.join(",", this.getServers()))
			.build()
		) {

			try (Producer<byte[]> producer = pulsarClient.newProducer()
				.topic(topic)
				.messageRoutingMode(MessageRoutingMode.SinglePartition)
				.create()
			) {

				producer.send(message.getBytes());

				return new RestMethodResult(200);
			}

		} catch (PulsarClientException ex) {

			LoggerFactory.getLogger(PulsarClient.class).error("Exception in PulsarClient.sendMessage.", ex);
			return new RestMethodResult(500);
		}
	}

	@Export
	public RestMethodResult subscribeTopic(final SecurityContext securityContext, final String topic) throws FrameworkException {
		return new RestMethodResult(200);
	}

	@Export
	public RestMethodResult unsubscribeTopic(final SecurityContext securityContext, final String topic) throws FrameworkException {

		return new RestMethodResult(200);
	}

	public void setup() {

		PulsarClient.ConsumerWorker cw = new PulsarClient.ConsumerWorker(this);
		Thread t = new Thread(cw);
		consumerWorkerMap.put(this.getUuid(), cw);
		t.start();
	}

	public void close() {

		PulsarClient.ConsumerWorker cw = consumerWorkerMap.get(this.getUuid());

		if (cw != null) {
			cw.stop();
		}
	}

	public void forwardReceivedMessage(final SecurityContext securityContext, String topic, String message) throws FrameworkException {
		sendMessage(securityContext, topic, message);
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

					if (thisClient.getServers() == null || thisClient.getServers().length == 0 || !thisClient.getEnabled()) {
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
								forwardReceivedMessage(getSecurityContext(), topic, new String(msg.getData()));
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
