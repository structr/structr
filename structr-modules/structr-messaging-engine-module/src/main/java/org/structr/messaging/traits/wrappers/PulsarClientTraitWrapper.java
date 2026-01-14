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
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.Traits;
import org.structr.messaging.engine.entities.MessageSubscriber;
import org.structr.messaging.traits.definitions.PulsarClientTraitDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PulsarClientTraitWrapper extends MessageClientTraitWrapper implements org.structr.messaging.implementation.pulsar.PulsarClient {

	private final Map<String, PulsarClientTraitWrapper.ConsumerWorker> consumerWorkerMap = new ConcurrentHashMap<>();

	public PulsarClientTraitWrapper(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
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

		PulsarClientTraitWrapper.ConsumerWorker cw = new PulsarClientTraitWrapper.ConsumerWorker(this);
		Thread t = new Thread(cw);
		consumerWorkerMap.put(this.getUuid(), cw);
		t.start();
	}

	@Override
	public void close() {

		PulsarClientTraitWrapper.ConsumerWorker cw = consumerWorkerMap.get(this.getUuid());

		if (cw != null) {
			cw.stop();
		}
	}

	@Override
	public void forwardReceivedMessage(final SecurityContext securityContext, String topic, String message) throws FrameworkException {
		sendMessage(securityContext, topic, message);
	}

	@Override
	public ConsumerWorker getConsumerWorker() {
		return consumerWorkerMap.get(this.getUuid());
	}

	public class ConsumerWorker implements Runnable {

		private final static Logger logger = LoggerFactory.getLogger(ConsumerWorker.class);
		private final PulsarClientTraitWrapper thisClient;
		private PulsarClient pulsarClient = null;
		private Consumer consumer = null;
		private boolean running = true;
		private List<String> subbedTopics = null;

		public ConsumerWorker(PulsarClientTraitWrapper thisClient) {

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
				String topic = sub.getTopic();
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

				pulsarClient = PulsarClient.builder()
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

				LoggerFactory.getLogger(PulsarClientTraitWrapper.class).error("Could not update consumer subscriptions for PulsarClient " + thisClient.getUuid() + ". " + ex);
			}

			return null;
		}
	}

}
