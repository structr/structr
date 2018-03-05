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
package org.structr.messaging.implementation.kafka.entity;

import com.google.gson.JsonSyntaxException;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.KafkaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.NotInTransactionException;
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
import org.structr.core.property.Property;
import org.structr.core.property.PropertyMap;
import org.structr.messaging.engine.entities.MessageClient;
import org.structr.messaging.engine.entities.MessageSubscriber;
import org.structr.rest.RestMethodResult;
import org.structr.schema.SchemaService;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

public class KafkaClient extends MessageClient {
	protected Producer<String, String> producer;
	protected Consumer<String, String> consumer;

	private static final Logger logger = LoggerFactory.getLogger(KafkaClient.class.getName());

	public static final Property<String[]> servers = new ArrayProperty<>("servers", String.class);

	public static final View publicView = new View(MessageSubscriber.class, PropertyView.Public,
			servers
	);

	public static final View uiView = new View(MessageSubscriber.class, PropertyView.Ui,
			servers
	);

	static {



		SchemaService.registerBuiltinTypeOverride("KafkaClient", KafkaClient.class.getName());


		Services.getInstance().registerInitializationCallback(() -> {

			final App app = StructrApp.getInstance();

			try (final Tx tx = app.tx()) {

				for (final KafkaClient client : app.nodeQuery(KafkaClient.class).getAsList()) {
					client.setup();
				}

				tx.success();

			} catch (Throwable t) {
				logger.error("Unable to initialize Kafka clients: {}", t.getMessage());
			}
		});

	}

	@Override
	public boolean onCreation(final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {
		refreshConfiguration();
		if(this.getProperty(KafkaClient.servers) != null) {
			this.refreshConfiguration();
		}
		this.setup();
		return super.onCreation(securityContext, errorBuffer);
	}

	@Override
	public boolean onModification(final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

		if(modificationQueue.isPropertyModified(this,servers)) {
			refreshConfiguration();
		}

		return super.onModification(securityContext, errorBuffer, modificationQueue);
	}

	@Override
	public boolean onDeletion(final SecurityContext securityContext, final ErrorBuffer errorBuffer, final PropertyMap properties) throws FrameworkException {
		close();
		return super.onDeletion(securityContext, errorBuffer, properties);
	}

	@Export
	public RestMethodResult sendMessage(final String topic, final String message) throws FrameworkException {

		if(producer == null && this.getProperty(KafkaClient.servers) != null && getProperty(servers).length > 0) {
			producer = new KafkaProducer<>(getConfiguration(KafkaProducer.class));
		} else if(this.getProperty(KafkaClient.servers) == null || getProperty(servers).length == 0) {
			logger.error("Could not initialize producer. No servers configured.");
			return new RestMethodResult(422);
		}
		if(producer != null) {
			producer.send(new ProducerRecord<>(topic, message));
		}

		return new RestMethodResult(200);
	}

	@Export
	public RestMethodResult subscribeTopic(final String topic) throws FrameworkException {

		if(consumer == null && this.getProperty(KafkaClient.servers) != null && getProperty(servers).length > 0) {
			consumer = new KafkaConsumer<>(getConfiguration(KafkaConsumer.class));
		} else if(this.getProperty(KafkaClient.servers) == null || getProperty(servers).length == 0) {
			logger.error("Could not initialize consumer. No servers configured.");
			return new RestMethodResult(422);
		}
		if(consumer != null) {
			List<String> newSubs = new ArrayList<>();
			newSubs.add(topic);
			consumer.subscribe(newSubs);
		}

		return new RestMethodResult(200);
	}

	@Export
	public RestMethodResult unsubscribeTopic(final String topic) throws FrameworkException {

		if(consumer == null && this.getProperty(KafkaClient.servers) != null && getProperty(servers).length > 0) {
			consumer = new KafkaConsumer<>(getConfiguration(KafkaConsumer.class));
		} else if(this.getProperty(KafkaClient.servers) == null || getProperty(servers).length == 0) {
			logger.error("Could not initialize consumer. No servers configured.");
			return new RestMethodResult(422);
		}
		if(consumer != null) {
			Set<String> subs = consumer.subscription();
			List<String> newSubs = new ArrayList<>();
			subs.forEach(s -> newSubs.add(s));
			consumer.subscribe(newSubs);
			newSubs.remove(topic);
			consumer.unsubscribe();
			consumer.subscribe(newSubs);
		}

		return new RestMethodResult(200);
	}

	protected void setup() {
		new Thread(new ConsumerWorker(this)).start();
	}

	private void close() {
		if(consumer != null) {
			consumer.close();
		}
		if(producer != null) {
			producer.close();
		}
	}

	private void refreshConfiguration() {
		try {
			if(getProperty(servers) != null && getProperty(servers).length > 0) {
				consumer = new KafkaConsumer<>(getConfiguration(KafkaConsumer.class));
				producer = new KafkaProducer<>(getConfiguration(KafkaProducer.class));
				refreshSubscriptions();
			}
		} catch (JsonSyntaxException | KafkaException ex) {
			consumer = null;
			producer = null;
			logger.error("Could not refresh Kafka configuration: " + ex.getLocalizedMessage());
		}
	}

	private Properties getConfiguration(Class clazz) {
		Properties props = new Properties();
		String[] servers = getProperty(KafkaClient.servers);
		if(servers != null) {
			props.setProperty("bootstrap.servers", String.join(",", servers));
		} else {
			props.setProperty("bootstrap.servers", "");
		}

		if(clazz == KafkaProducer.class) {
			props.put("acks", "all");
			props.put("retries", 0);
			props.put("batch.size", 16384);
			props.put("linger.ms", 1);
			props.put("buffer.memory", 33554432);
			props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
			props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
		} else if(clazz == KafkaConsumer.class) {
			props.put("group.id", "structr");
			props.put("enable.auto.commit", "true");
			props.put("auto.commit.interval.ms", "1000");
			props.put("session.timeout.ms", "30000");
			props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
			props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
		} else {
			return null;
		}

		return props;
	}

	private void refreshSubscriptions() {

		final App app = StructrApp.getInstance();
		try(final Tx tx = app.tx()) {

			getProperty(MessageClient.subscribers).forEach((MessageSubscriber sub) -> {
				try {
					String topic = sub.getProperty(MessageSubscriber.topic);
					if (topic != null) {
						subscribeTopic(topic);
					}
				} catch (FrameworkException e) {
					logger.warn("Could not subscribe to topic in KafkaClient: " + e.getMessage());
				}
			});
			tx.success();
		} catch (FrameworkException ex) {
			logger.error("Could not refresh subscriptions in KafkaClient: " + ex.getMessage());
		}
	}

	protected void forwardReceivedMessage(String topic, String message) throws FrameworkException {
		super.sendMessage(topic,message);
	}

	private class ConsumerWorker implements Runnable {
		private KafkaClient client;
		private final Logger logger = LoggerFactory.getLogger(ConsumerWorker.class.getName());

		public ConsumerWorker(KafkaClient client) {
			this.client = client;
			logger.info("Started ConsumerWorker for id: " + client.getProperty(id) + (client.getProperty(name) != null ? " name:" + client.getProperty(name) : ""));
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

			try {
				while (true) {
					if (this.client == null) {
						break;
					}

					if (this.client.getProperty(KafkaClient.servers) != null && this.client.getProperty(KafkaClient.servers).length > 0) {
						if (consumer == null) {
							try {
								consumer = new KafkaConsumer<>(getConfiguration(KafkaConsumer.class));
								client.refreshSubscriptions();
							} catch (KafkaException ex) {
								logger.error("Could not construct consumer for KafkaClient, triggered by ConsumerWorker Thread. " + ex.getLocalizedMessage());
								try {Thread.sleep(1000);} catch (InterruptedException iex) {}
							}
						} else {

							if (consumer.subscription().size() > 0) {
								final ConsumerRecords<String, String> records = consumer.poll(1000);

								records.forEach(record -> {
									try {
										client.forwardReceivedMessage(record.topic(), record.value());
									} catch (FrameworkException e) {
										logger.error("Could not process records in ConsumerWorker: " + e.getMessage());
									}
								});

							} else {
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
				}

			} catch (NotInTransactionException ex) {
				// Terminate thread since client became stale or invalid
				logger.warn("Terminating KafkaClient Thread with stale or invalid client reference.");
			}
		}

	}


}
