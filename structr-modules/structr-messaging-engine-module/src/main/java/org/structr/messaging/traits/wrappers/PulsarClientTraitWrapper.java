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

import org.apache.pulsar.client.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.messaging.engine.AfterCommit;
import org.structr.messaging.engine.entities.MessageSubscriber;
import org.structr.messaging.traits.definitions.PulsarClientTraitDefinition;
import org.structr.schema.action.ActionContext;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PulsarClientTraitWrapper extends MessageClientTraitWrapper implements org.structr.messaging.implementation.pulsar.PulsarClient {

	// static, because every transaction creates new wrapper instances, and close() must find the worker that setup() started
	private static final Map<String, PulsarClientTraitWrapper.ConsumerWorker> consumerWorkerMap = new ConcurrentHashMap<>();

	public PulsarClientTraitWrapper(final Traits traits, final NodeInterface wrappedObject) {

		super(traits, wrappedObject);
	}

	/**
	 * Registers the boot-time initialization callback that starts the consumers of the existing clients. Called from
	 * {@code MessageEngineModule.onLoad()}, see MQTTContext.registerInitializationCallback().
	 */
	public static void registerInitializationCallback() {

		Services.getInstance().registerInitializationCallback(() -> {

			if (Traits.exists(StructrTraits.PULSAR_CLIENT)) {

				final App app = StructrApp.getInstance();

				try (final Tx tx = app.tx()) {

					for (final NodeInterface client : app.nodeQuery(StructrTraits.PULSAR_CLIENT).getAsList()) {

						client.as(org.structr.messaging.implementation.pulsar.PulsarClient.class).setup();
					}

					tx.success();

				} catch (Throwable t) {

					LoggerFactory.getLogger(PulsarClientTraitWrapper.class).error("Unable to initialize Pulsar clients. " + t);
				}
			}
		});
	}

	public boolean getEnabled() {

		return wrappedObject.getProperty(traits.key(PulsarClientTraitDefinition.ENABLED_PROPERTY));
	}

	@Override
	public void setEnabled(final boolean enabled) throws FrameworkException {

		wrappedObject.setProperty(traits.key(PulsarClientTraitDefinition.ENABLED_PROPERTY), enabled);
	}

	@Override
	public String[] getServers() {

		return wrappedObject.getProperty(traits.key(PulsarClientTraitDefinition.SERVERS_PROPERTY));
	}

	public void setServers(final String[] servers) throws FrameworkException {

		wrappedObject.setProperty(traits.key(PulsarClientTraitDefinition.SERVERS_PROPERTY), servers);
	}

	@Override
	public void setup() {

		final PulsarClientTraitWrapper.ConsumerWorker cw = new PulsarClientTraitWrapper.ConsumerWorker(this.getUuid());

		consumerWorkerMap.put(this.getUuid(), cw);

		// started once the client is committed, so that a rolled back or repeated transaction does not leave a
		// consumer running
		AfterCommit.run(() -> new Thread(cw).start());
	}

	@Override
	public void close() {

		final PulsarClientTraitWrapper.ConsumerWorker cw = consumerWorkerMap.remove(this.getUuid());
		if (cw != null) {

			cw.stop();
		}
	}

	@Override
	public void forwardReceivedMessage(final ActionContext actionContext, String topic, String message) throws FrameworkException {

		deliverToSubscribers(actionContext, topic, message);
	}

	@Override
	public ConsumerWorker getConsumerWorker() {

		return consumerWorkerMap.get(this.getUuid());
	}

	/**
	 * Keeps a Pulsar consumer in line with its client: it creates the consumer when the client is enabled and has
	 * servers and subscribers, recreates it when the servers or the topics change, closes it when the client is
	 * disabled, and stops when the client is deleted. The client is read anew in each round, because an instance from
	 * an earlier transaction would not see the changes.
	 */
	public static class ConsumerWorker implements Runnable {

		private final static Logger logger = LoggerFactory.getLogger(ConsumerWorker.class);
		private final String clientId;
		private volatile org.structr.messaging.implementation.pulsar.PulsarClient thisClient = null;
		private volatile Consumer consumer                                                  = null;
		private volatile boolean running                                                    = true;
		private PulsarClient pulsarClient                                                   = null;
		private List<String> subbedTopics                                                   = null;
		private String subbedServiceUrl                                                     = null;

		public ConsumerWorker(final String clientId) {

			this.clientId = clientId;
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

				if (Thread.currentThread().isInterrupted()) {

					running = false;
					break;
				}

				try {

					try (final Tx tx = StructrApp.getInstance().tx()) {

						update();

						tx.success();
					}

				} catch (FrameworkException | RuntimeException ex) {

					// the next round reads the client again
					logger.error("Exception in PulsarClient.ConsumerWorker. " + ex);
				}

				try {

					Thread.sleep(1000);

				} catch (InterruptedException iex) {

					running = false;
				}
			}

			closeClient();
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

		/**
		 * Brings the consumer in line with the current state of the client.
		 */
		private void update() throws FrameworkException {

			final NodeInterface node = StructrApp.getInstance().getNodeById(StructrTraits.PULSAR_CLIENT, clientId);
			if (node == null) {

				logger.info("PulsarClient {} was deleted, stopping its ConsumerWorker", clientId);

				running = false;

				return;
			}

			thisClient = node.as(org.structr.messaging.implementation.pulsar.PulsarClient.class);

			final String[] servers = thisClient.getServers();
			if (servers == null || servers.length == 0 || !thisClient.getIsEnabled()) {

				closeClient();

				return;
			}

			final String serviceUrl  = String.join(",", servers);
			final List<String> topics = getSubTopics();

			if (consumer == null || !serviceUrl.equals(subbedServiceUrl) || !topics.equals(subbedTopics)) {

				closeClient();

				consumer = createConsumer(serviceUrl, topics);
			}
		}

		private List<String> getSubTopics() {

			List<String> aggregatedTopics = new ArrayList<>();

			thisClient.getSubscribers().forEach((MessageSubscriber sub) -> {
				String topic = sub.getTopic();
				if (topic != null) {

					aggregatedTopics.add(topic);
				}
			});

			return aggregatedTopics;
		}

		private void closeClient() {

			invalidateConsumer();

			try {

				if (pulsarClient != null) {

					pulsarClient.close();
				}

			} catch (PulsarClientException ex) {

				logger.error("Could not close pulsar client. " + ex);
			}

			pulsarClient     = null;
			subbedTopics     = null;
			subbedServiceUrl = null;
		}

		/**
		 * Creates the consumer for the given topics, or returns null if there are none.
		 */
		private Consumer createConsumer(final String serviceUrl, final List<String> topics) {

			if (topics.isEmpty()) {

				return null;
			}

			try {

				pulsarClient = PulsarClient.builder()
					.serviceUrl(serviceUrl)
					.build();

				MessageListener messageListener = ((Consumer consumer, Message msg) -> {
					try {

						String[] topicFragments = msg.getTopicName().split("/");
						String topic = topicFragments[topicFragments.length - 1];
						final org.structr.messaging.implementation.pulsar.PulsarClient client = thisClient;

						client.forwardReceivedMessage(new ActionContext(client.getSecurityContext()), topic, new String(msg.getData(), StandardCharsets.UTF_8));
						consumer.acknowledge(msg);

					} catch (Exception e) {

						consumer.negativeAcknowledge(msg);
					}
				});

				final Consumer newConsumer = pulsarClient.newConsumer()
					.subscriptionName("structr-pulsar-subscription")
					.topics(topics)
					.messageListener(messageListener)
					.subscribe();

				subbedTopics     = topics;
				subbedServiceUrl = serviceUrl;

				return newConsumer;

			} catch (PulsarClientException ex) {

				logger.error("Could not update consumer subscriptions for PulsarClient " + clientId + ". " + ex);
			}

			return null;
		}
	}
}
