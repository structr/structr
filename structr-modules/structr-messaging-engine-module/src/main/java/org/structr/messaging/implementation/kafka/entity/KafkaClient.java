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
import org.structr.schema.json.JsonObjectType;
import org.structr.schema.json.JsonSchema;

import java.lang.reflect.Array;
import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public interface KafkaClient extends MessageClient {

	class Impl {
		static {


			final JsonSchema schema   = SchemaService.getDynamicSchema();
			final JsonObjectType type = schema.addType("KafkaClient");

			type.setImplements(URI.create("https://structr.org/v1.1/definitions/KafkaClient"));

			type.setExtends(URI.create("#/definitions/MessageClient"));

			type.addStringArrayProperty("servers", PropertyView.Public, PropertyView.Ui);


			type.addPropertyGetter("subscribers", List.class);

			type.addMethod("getServers")
					.setReturnType("String[]")
					.setSource("return getProperty(serversProperty);");

			type.overrideMethod("onCreation",     true, KafkaClient.class.getName() + ".onCreation(this, arg0, arg1);");
			type.overrideMethod("onModification", true, KafkaClient.class.getName() + ".onModification(this, arg0, arg1, arg2);");
			type.overrideMethod("onDeletion",     true, KafkaClient.class.getName() + ".onDeletion(this, arg0, arg1, arg2);");

			type.overrideMethod("sendMessage", true, "return " + KafkaClient.class.getName() + ".sendMessage(this,topic,message);");
			type.overrideMethod("subscribeTopic", false, "return " + KafkaClient.class.getName() + ".subscribeTopic(this,topic);");
			type.overrideMethod("unsubscribeTopic", false, "return " + KafkaClient.class.getName() + ".unsubscribeTopic(this,topic);");

			Services.getInstance().registerInitializationCallback(() -> {

				final App app = StructrApp.getInstance();

				try (final Tx tx = app.tx()) {

					for (final KafkaClient client : app.nodeQuery(KafkaClient.class).getAsList()) {
						setup(client);
					}

					tx.success();

				} catch (Throwable t) {
					logger.error("Unable to initialize Kafka clients: {}", t.getMessage());
				}
			});

		}
	}


	Map<String, Producer<String,String>> producerMap = new ConcurrentHashMap<>();
	Map<String, Consumer<String,String>> consumerMap = new ConcurrentHashMap<>();

	static Producer<String,String> getProducer(KafkaClient thisClient) {
		return producerMap.get(thisClient.getUuid());
	}

	static void setProducer(KafkaClient thisClient, KafkaProducer<String, String> producer) {
		producerMap.put(thisClient.getUuid(), producer);
	}

	static Consumer<String,String> getConsumer(KafkaClient thisClient) {
		return consumerMap.get(thisClient.getUuid());
	}

	static void setConsumer(KafkaClient thisClient, KafkaConsumer<String, String> consumer) {
		consumerMap.put(thisClient.getUuid(), consumer);
	}

	String[] getServers();
	List<MessageSubscriber> getSubscribers();

	static void onCreation(final KafkaClient thisClient, final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {
		refreshConfiguration(thisClient);
		setup(thisClient);
	}


	static void onModification(final KafkaClient thisClient, final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

		if(modificationQueue.isPropertyModified(thisClient,StructrApp.key(KafkaClient.class,"servers"))) {
			refreshConfiguration(thisClient);
		}

	}

	static void onDeletion(final KafkaClient thisClient, final SecurityContext securityContext, final ErrorBuffer errorBuffer, final PropertyMap properties) throws FrameworkException {
		close(thisClient);
	}


	static RestMethodResult sendMessage(KafkaClient thisClient, final String topic, final String message) throws FrameworkException {

		if(getProducer(thisClient) == null && thisClient.getServers() != null && thisClient.getServers().length > 0) {
			setProducer(thisClient,new KafkaProducer<>(getConfiguration(thisClient, KafkaProducer.class)));
		} else if(thisClient.getServers() == null || thisClient.getServers().length == 0) {
			logger.error("Could not initialize producer. No servers configured.");
			return new RestMethodResult(422);
		}
		if(getProducer(thisClient) != null) {
			getProducer(thisClient).send(new ProducerRecord<>(topic, message));
		}

		return new RestMethodResult(200);
	}

	static RestMethodResult subscribeTopic(KafkaClient thisClient, final String topic) throws FrameworkException {

		if(getConsumer(thisClient) == null && thisClient.getServers() != null && thisClient.getServers().length > 0) {
			setConsumer(thisClient, new KafkaConsumer<>(getConfiguration(thisClient, KafkaConsumer.class)));
		} else if(thisClient.getServers() == null || thisClient.getServers().length == 0) {
			logger.error("Could not initialize consumer. No servers configured.");
			return new RestMethodResult(422);
		}
		if(getConsumer(thisClient) != null) {
			List<String> newSubs = new ArrayList<>();
			newSubs.add(topic);
			getConsumer(thisClient).subscribe(newSubs);
		}

		return new RestMethodResult(200);
	}


	static RestMethodResult unsubscribeTopic(KafkaClient thisClient, final String topic) throws FrameworkException {

		if(getConsumer(thisClient) == null && thisClient.getServers() != null && thisClient.getServers().length > 0) {
			setConsumer(thisClient, new KafkaConsumer<>(getConfiguration(thisClient, KafkaConsumer.class)));
		} else if(thisClient.getServers() == null || thisClient.getServers().length == 0) {
			logger.error("Could not initialize consumer. No servers configured.");
			return new RestMethodResult(422);
		}
		if(getConsumer(thisClient) != null) {
			Set<String> subs = getConsumer(thisClient).subscription();
			List<String> newSubs = new ArrayList<>();
			subs.forEach(s -> newSubs.add(s));
			getConsumer(thisClient).subscribe(newSubs);
			newSubs.remove(topic);
			getConsumer(thisClient).unsubscribe();
			getConsumer(thisClient).subscribe(newSubs);
		}

		return new RestMethodResult(200);
	}

	static void setup(KafkaClient thisClient) {
		new Thread(new ConsumerWorker(thisClient)).start();
	}

	static void close(KafkaClient thisClient) {
		if(getConsumer(thisClient) != null) {
			getConsumer(thisClient).close();
		}
		if(getProducer(thisClient) != null) {
			getProducer(thisClient).close();
		}
	}

	static void refreshConfiguration(KafkaClient thisClient) {
		try {
			if(thisClient.getServers() != null && thisClient.getServers().length > 0) {
				setConsumer(thisClient, new KafkaConsumer<>(getConfiguration(thisClient, KafkaConsumer.class)));
				setProducer(thisClient, new KafkaProducer<>(getConfiguration(thisClient, KafkaProducer.class)));
				refreshSubscriptions(thisClient);
			}
		} catch (JsonSyntaxException | KafkaException ex) {
			setConsumer(thisClient, null);
			setConsumer(thisClient, null);
			logger.error("Could not refresh Kafka configuration: " + ex.getLocalizedMessage());
		}
	}

	static Properties getConfiguration(KafkaClient thisClient, Class clazz) {
		Properties props = new Properties();
		String[] servers = thisClient.getServers();
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

	static void refreshSubscriptions(KafkaClient thisClient) {

		final App app = StructrApp.getInstance();
		try(final Tx tx = app.tx()) {

			thisClient.getSubscribers().forEach((MessageSubscriber sub) -> {
				try {
					String topic = sub.getProperty(StructrApp.key(MessageSubscriber.class,"topic"));
					if (topic != null) {
						subscribeTopic(thisClient, topic);
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

	static void forwardReceivedMessage(KafkaClient thisClient, String topic, String message) throws FrameworkException {
		MessageClient.sendMessage(thisClient, topic, message);
	}

	class ConsumerWorker implements Runnable {
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

					if (this.client.getServers() != null && this.client.getServers().length > 0) {
						if (getConsumer(client) == null) {
							try {
								setConsumer(client, new KafkaConsumer<>(getConfiguration(client, KafkaConsumer.class)));
								refreshSubscriptions(client);
							} catch (KafkaException ex) {
								logger.error("Could not construct consumer for KafkaClient, triggered by ConsumerWorker Thread. " + ex.getLocalizedMessage());
								try {Thread.sleep(1000);} catch (InterruptedException iex) {}
							}
						} else {

							if (getConsumer(client).subscription().size() > 0) {
								final ConsumerRecords<String, String> records = getConsumer(client).poll(1000);

								records.forEach(record -> {
									try {
										forwardReceivedMessage(client, record.topic(), record.value());
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
