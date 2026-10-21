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

import io.restassured.RestAssured;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonSchema;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.script.Scripting;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.definitions.SchemaMethodTraitDefinition;
import org.structr.messaging.engine.entities.MessageClient;
import org.structr.messaging.traits.definitions.MQTTClientTraitDefinition;
import org.structr.messaging.traits.definitions.MessageClientTraitDefinition;
import org.structr.messaging.traits.definitions.MessageSubscriberTraitDefinition;
import org.structr.schema.action.ActionContext;
import org.structr.schema.export.StructrSchema;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.structr.messaging.MosquittoTestSupport.connectTestClient;
import static org.structr.messaging.MosquittoTestSupport.message;
import static org.structr.messaging.MosquittoTestSupport.uniqueTopic;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * Tests the common ways of using message brokers in an application, as the documentation describes them, with MQTT as
 * the broker: setting up clients and subscribers, what a callback sees and does, and how subscriptions change at runtime.
 * The delivery semantics are tested per broker in MQTTTest, KafkaTest and PulsarTest.
 */
public class MessagingUseCaseTest extends MessagingTestBase {

	// a callback that records each message it processes as a group, named after the message, see getGroupNames()
	private static final String GROUP_CALLBACK = "{ $.create('Group', { name: $.message + '#' + Math.random() }); }";

	@Parameters("testDatabaseConnection")
	@BeforeClass(alwaysRun = true)
	@Override
	public void setup(@Optional final String testDatabaseConnection) {

		MosquittoTestSupport.start();

		super.setup(testDatabaseConnection);
	}

	@AfterClass(alwaysRun = true)
	@Override
	public void teardown() throws Exception {

		super.teardown();

		MosquittoTestSupport.stop();
	}

	@Test
	public void testClientSetUpThroughRestReceivesMessages() throws Exception {

		// the way the Admin UI and other REST clients set up a client: create, link, enable, check the status

		final String topic  = uniqueTopic();
		final String prefix = uniquePrefix();

		try (final Tx tx = app.tx()) {

			createAdminUser();

			tx.success();
		}

		final String subscriberId = RestAssured
			.given()
				.header(X_USER_HEADER, ADMIN_USERNAME)
				.header(X_PASSWORD_HEADER, ADMIN_PASSWORD)
				.contentType("application/json; charset=UTF-8")
				.body("{ \"topic\": \"" + topic + "\", \"callback\": \"" + GROUP_CALLBACK + "\" }")
			.expect()
				.statusCode(201)
			.when()
				.post("/MessageSubscriber")
			.jsonPath().getString("result[0]");

		final String clientId = RestAssured
			.given()
				.header(X_USER_HEADER, ADMIN_USERNAME)
				.header(X_PASSWORD_HEADER, ADMIN_PASSWORD)
				.contentType("application/json; charset=UTF-8")
				.body("{ \"name\": \"rest-client\", \"mainBrokerURL\": \"" + MosquittoTestSupport.getBrokerURL() + "\", \"qos\": 1, \"subscribers\": [\"" + subscriberId + "\"] }")
			.expect()
				.statusCode(201)
			.when()
				.post("/MQTTClient")
			.jsonPath().getString("result[0]");

		RestAssured
			.given()
				.header(X_USER_HEADER, ADMIN_USERNAME)
				.header(X_PASSWORD_HEADER, ADMIN_PASSWORD)
				.contentType("application/json; charset=UTF-8")
				.body("{ \"isEnabled\": true }")
			.expect()
				.statusCode(200)
			.when()
				.put("/MQTTClient/" + clientId);

		RestAssured
			.given()
				.header(X_USER_HEADER, ADMIN_USERNAME)
				.header(X_PASSWORD_HEADER, ADMIN_PASSWORD)
			.expect()
				.statusCode(200)
				.body("result.isConnected", equalTo(true))
			.when()
				.get("/MQTTClient/" + clientId);

		final MqttClient publisher = connectTestClient();

		try {

			publisher.publish(topic, message(prefix + "via-rest"));

			tryWithTimeout(() -> getGroupNames(prefix).size() >= 1, () -> fail("The client set up through REST must process the message"), 30_000, 100);

			assertEquals(List.of(prefix + "via-rest"), getGroupNames(prefix));

		} finally {

			publisher.disconnect();
		}
	}

	@Test
	public void testCallbackSeesTopicMessageAndSubscriber() throws Exception {

		final String topic         = uniqueTopic();
		final String callback      = "{ $.create('Group', { name: $.topic + '|' + $.message + '|' + $.this.id + '#' + Math.random() }); }";
		final String subscriberId  = createSubscriber(topic, callback);
		final MqttClient publisher = connectTestClient();

		createEnabledClient(subscriberId);

		try {

			publisher.publish(topic, message("payload"));

			tryWithTimeout(() -> getGroupNames(topic).size() >= 1, () -> fail("The callback must run"), 30_000, 100);

			assertEquals("The callback must see the topic, the message and the subscriber", List.of(topic + "|payload|" + subscriberId), getGroupNames(topic));

		} finally {

			publisher.disconnect();
		}
	}

	@Test
	public void testCallbackDelegatesToGlobalSchemaMethod() throws Exception {

		// the pattern the documentation recommends for complex processing: the callback only calls a schema method

		final String topic  = uniqueTopic();
		final String prefix = uniquePrefix();

		try (final Tx tx = app.tx()) {

			app.create(StructrTraits.SCHEMA_METHOD,
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "handleIncomingMessage"),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.SOURCE_PROPERTY), "{ $.create('Group', { name: 'handled:' + $.arguments.message + ':' + $.arguments.topic + '#' + Math.random() }); }")
			);

			tx.success();
		}

		final String subscriberId  = createSubscriber(topic, "{ $.call('handleIncomingMessage', { topic: $.topic, message: $.message }); }");
		final MqttClient publisher = connectTestClient();

		createEnabledClient(subscriberId);

		try {

			publisher.publish(topic, message(prefix + "delegated"));

			tryWithTimeout(() -> getGroupNames("handled:" + prefix).size() >= 1, () -> fail("The schema method must process the message"), 30_000, 100);

			assertEquals(List.of("handled:" + prefix + "delegated:" + topic), getGroupNames("handled:" + prefix));

		} finally {

			publisher.disconnect();
		}
	}

	@Test
	public void testJsonPayloadCreatesObjectsOfACustomType() throws Exception {

		// the typical IoT case: a device publishes JSON, the callback stores it as an object of the application's schema

		final String topic    = uniqueTopic();
		final String sensorId = uniquePrefix() + "sensor";

		try (final Tx tx = app.tx()) {

			final JsonSchema schema      = StructrSchema.createFromDatabase(app);
			final JsonObjectType reading = schema.addType("SensorReading");

			reading.addStringProperty("sensorId");
			reading.addDoubleProperty("value");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();
		}

		final String callback      = "{ let data = JSON.parse($.message); $.create('SensorReading', { sensorId: data.sensorId, value: data.value }); }";
		final String subscriberId  = createSubscriber(topic, callback);
		final MqttClient publisher = connectTestClient();

		createEnabledClient(subscriberId);

		try {

			publisher.publish(topic, message("{ \"sensorId\": \"" + sensorId + "\", \"value\": 21.5 }"));

			tryWithTimeout(() -> getSensorValues(sensorId).size() >= 1, () -> fail("The reading must be stored"), 30_000, 100);

			assertEquals("The reading must be stored once with its value", List.of(21.5), getSensorValues(sensorId));

		} finally {

			publisher.disconnect();
		}
	}

	@Test
	public void testWildcardSubscriberReceivesEveryMessageOfItsClient() throws Exception {

		// a subscriber with the topic * receives the messages of all topics its client is subscribed to

		final String topic         = uniqueTopic();
		final String prefix        = uniquePrefix();
		final String exact         = createSubscriber(topic, "{ $.create('Group', { name: 'exact-' + $.message + '#' + Math.random() }); }");
		final String wildcard      = createSubscriber("*", "{ $.create('Group', { name: 'wildcard-' + $.message + '#' + Math.random() }); }");
		final MqttClient publisher = connectTestClient();

		createEnabledClient(exact, wildcard);

		try {

			publisher.publish(topic, message(prefix + "shared"));

			tryWithTimeout(() -> getGroupNames("exact-" + prefix).size() >= 1 && getGroupNames("wildcard-" + prefix).size() >= 1, () -> fail("Both subscribers must process the message"), 30_000, 100);

			sleep(1_000);

			assertEquals("The subscriber of the topic must process the message once", 1, getGroupNames("exact-" + prefix).size());
			assertEquals("The wildcard subscriber must process the message once", 1, getGroupNames("wildcard-" + prefix).size());

		} finally {

			publisher.disconnect();
		}
	}

	@Test
	public void testSubscriberOfTwoClientsProcessesTheMessageOfEachClient() throws Exception {

		// a subscriber can be linked to several clients, and each client passes the messages it receives to it, so a
		// message that both clients receive is processed twice

		final String topic         = uniqueTopic();
		final String prefix        = uniquePrefix();
		final String subscriberId  = createSubscriber(topic, GROUP_CALLBACK);
		final MqttClient publisher = connectTestClient();

		createEnabledClient(subscriberId);
		createEnabledClient(subscriberId);

		try {

			publisher.publish(topic, message(prefix + "twice"));

			tryWithTimeout(() -> getGroupNames(prefix).size() >= 2, () -> fail("Each client must pass the message to the subscriber"), 30_000, 100);

			sleep(1_000);

			assertEquals("The message must be processed once per client", 2, getGroupNames(prefix).size());

		} finally {

			publisher.disconnect();
		}
	}

	@Test
	public void testSubscriberFollowsChangeOfItsTopic() throws Exception {

		final String oldTopic      = uniqueTopic();
		final String newTopic      = uniqueTopic();
		final String prefix        = uniquePrefix();
		final String subscriberId  = createSubscriber(oldTopic, GROUP_CALLBACK);
		final MqttClient publisher = connectTestClient();

		createEnabledClient(subscriberId);

		try (final Tx tx = app.tx()) {

			app.getNodeById(StructrTraits.MESSAGE_SUBSCRIBER, subscriberId).setProperty(Traits.of(StructrTraits.MESSAGE_SUBSCRIBER).key(MessageSubscriberTraitDefinition.TOPIC_PROPERTY), newTopic);

			tx.success();
		}

		try {

			publisher.publish(oldTopic, message(prefix + "old"));
			publisher.publish(newTopic, message(prefix + "new"));

			tryWithTimeout(() -> getGroupNames(prefix).size() >= 1, () -> fail("The message on the new topic must be processed"), 30_000, 100);

			sleep(1_000);

			assertEquals("Only the message on the new topic must be processed", List.of(prefix + "new"), getGroupNames(prefix));

		} finally {

			publisher.disconnect();
		}
	}

	@Test
	public void testMqttSubscribeAndUnsubscribeFunctions() throws Exception {

		// a script can subscribe the client to further topics at runtime, a wildcard subscriber processes their messages

		final String topic         = uniqueTopic();
		final String prefix        = uniquePrefix();
		final String subscriberId  = createSubscriber("*", GROUP_CALLBACK);
		final String clientId      = createEnabledClient(subscriberId);
		final MqttClient publisher = connectTestClient();

		try {

			runScript(clientId, "${{ $.mqttSubscribe($.this, '" + topic + "'); }}");

			publisher.publish(topic, message(prefix + "subscribed"));

			tryWithTimeout(() -> getGroupNames(prefix).size() >= 1, () -> fail("The message on the subscribed topic must be processed"), 30_000, 100);

			runScript(clientId, "${{ $.mqttUnsubscribe($.this, '" + topic + "'); }}");

			publisher.publish(topic, message(prefix + "unsubscribed"));

			sleep(2_000);

			assertEquals("Only the message before unsubscribing must be processed", List.of(prefix + "subscribed"), getGroupNames(prefix));

		} finally {

			publisher.disconnect();
		}
	}

	@Test
	public void testNonAsciiPayloadIsKeptInBothDirections() throws Exception {

		final String topic          = uniqueTopic();
		final String replyTopic     = uniqueTopic();
		final String prefix         = uniquePrefix();
		final String text           = "Grüße aus Köln, ½ °C, 東京, 🌍";
		final String subscriberId   = createSubscriber(topic, GROUP_CALLBACK);
		final String clientId       = createEnabledClient(subscriberId);
		final List<String> received = new CopyOnWriteArrayList<>();
		final MqttClient publisher  = connectTestClient();

		try {

			// incoming
			publisher.publish(topic, message(prefix + text));

			tryWithTimeout(() -> getGroupNames(prefix).size() >= 1, () -> fail("The message must be processed"), 30_000, 100);

			assertEquals("The incoming text must arrive unchanged", List.of(prefix + text), getGroupNames(prefix));

			// outgoing
			publisher.subscribe(replyTopic, 1, (t, m) -> received.add(new String(m.getPayload(), StandardCharsets.UTF_8)));

			try (final Tx tx = app.tx()) {

				getClient(clientId).as(MessageClient.class).sendMessage(new ActionContext(securityContext), replyTopic, text);

				tx.success();
			}

			tryWithTimeout(() -> !received.isEmpty(), () -> fail("The message must be published"), 30_000, 100);

			assertEquals("The outgoing text must arrive unchanged", List.of(text), received);

		} finally {

			publisher.disconnect();
		}
	}

	@Test
	public void testSendMessageTakesItsArgumentsByName() throws Exception {

		// a script passes the arguments of a method as an object, so their order must not matter

		final String topic          = uniqueTopic();
		final String clientId       = createEnabledClient(createSubscriber(uniqueTopic(), GROUP_CALLBACK));
		final List<String> received = new CopyOnWriteArrayList<>();
		final MqttClient listener   = connectTestClient();

		try {

			listener.subscribe(topic, 1, (t, m) -> received.add(new String(m.getPayload(), StandardCharsets.UTF_8)));

			runScript(clientId, "${{ $.this.sendMessage({ topic: '" + topic + "', message: 'topic first' }); }}");
			runScript(clientId, "${{ $.this.sendMessage({ message: 'message first', topic: '" + topic + "' }); }}");

			tryWithTimeout(() -> received.size() >= 2, () -> fail("Both messages must be published, got " + received), 30_000, 100);

			assertEquals(List.of("topic first", "message first"), received);

		} finally {

			listener.disconnect();
		}
	}

	@Test
	public void testDeletedClientStopsProcessing() throws Exception {

		final String topic         = uniqueTopic();
		final String prefix        = uniquePrefix();
		final String subscriberId  = createSubscriber(topic, GROUP_CALLBACK);
		final String clientId      = createEnabledClient(subscriberId);
		final MqttClient publisher = connectTestClient();

		try {

			try (final Tx tx = app.tx()) {

				app.delete(app.getNodeById(StructrTraits.MQTT_CLIENT, clientId));

				tx.success();
			}

			publisher.publish(topic, message(prefix + "after-deletion"));

			sleep(2_000);

			assertTrue("The subscriber of a deleted client must not receive messages", getGroupNames(prefix).isEmpty());

		} finally {

			publisher.disconnect();
		}
	}

	// ----- private methods -----
	private String createSubscriber(final String topic, final String callback) throws FrameworkException {

		final Traits subscriberTraits = Traits.of(StructrTraits.MESSAGE_SUBSCRIBER);

		try (final Tx tx = app.tx()) {

			final NodeInterface subscriber = app.create(StructrTraits.MESSAGE_SUBSCRIBER);

			subscriber.setProperty(subscriberTraits.key(MessageSubscriberTraitDefinition.TOPIC_PROPERTY), topic);
			subscriber.setProperty(subscriberTraits.key(MessageSubscriberTraitDefinition.CALLBACK_PROPERTY), callback);

			tx.success();

			return subscriber.getUuid();
		}
	}

	/**
	 * Creates an MQTTClient with QoS 1 for the given subscribers and enables it in a second transaction, so that it
	 * connects and subscribes to their topics.
	 */
	private String createEnabledClient(final String... subscriberIds) throws FrameworkException {

		final Traits clientTraits = Traits.of(StructrTraits.MQTT_CLIENT);
		final String clientId;

		try (final Tx tx = app.tx()) {

			final List<NodeInterface> subscribers = new ArrayList<>();

			for (final String subscriberId : subscriberIds) {

				subscribers.add(app.getNodeById(StructrTraits.MESSAGE_SUBSCRIBER, subscriberId));
			}

			final NodeInterface client = app.create(StructrTraits.MQTT_CLIENT, "client");

			client.setProperty(clientTraits.key(MQTTClientTraitDefinition.MAIN_BROKER_URL_PROPERTY), MosquittoTestSupport.getBrokerURL());
			client.setProperty(clientTraits.key(MQTTClientTraitDefinition.QOS_PROPERTY), 1);
			client.setProperty(clientTraits.key(MessageClientTraitDefinition.SUBSCRIBERS_PROPERTY), subscribers);

			clientId = client.getUuid();

			tx.success();
		}

		try (final Tx tx = app.tx()) {

			app.getNodeById(StructrTraits.MQTT_CLIENT, clientId).setProperty(clientTraits.key(MQTTClientTraitDefinition.IS_ENABLED_PROPERTY), true);

			tx.success();
		}

		return clientId;
	}

	private void runScript(final String clientId, final String script) throws FrameworkException {

		try (final Tx tx = app.tx()) {

			Scripting.replaceVariables(new ActionContext(securityContext), getClient(clientId), script);

			tx.success();
		}
	}

	private NodeInterface getClient(final String clientId) throws FrameworkException {

		return app.getNodeById(StructrTraits.MQTT_CLIENT, clientId);
	}

	private List<Double> getSensorValues(final String sensorId) {

		try (final Tx tx = app.tx()) {

			final Traits traits       = Traits.of("SensorReading");
			final List<Double> values = new ArrayList<>();

			for (final NodeInterface reading : app.nodeQuery("SensorReading").key(traits.key("sensorId"), sensorId).getAsList()) {

				values.add(reading.getProperty(traits.key("value")));
			}

			tx.success();

			return values;

		} catch (FrameworkException fex) {

			throw new RuntimeException(fex);
		}
	}

	private static String uniquePrefix() {

		return "usecase-" + UUID.randomUUID() + "-";
	}
}
