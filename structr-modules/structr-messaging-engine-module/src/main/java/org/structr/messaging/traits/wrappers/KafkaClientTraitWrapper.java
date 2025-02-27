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

import com.google.gson.JsonSyntaxException;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.common.KafkaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.NotInTransactionException;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.script.Scripting;
import org.structr.core.traits.Traits;
import org.structr.messaging.engine.entities.MessageSubscriber;
import org.structr.messaging.implementation.kafka.entity.KafkaClient;
import org.structr.schema.action.ActionContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class KafkaClientTraitWrapper extends MessageClientTraitWrapper implements KafkaClient {

	private final Map<String, Producer<String, String>> producerMap = new ConcurrentHashMap<>();
	private final Map<String, ConsumerWorker> consumerWorkerMap = new ConcurrentHashMap<>();

	public KafkaClientTraitWrapper(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	public String getGroupId() {
		return getProperty(traits.key("groupId"));
	}

	public String[] getServers() {
		return getProperty(traits.key("servers"));
	}

	public void setServers(final String[] servers) throws FrameworkException {
		setProperty(traits.key("servers"), servers);
	}

	public Iterable<MessageSubscriber> getSubscribers() {
		return getProperty(traits.key("subscribers"));
	}

	static {

		Services.getInstance().registerInitializationCallback(() -> {

			final App app = StructrApp.getInstance();

			try (final Tx tx = app.tx()) {

				for (final NodeInterface client : app.nodeQuery("KafkaClient").getAsList()) {

					client.as(KafkaClient.class).setup();
				}

				tx.success();

			} catch (Throwable t) {
				final Logger logger = LoggerFactory.getLogger(KafkaClientTraitWrapper.class);
				logger.error("Unable to initialize Kafka clients. " + t);
			}
		});
	}

	public Producer<String, String> getProducer() {
		return producerMap.get(getUuid());
	}

	public void setProducer(KafkaProducer<String, String> producer) {
		producerMap.put(getUuid(), producer);
	}

	public void setup() {

		ConsumerWorker cw = new ConsumerWorker(this);
		Thread t = new Thread(cw);
		consumerWorkerMap.put(this.getUuid(), cw);
		t.start();
	}

	public void close() {

		if (getProducer() != null) {
			getProducer().close();
		}

		ConsumerWorker cw = consumerWorkerMap.get(this.getUuid());

		if (cw != null) {
			cw.stop();
		}

	}

	public void refreshConfiguration() {
		try {
			if (this.getServers() != null && this.getServers().length > 0) {
				setProducer(new KafkaProducer<>(getConfiguration(KafkaProducer.class)));
			}
		} catch (JsonSyntaxException | KafkaException ex) {
			final Logger logger = LoggerFactory.getLogger(KafkaClientTraitWrapper.class);
			logger.error("Could not refresh Kafka configuration. " + ex);
		}
	}

	public void forwardReceivedMessage(final String topic, final String message) throws FrameworkException {
		sendMessage(getSecurityContext(), topic, message);
	}

	public Properties getConfiguration(final Class clazz) {

		Properties props = new Properties();

		try {
			String[] servers = this.getServers();
			if (servers != null) {
				props.setProperty("bootstrap.servers", String.join(",", servers));
			} else {
				props.setProperty("bootstrap.servers", "");
			}

			if (clazz == KafkaProducer.class) {
				props.put("acks", "all");
				props.put("retries", 0);
				props.put("batch.size", 16384);
				props.put("linger.ms", 1);
				props.put("buffer.memory", 33554432);
				props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
				props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
			} else if (clazz == KafkaConsumer.class) {

				String gId = null;

				if (this.getGroupId() != null) {

					gId = Scripting.replaceVariables(new ActionContext(SecurityContext.getSuperUserInstance()), null, this.getGroupId(), false, "groupId");
				} else {
					gId = this.getGroupId();
				}

				if (gId != null && gId.length() > 0) {
					props.put("group.id", gId);
				} else {
					props.put("group.id", "structr-" + this.getUuid());
				}
				props.put("enable.auto.commit", "true");
				props.put("auto.commit.interval.ms", "1000");
				props.put("session.timeout.ms", "30000");
				props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
				props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
			} else {
				return null;
			}

		} catch (FrameworkException ex) {

			final Logger logger = LoggerFactory.getLogger(KafkaClientTraitWrapper.class);
			logger.error("Exception while trying to generate KafkaClient configuration. " + ex);
		}

		return props;
	}

	public class ConsumerWorker implements Runnable {

		private final Logger logger = LoggerFactory.getLogger(ConsumerWorker.class.getName());
		private KafkaClient client;
		private KafkaConsumer<String, String> consumer;
		private List<String> currentlySubscribedTopics;
		private String currentGroupId;
		private volatile boolean running;

		public ConsumerWorker(final KafkaClient client) {
			this.currentlySubscribedTopics = null;
			this.currentGroupId = null;
			this.client = client;
			this.consumer = null;
			this.running = true;
			logger.info("Started ConsumerWorker for id: " + client.getUuid() + (client.getName() != null ? " name:" + client.getName() : ""));
		}

		public void stop() {
			this.running = false;
		}

		public void refreshConsumer() {

			try {

				if (consumer != null) {
					consumer.close();
				}
				this.consumer = new KafkaConsumer<>(getConfiguration(KafkaConsumer.class));

				if (client.getGroupId() != null) {
					this.currentGroupId = Scripting.replaceVariables(new ActionContext(SecurityContext.getSuperUserInstance()), null, client.getGroupId(), false, "groupId");
				} else {
					this.currentGroupId = client.getGroupId();
				}

			} catch (KafkaException ex) {

				logger.info("Could not setup consumer for KafkaClient " + client.getUuid() + ", triggered by ConsumerWorker Thread. Check for configuration faults. " + ex);
				try {
					Thread.sleep(1000);
				} catch (InterruptedException iex) {
				}

			} catch (FrameworkException ex) {

				logger.error("Could not refresh KafkaConsumer configuration. " + ex);
			}
		}

		public boolean newGroupId() {
			try {
				String cId = null;

				if (client.getGroupId() != null) {
					cId = Scripting.replaceVariables(new ActionContext(SecurityContext.getSuperUserInstance()), null, client.getGroupId(), false, "groupId");
				} else {
					cId = client.getGroupId();
				}

				return !currentGroupId.equals(cId);

			} catch (FrameworkException ex) {

				logger.error("Exception while trying to evaluate KafkaClient groupId. " + ex);
				return false;
			}
		}

		public void updateSubscriptions(boolean forceUpdate) {

			List<String> newTopics = new ArrayList<>();

			if (this.consumer != null) {
				try {
					client.getSubscribers().forEach((MessageSubscriber sub) -> {
						String topic = sub.getTopic();
						if (topic != null) {
							newTopics.add(topic);
						}
					});

					if (!forceUpdate && currentlySubscribedTopics != null && !currentlySubscribedTopics.equals(newTopics)) {
						if (this.consumer.subscription().size() > 0) {
							this.consumer.unsubscribe();
						}

						this.consumer.subscribe(newTopics);
						this.currentlySubscribedTopics = newTopics;
					} else if (forceUpdate || currentlySubscribedTopics == null) {
						if (this.consumer.subscription().size() > 0) {
							this.consumer.unsubscribe();
						}

						this.consumer.subscribe(newTopics);
						this.currentlySubscribedTopics = newTopics;
					}

				} catch (KafkaException ex) {
					logger.error("Could not update consumer subscriptions for KafkaClient " + client.getUuid() + ", triggered by ConsumerWorker Thread. " + ex);
				}
			}


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

			final App app = StructrApp.getInstance();

			boolean wasDisabled = true;

			while (running) {

				try (final Tx tx = app.tx()) {

					if (this.client == null || Thread.currentThread().isInterrupted()) {
						running = false;
						break;
					}

					if (this.client.getServers() != null && this.client.getServers().length > 0 && this.client.getIsEnabled()) {

						if (this.consumer == null || wasDisabled) {

							this.refreshConsumer();
							this.updateSubscriptions(true);
							wasDisabled = false;

						} else {

							if (newGroupId()) {
								this.refreshConsumer();
								logger.info("New groupId for KafkaClient " + this.client.getUuid() + ", updating ConsumerWorker..");
								this.updateSubscriptions(true);
							} else {
								this.updateSubscriptions(false);
							}

							if (this.consumer.subscription().size() > 0) {
								final ConsumerRecords<String, String> records = this.consumer.poll(1000);

								records.forEach(record -> {
									try {
										forwardReceivedMessage(record.topic(), record.value());
									} catch (FrameworkException e) {
										logger.error("Could not process records in ConsumerWorker: " + e);
									}
								});

							} else {
								wasDisabled = true;
								try {
									Thread.sleep(1000);
								} catch (InterruptedException iex) {
								}
								// Wait for subscriptions
							}

						}
					} else {
						try {
							Thread.sleep(1000);
						} catch (InterruptedException iex) {
							// Wait for servers to be configured
						}
					}

					tx.success();

				} catch (FrameworkException | NotInTransactionException ex) {
					// Terminate thread since client became stale or invalid
					logger.error("Exception in ConsumerWorker for KafkaClient. " + ex);
				} catch (IllegalStateException ex) {
					// Main thread has shut down driver, since this worker only does reads, we can safely shutdown
				}

			}

			this.consumer.close();

		}

	}


}
