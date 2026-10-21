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

import com.google.gson.JsonSyntaxException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.common.KafkaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.NotInTransactionException;
import org.structr.api.RetryException;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.script.Scripting;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.messaging.engine.AfterCommit;
import org.structr.messaging.engine.entities.MessageSubscriber;
import org.structr.messaging.implementation.kafka.entity.KafkaClient;
import org.structr.messaging.traits.definitions.KafkaClientTraitDefinition;
import org.structr.schema.action.ActionContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class KafkaClientTraitWrapper extends MessageClientTraitWrapper implements KafkaClient {

	// static, because every transaction creates new wrapper instances: an instance map would create a new producer for
	// each message and never find the worker that setup() started
	private static final Map<String, Producer<String, String>> producerMap = new ConcurrentHashMap<>();
	private static final Map<String, ConsumerWorker> consumerWorkerMap     = new ConcurrentHashMap<>();

	public KafkaClientTraitWrapper(final Traits traits, final NodeInterface wrappedObject) {

		super(traits, wrappedObject);
	}

	public String getGroupId() {

		return getProperty(traits.key(KafkaClientTraitDefinition.GROUP_ID_PROPERTY));
	}

	public String[] getServers() {

		return getProperty(traits.key(KafkaClientTraitDefinition.SERVERS_PROPERTY));
	}

	public void setServers(final String[] servers) throws FrameworkException {

		setProperty(traits.key(KafkaClientTraitDefinition.SERVERS_PROPERTY), servers);
	}

	/**
	 * Registers the boot-time initialization callback that starts the consumers of the existing clients. Called from
	 * {@code MessageEngineModule.onLoad()}: a static initializer would only run once something loads this class, which
	 * may happen after the initialization callbacks have run, see MQTTContext.registerInitializationCallback().
	 */
	public static void registerInitializationCallback() {

		Services.getInstance().registerInitializationCallback(() -> {

			if (Traits.exists(StructrTraits.KAFKA_CLIENT)) {

				final App app = StructrApp.getInstance();

				try (final Tx tx = app.tx()) {

					for (final NodeInterface client : app.nodeQuery(StructrTraits.KAFKA_CLIENT).getAsList()) {

						client.as(KafkaClient.class).setup();
					}

					tx.success();

				} catch (Throwable t) {

					final Logger logger = LoggerFactory.getLogger(KafkaClientTraitWrapper.class);
					logger.error("Unable to initialize Kafka clients. " + t);
				}
			}
		});
	}

	public Producer<String, String> getProducer() {

		return producerMap.get(getUuid());
	}

	public void setProducer(KafkaProducer<String, String> producer) {

		final Producer<String, String> previous = producerMap.put(getUuid(), producer);

		// a new configuration replaces the producer, the old one would keep its connections open
		if (previous != null && previous != producer) {

			previous.close();
		}
	}

	public void setup() {

		final ConsumerWorker cw = new ConsumerWorker(this);

		consumerWorkerMap.put(this.getUuid(), cw);

		// started once the client is committed, so that a rolled back or repeated transaction does not leave a
		// consumer running
		AfterCommit.run(() -> new Thread(cw).start());
	}

	public void close() {

		final Producer<String, String> producer = producerMap.remove(getUuid());
		if (producer != null) {

			producer.close();
		}

		final ConsumerWorker cw = consumerWorkerMap.remove(getUuid());
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

		deliverToSubscribers(new ActionContext(getSecurityContext()), topic, message);
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

				// the offsets are committed by the ConsumerWorker after the records are processed
				props.put("enable.auto.commit", "false");
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
		private final String clientId;
		private KafkaClient client;
		private KafkaConsumer<String, String> consumer;
		private List<String> currentlySubscribedTopics;
		private String currentGroupId;
		private volatile boolean running;
		private boolean wasDisabled = true;

		public ConsumerWorker(final KafkaClient client) {

			this.currentlySubscribedTopics = null;
			this.currentGroupId = null;
			this.clientId = client.getUuid();
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

				this.consumer = new KafkaConsumer<>(client.getConfiguration(KafkaConsumer.class));

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

				// the group id is null for a client without one, then the consumer uses a group of its own
				return !Objects.equals(currentGroupId, cId);

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

		/**
		 * Updates the consumer from the configuration of the client and returns whether it is ready to receive. The
		 * records are polled and processed outside of this transaction, each record in a transaction of its own.
		 */
		private boolean updateConsumer() throws FrameworkException {

			// an instance from an earlier transaction would not see changes to the client, like disabling it
			final NodeInterface current = StructrApp.getInstance().getNodeById(StructrTraits.KAFKA_CLIENT, clientId);
			if (current == null) {

				logger.info("KafkaClient {} was deleted, stopping its ConsumerWorker", clientId);

				running = false;

				return false;
			}

			this.client = current.as(KafkaClient.class);

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

						return true;

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

			return false;
		}

		/**
		 * Passes each record to the subscribers in a transaction of its own, which is repeated if the database terminates
		 * it, and commits the offsets afterwards. The offsets are committed after an empty poll too, so that a new consumer
		 * group keeps its starting position and a restart does not skip the records sent in the meantime, which the
		 * default reset to the end of the topic would do. A record whose callbacks fail, or whose transaction is still
		 * terminated after the last repetition, counts as processed, since delivering it again would most likely fail the
		 * same way. When the server shuts down, the offsets are not committed, so that the records are delivered again
		 * after the restart.
		 */
		private void process(final ConsumerRecords<String, String> records) {

			for (final ConsumerRecord<String, String> record : records) {

				if (Services.getInstance().isShuttingDown()) {

					return;
				}

				try {

					// the transaction of a record is the outermost one of this thread, so it can be repeated as a whole if
					// the database terminates it, e.g. to resolve a deadlock
					RetryException.repeat(() -> client.forwardReceivedMessage(record.topic(), record.value()));

				} catch (FrameworkException | RuntimeException e) {

					logger.error("Could not process records in ConsumerWorker: " + e);
				}
			}

			if (!Services.getInstance().isShuttingDown()) {

				this.consumer.commitSync();
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

			while (running) {

				if (this.client == null || Thread.currentThread().isInterrupted()) {

					running = false;
					break;
				}

				try {

					final boolean ready;

					try (final Tx tx = app.tx()) {

						ready = updateConsumer();

						tx.success();
					}

					if (ready) {

						process(this.consumer.poll(1000));
					}

				} catch (FrameworkException | NotInTransactionException ex) {

					// the next round reads the client again
					logger.error("Exception in ConsumerWorker for KafkaClient. " + ex);

				} catch (KafkaException ex) {

					// e.g. a commit that fails because of a rebalance: the records are delivered again, the worker goes on
					logger.warn("Kafka error in ConsumerWorker for KafkaClient {}: {}", clientId, ex.getMessage());

				} catch (IllegalStateException ex) {

					// the database driver was shut down, which happens when Structr stops
					if (Services.getInstance().isShuttingDown()) {

						running = false;
					}
				}

			}

			if (this.consumer != null) {

				this.consumer.close();
			}

		}

	}

}
