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
package org.structr.messaging;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.messaging.engine.MessageEngineModule;
import org.structr.messaging.engine.entities.MessageClient;
import org.structr.messaging.engine.entities.MessageSubscriber;
import org.structr.messaging.traits.definitions.KafkaClientTraitDefinition;
import org.structr.messaging.traits.definitions.MQTTClientTraitDefinition;
import org.structr.messaging.traits.definitions.MessageClientTraitDefinition;
import org.structr.messaging.traits.definitions.MessageSubscriberTraitDefinition;
import org.structr.messaging.traits.definitions.PulsarClientTraitDefinition;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;

/**
 * Tests the export and import of message clients and subscribers in a deployment.
 */
public class MessagingDeploymentTest extends MessagingTestBase {

	@Test
	public void testClientKeepsItsSubscribersThroughExportAndImport() throws Exception {

		final Traits clientTraits        = Traits.of(StructrTraits.MESSAGE_CLIENT);
		final Traits subscriberTraits    = Traits.of(StructrTraits.MESSAGE_SUBSCRIBER);
		final MessageEngineModule module = new MessageEngineModule();
		final Gson gson                  = new GsonBuilder().setPrettyPrinting().create();
		final Path deployment            = Files.createTempDirectory("structr-messaging-deployment");

		final String subscriberId;

		try (final Tx tx = app.tx()) {

			final NodeInterface subscriber = app.create(StructrTraits.MESSAGE_SUBSCRIBER, "subscriber");
			final NodeInterface client     = app.create(StructrTraits.MESSAGE_CLIENT, "client");

			subscriber.setProperty(subscriberTraits.key(MessageSubscriberTraitDefinition.TOPIC_PROPERTY), "deployment-topic");
			subscriber.setProperty(subscriberTraits.key(MessageSubscriberTraitDefinition.CALLBACK_PROPERTY), APPENDING_CALLBACK);

			client.setProperty(clientTraits.key(MessageClientTraitDefinition.SUBSCRIBERS_PROPERTY), List.of(subscriber));

			subscriberId = subscriber.getUuid();

			tx.success();
		}

		module.exportDeploymentData(deployment, gson);

		// the import replaces all clients and subscribers with those from the file
		module.importDeploymentData(deployment, gson);

		try (final Tx tx = app.tx()) {

			final List<NodeInterface> clients = app.nodeQuery(StructrTraits.MESSAGE_CLIENT).getAsList();

			assertEquals("The import must create the exported client", 1, clients.size());

			final List<MessageSubscriber> subscribers = new ArrayList<>();

			clients.get(0).as(MessageClient.class).getSubscribers().forEach(subscribers::add);

			assertEquals("The imported client must keep its subscriber", 1, subscribers.size());
			assertEquals("The subscriber must keep its id", subscriberId, subscribers.get(0).getUuid());
			assertEquals("The subscriber must keep its topic", "deployment-topic", subscribers.get(0).getTopic());
			assertEquals("The subscriber must keep its callback", APPENDING_CALLBACK, subscribers.get(0).getCallback());

			tx.success();
		}
	}

	@Test
	public void testClientsKeepTheirIdAndPropertiesThroughExportAndImport() throws Exception {

		// The id matters beyond the deployment: an MQTT client is known to its broker by its id, which identifies its
		// persistent session, and a Kafka client without a group id consumes in the group structr-<id>. All clients are
		// disabled, so that none of them connects.

		final Traits mqttTraits          = Traits.of(StructrTraits.MQTT_CLIENT);
		final Traits kafkaTraits         = Traits.of(StructrTraits.KAFKA_CLIENT);
		final Traits pulsarTraits        = Traits.of(StructrTraits.PULSAR_CLIENT);
		final MessageEngineModule module = new MessageEngineModule();
		final Gson gson                  = new GsonBuilder().setPrettyPrinting().create();
		final Path deployment            = Files.createTempDirectory("structr-messaging-deployment");

		final String mqttId;
		final String kafkaId;
		final String pulsarId;

		try (final Tx tx = app.tx()) {

			final NodeInterface mqtt   = app.create(StructrTraits.MQTT_CLIENT, "mqtt");
			final NodeInterface kafka  = app.create(StructrTraits.KAFKA_CLIENT, "kafka");
			final NodeInterface pulsar = app.create(StructrTraits.PULSAR_CLIENT, "pulsar");

			mqtt.setProperty(mqttTraits.key(MQTTClientTraitDefinition.MAIN_BROKER_URL_PROPERTY), "tcp://localhost:1");
			mqtt.setProperty(mqttTraits.key(MQTTClientTraitDefinition.FALLBACK_BROKER_URLS_PROPERTY), new String[] { "tcp://localhost:2" });
			mqtt.setProperty(mqttTraits.key(MQTTClientTraitDefinition.QOS_PROPERTY), 2);
			mqtt.setProperty(mqttTraits.key(MQTTClientTraitDefinition.CLEAN_SESSION_PROPERTY), true);
			mqtt.setProperty(mqttTraits.key(MQTTClientTraitDefinition.USERNAME_PROPERTY), "mqtt-user");
			mqtt.setProperty(mqttTraits.key(MQTTClientTraitDefinition.PASSWORD_PROPERTY), "mqtt-password");

			kafka.setProperty(kafkaTraits.key(KafkaClientTraitDefinition.SERVERS_PROPERTY), new String[] { "localhost:3" });
			kafka.setProperty(kafkaTraits.key(KafkaClientTraitDefinition.GROUP_ID_PROPERTY), "kafka-group");

			pulsar.setProperty(pulsarTraits.key(PulsarClientTraitDefinition.SERVERS_PROPERTY), new String[] { "pulsar://localhost:4" });

			mqttId   = mqtt.getUuid();
			kafkaId  = kafka.getUuid();
			pulsarId = pulsar.getUuid();

			tx.success();
		}

		module.exportDeploymentData(deployment, gson);
		module.importDeploymentData(deployment, gson);

		try (final Tx tx = app.tx()) {

			final NodeInterface mqtt   = app.getNodeById(StructrTraits.MQTT_CLIENT, mqttId);
			final NodeInterface kafka  = app.getNodeById(StructrTraits.KAFKA_CLIENT, kafkaId);
			final NodeInterface pulsar = app.getNodeById(StructrTraits.PULSAR_CLIENT, pulsarId);

			assertNotNull("The MQTT client must keep its id", mqtt);
			assertNotNull("The Kafka client must keep its id", kafka);
			assertNotNull("The Pulsar client must keep its id", pulsar);

			assertEquals("tcp://localhost:1", mqtt.getProperty(mqttTraits.key(MQTTClientTraitDefinition.MAIN_BROKER_URL_PROPERTY)));
			assertEquals(List.of("tcp://localhost:2"), List.of((String[]) mqtt.getProperty(mqttTraits.key(MQTTClientTraitDefinition.FALLBACK_BROKER_URLS_PROPERTY))));
			assertEquals(2, (int) mqtt.getProperty(mqttTraits.key(MQTTClientTraitDefinition.QOS_PROPERTY)));
			assertEquals(Boolean.TRUE, mqtt.getProperty(mqttTraits.key(MQTTClientTraitDefinition.CLEAN_SESSION_PROPERTY)));
			assertEquals("mqtt-user", mqtt.getProperty(mqttTraits.key(MQTTClientTraitDefinition.USERNAME_PROPERTY)));
			assertEquals("mqtt-password", mqtt.getProperty(mqttTraits.key(MQTTClientTraitDefinition.PASSWORD_PROPERTY)));
			assertFalse("The MQTT client must stay disabled", Boolean.TRUE.equals(mqtt.getProperty(mqttTraits.key(MQTTClientTraitDefinition.IS_ENABLED_PROPERTY))));

			assertEquals(List.of("localhost:3"), List.of((String[]) kafka.getProperty(kafkaTraits.key(KafkaClientTraitDefinition.SERVERS_PROPERTY))));
			assertEquals("kafka-group", kafka.getProperty(kafkaTraits.key(KafkaClientTraitDefinition.GROUP_ID_PROPERTY)));
			assertFalse("The Kafka client must stay disabled", Boolean.TRUE.equals(kafka.getProperty(kafkaTraits.key(KafkaClientTraitDefinition.ENABLED_PROPERTY))));

			assertEquals(List.of("pulsar://localhost:4"), List.of((String[]) pulsar.getProperty(pulsarTraits.key(PulsarClientTraitDefinition.SERVERS_PROPERTY))));
			assertFalse("The Pulsar client must stay disabled", Boolean.TRUE.equals(pulsar.getProperty(pulsarTraits.key(PulsarClientTraitDefinition.ENABLED_PROPERTY))));

			tx.success();
		}
	}
}
