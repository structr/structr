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

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.structr.api.RetryException;
import org.structr.api.config.Settings;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.script.Scripting;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.messaging.engine.entities.MessageClient;
import org.structr.messaging.implementation.mqtt.MQTTClientConnection;
import org.structr.messaging.implementation.mqtt.MQTTInfo;
import org.structr.messaging.traits.definitions.MQTTClientTraitDefinition;
import org.structr.messaging.traits.definitions.MessageClientTraitDefinition;
import org.structr.messaging.traits.definitions.MessageSubscriberTraitDefinition;
import org.structr.schema.action.ActionContext;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.structr.messaging.MosquittoTestSupport.connectTestClient;
import static org.structr.messaging.MosquittoTestSupport.message;
import static org.structr.messaging.MosquittoTestSupport.uniqueTopic;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * Tests the delivery semantics of MQTTClient against a Mosquitto test container: incoming messages are processed and
 * acknowledged after the commit, outgoing messages are published after the commit of the sending transaction, and a
 * persistent session keeps the messages that arrive while the client is disconnected.
 */
public class MQTTTest extends MessagingTestBase {

	private static String brokerURL = null;

	@Parameters("testDatabaseConnection")
	@BeforeClass(alwaysRun = true)
	@Override
	public void setup(@Optional final String testDatabaseConnection) {

		MosquittoTestSupport.start();

		brokerURL = MosquittoTestSupport.getBrokerURL();

		super.setup(testDatabaseConnection);
	}

	@AfterClass(alwaysRun = true)
	@Override
	public void teardown() throws Exception {

		super.teardown();

		MosquittoTestSupport.stop();
	}

	@Test
	public void testIncomingMessageIsProcessedOnce() throws Exception {

		final String topic         = uniqueTopic();
		final ClientSetup setup    = createEnabledClient(topic, false, APPENDING_CALLBACK);
		final MqttClient publisher = connectTestClient();

		try {

			publisher.publish(topic, message("first"));

			tryWithTimeout(() -> "first;".equals(getSubscriberName(setup.subscriber())), () -> fail("The message must be processed"), 30_000, 100);

			// a message that is acknowledged after its callbacks must not come back
			sleep(2_000);

			assertEquals("The message must be processed exactly once", "first;", getSubscriberName(setup.subscriber()));

		} finally {

			publisher.disconnect();
		}
	}

	@Test
	public void testPublishedMessageIsSentOnlyAfterCommit() throws Exception {

		final String topic          = uniqueTopic();
		final ClientSetup setup     = createEnabledClient(uniqueTopic(), false, APPENDING_CALLBACK);
		final List<String> received = new CopyOnWriteArrayList<>();
		final MqttClient listener   = connectTestClient();

		try {

			listener.subscribe(topic, 1, (t, m) -> received.add(m.toString()));

			// rolled back: the message must never be published
			try (final Tx tx = app.tx()) {

				getClient(setup.client()).sendMessage(new ActionContext(securityContext), topic, "rolled-back");
			}

			try (final Tx tx = app.tx()) {

				getClient(setup.client()).sendMessage(new ActionContext(securityContext), topic, "committed");

				sleep(1_000);

				assertTrue("The message must not be published before the commit", received.isEmpty());

				tx.success();
			}

			tryWithTimeout(() -> !received.isEmpty(), () -> fail("The message must be published after the commit"), 30_000, 100);

			sleep(1_000);

			assertEquals("Only the message of the committed transaction must be published", List.of("committed"), received);

		} finally {

			listener.disconnect();
		}
	}

	@Test
	public void testRepeatedTransactionPublishesOnce() throws Exception {

		final String topic           = uniqueTopic();
		final ClientSetup setup      = createEnabledClient(uniqueTopic(), false, APPENDING_CALLBACK);
		final List<String> received  = new CopyOnWriteArrayList<>();
		final AtomicInteger attempts = new AtomicInteger();
		final MqttClient listener    = connectTestClient();

		try {

			listener.subscribe(topic, 1, (t, m) -> received.add(m.toString()));

			// the database terminating the first attempt is simulated by a RetryException
			RetryException.repeat(() -> {

				try (final Tx tx = app.tx()) {

					getClient(setup.client()).sendMessage(new ActionContext(securityContext), topic, "repeated");

					if (attempts.incrementAndGet() == 1) {

						throw new RetryException("simulated");
					}

					tx.success();
				}
			});

			tryWithTimeout(() -> !received.isEmpty(), () -> fail("The message must be published after the commit"), 30_000, 100);

			sleep(1_000);

			assertEquals("The transaction must be repeated once", 2, attempts.get());
			assertEquals("The message must be published once", List.of("repeated"), received);

		} finally {

			listener.disconnect();
		}
	}

	@Test
	public void testReplyFromCallbackIsPublishedAfterTheCallbackCommitted() throws Exception {

		final String requestTopic       = uniqueTopic();
		final String replyTopic         = uniqueTopic();
		final List<String> replies      = new CopyOnWriteArrayList<>();
		final List<String> namesAtReply = new CopyOnWriteArrayList<>();
		final String callback           = "{ $.this.name = ($.this.name ?? '') + $.message + ';'; $.first($.this.clients).sendMessage({ topic: '" + replyTopic + "', message: 'reply:' + $.message }); }";
		final ClientSetup setup         = createEnabledClient(requestTopic, false, callback);
		final MqttClient requester      = connectTestClient();

		try {

			// what the subscriber shows when the reply arrives tells whether the reply was published after the commit
			requester.subscribe(replyTopic, 1, (t, m) -> {

				namesAtReply.add(String.valueOf(getSubscriberName(setup.subscriber())));
				replies.add(m.toString());
			});

			requester.publish(requestTopic, message("request"));

			tryWithTimeout(() -> !replies.isEmpty(), () -> fail("The callback must publish a reply"), 30_000, 100);

			sleep(1_000);

			assertEquals("The reply must be published once", List.of("reply:request"), replies);
			assertEquals("The work of the callback must be committed when the reply arrives", List.of("request;"), namesAtReply);

		} finally {

			requester.disconnect();
		}
	}

	@Test
	public void testFailingCallbackDoesNotStopLaterMessages() throws Exception {

		// each message is processed in a thread of its own, so the callback creates a group per message instead of
		// appending to the name of the subscriber, where concurrent callbacks would overwrite each other
		final String prefix        = "failing-" + UUID.randomUUID() + "-";
		final String topic         = uniqueTopic();
		final String callback      = "{ if ($.message.endsWith('failing')) { throw new Error('rejected by the test'); } $.create('Group', { name: $.message + '#' + Math.random() }); }";
		final MqttClient publisher = connectTestClient();

		createEnabledClient(topic, false, callback);

		try {

			publisher.publish(topic, message(prefix + "before"));
			publisher.publish(topic, message(prefix + "failing"));
			publisher.publish(topic, message(prefix + "after"));

			tryWithTimeout(() -> getGroupNames(prefix).size() >= 2, () -> fail("The messages around the failing one must be processed, got " + getGroupNames(prefix)), 30_000, 100);

			sleep(1_000);

			assertEquals("Each message must be processed once, the failing one not at all", Set.of(prefix + "before", prefix + "after"), new HashSet<>(getGroupNames(prefix)));
			assertEquals("No message may be processed twice", 2, getGroupNames(prefix).size());

		} finally {

			publisher.disconnect();
		}
	}

	@Test
	public void testBurstOfMessagesIsProcessedExactlyOnce() throws Exception {

		final int count            = 50;
		final String prefix        = "burst-" + UUID.randomUUID() + "-";
		final String topic         = uniqueTopic();
		final String callback      = "{ $.create('Group', { name: $.message + '#' + Math.random() }); }";
		final MqttClient publisher = connectTestClient();

		createEnabledClient(topic, false, callback);

		try {

			for (int i = 0; i < count; i++) {

				publisher.publish(topic, message(prefix + i));
			}

			tryWithTimeout(() -> getGroupNames(prefix).size() >= count, () -> fail("All messages must be processed, got " + getGroupNames(prefix).size()), 60_000, 200);

			sleep(2_000);

			final List<String> names = getGroupNames(prefix);

			assertEquals("Each message must be processed exactly once", count, names.size());
			assertEquals("No message may be processed twice", count, new HashSet<>(names).size());

		} finally {

			publisher.disconnect();
		}
	}

	@Test
	public void testPersistentSessionReceivesMessagesSentWhileDisconnected() throws Exception {

		final String topic         = uniqueTopic();
		final ClientSetup setup    = createEnabledClient(topic, false, APPENDING_CALLBACK);
		final MqttClient publisher = connectTestClient();

		try {

			setEnabled(setup.client(), false);

			publisher.publish(topic, message("while-disconnected"));

			setEnabled(setup.client(), true);

			tryWithTimeout(() -> "while-disconnected;".equals(getSubscriberName(setup.subscriber())), () -> fail("The broker must deliver the message that arrived while the client was disconnected"), 30_000, 100);

		} finally {

			publisher.disconnect();
		}
	}

	@Test
	public void testCleanSessionDropsMessagesSentWhileDisconnected() throws Exception {

		final String topic         = uniqueTopic();
		final ClientSetup setup    = createEnabledClient(topic, true, APPENDING_CALLBACK);
		final MqttClient publisher = connectTestClient();

		try {

			setEnabled(setup.client(), false);

			publisher.publish(topic, message("while-disconnected"));

			setEnabled(setup.client(), true);

			// the client receives the next message, so it is subscribed again
			publisher.publish(topic, message("after-reconnect"));

			tryWithTimeout(() -> getSubscriberName(setup.subscriber()) != null, () -> fail("The message after the reconnect must be processed"), 30_000, 100);

			sleep(1_000);

			assertEquals("A clean session must not deliver the message that arrived while the client was disconnected", "after-reconnect;", getSubscriberName(setup.subscriber()));

		} finally {

			publisher.disconnect();
		}
	}

	@Test
	public void testMessageNotAcknowledgedBeforeDisconnectIsDeliveredAgain() throws Exception {

		// The callback takes three seconds, and the client disconnects while it runs. The message is acknowledged only
		// after the callback, which is too late for the closed connection, so the broker delivers it again when the
		// client reconnects to its persistent session. This is the at-least-once guarantee: no message is lost when the
		// connection ends while Structr processes it, for example because Structr stops, at the price of processing it
		// twice.

		final String topic         = uniqueTopic();
		final String callback      = "{ $.sleep(3000); $.this.name = ($.this.name ?? '') + $.message + ';'; }";
		final ClientSetup setup    = createEnabledClient(topic, false, callback);
		final MqttClient publisher = connectTestClient();

		try {

			publisher.publish(topic, message("interrupted"));

			// the callback is running now
			sleep(1_000);

			setEnabled(setup.client(), false);

			tryWithTimeout(() -> "interrupted;".equals(getSubscriberName(setup.subscriber())), () -> fail("The callback must finish"), 30_000, 100);

			setEnabled(setup.client(), true);

			tryWithTimeout(() -> "interrupted;interrupted;".equals(getSubscriberName(setup.subscriber())), () -> fail("The broker must deliver the unacknowledged message again, got " + getSubscriberName(setup.subscriber())), 30_000, 100);

		} finally {

			publisher.disconnect();
		}
	}

	@Test
	public void testSubscriberAddedAtRuntimeReceivesMessages() throws Exception {

		// creating a subscriber for a connected client subscribes the client to the new topic at the broker

		final String prefix        = "runtime-" + UUID.randomUUID() + "-";
		final String newTopic      = uniqueTopic();
		final ClientSetup setup    = createEnabledClient(uniqueTopic(), false, APPENDING_CALLBACK);
		final MqttClient publisher = connectTestClient();

		try {

			addSubscriber(setup.client(), newTopic, "{ $.create('Group', { name: $.message + '#' + Math.random() }); }");

			publisher.publish(newTopic, message(prefix + "on-new-topic"));

			tryWithTimeout(() -> getGroupNames(prefix).size() >= 1, () -> fail("The subscriber added at runtime must receive the message"), 30_000, 100);

			sleep(1_000);

			assertEquals("The message must be processed once", List.of(prefix + "on-new-topic"), getGroupNames(prefix));

		} finally {

			publisher.disconnect();
		}
	}

	@Test
	public void testFailingSubscriberDoesNotStopTheOtherSubscribersOfATopic() throws Exception {

		// all subscribers of a topic run in the transaction of the message, the error of one of them is logged and the
		// work of the others is committed

		final String prefix        = "subscribers-" + UUID.randomUUID() + "-";
		final String topic         = uniqueTopic();
		final ClientSetup setup    = createEnabledClient(topic, false, "{ throw new Error('rejected by the test'); }");
		final MqttClient publisher = connectTestClient();

		try {

			addSubscriber(setup.client(), topic, "{ $.create('Group', { name: $.message + '#' + Math.random() }); }");

			publisher.publish(topic, message(prefix + "shared"));

			tryWithTimeout(() -> getGroupNames(prefix).size() >= 1, () -> fail("The working subscriber must process the message"), 30_000, 100);

			sleep(1_000);

			assertEquals("The working subscriber must process the message once", List.of(prefix + "shared"), getGroupNames(prefix));

		} finally {

			publisher.disconnect();
		}
	}

	@Test
	public void testMqttPublishFunctionSendsOnlyAfterCommit() throws Exception {

		final String topic          = uniqueTopic();
		final ClientSetup setup     = createEnabledClient(uniqueTopic(), false, APPENDING_CALLBACK);
		final List<String> received = new CopyOnWriteArrayList<>();
		final MqttClient listener   = connectTestClient();

		try {

			listener.subscribe(topic, 1, (t, m) -> received.add(m.toString()));

			// rolled back: the message must never be published
			try (final Tx tx = app.tx()) {

				Scripting.replaceVariables(new ActionContext(securityContext), getClient(setup.client()), "${{ $.mqttPublish($.this, '" + topic + "', 'rolled-back'); }}");
			}

			try (final Tx tx = app.tx()) {

				Scripting.replaceVariables(new ActionContext(securityContext), getClient(setup.client()), "${{ $.mqttPublish($.this, '" + topic + "', 'committed'); }}");

				sleep(1_000);

				assertTrue("The message must not be published before the commit", received.isEmpty());

				tx.success();
			}

			tryWithTimeout(() -> !received.isEmpty(), () -> fail("The message must be published after the commit"), 30_000, 100);

			sleep(1_000);

			assertEquals("Only the message of the committed transaction must be published", List.of("committed"), received);

		} finally {

			listener.disconnect();
		}
	}

	@Test
	public void testTerminatedCallbackTransactionIsRepeated() throws Exception {

		// The callbacks run in a connection whose client is a test double. Its first attempt fails with a RetryException,
		// which is what a transaction that the database terminated, e.g. to resolve a deadlock, ends with.

		final String topic                    = uniqueTopic();
		final AtomicInteger attempts          = new AtomicInteger();
		final List<String> processed          = new CopyOnWriteArrayList<>();
		final MQTTClientConnection connection = new MQTTClientConnection(new TestClientInfo(topic, message -> {

			if (attempts.incrementAndGet() == 1) {

				throw new RetryException("simulated");
			}

			processed.add(message);
		}));

		final MqttClient publisher = connectTestClient();

		try {

			connection.connect();
			connection.subscribeTopic(topic);

			publisher.publish(topic, message("terminated"));

			tryWithTimeout(() -> !processed.isEmpty(), () -> fail("The callbacks must be repeated after the terminated attempt"), 30_000, 100);

			sleep(1_000);

			assertEquals("The callbacks must run twice", 2, attempts.get());
			assertEquals("The message must be processed once", List.of("terminated"), processed);

		} finally {

			publisher.disconnect();
			connection.disconnect();
		}
	}

	@Test
	public void testClientConnectsToFallbackBrokerWhenMainBrokerIsUnreachable() throws Exception {

		// nothing listens on the port of the main broker, so the client must connect to the fallback broker

		final String topic            = uniqueTopic();
		final Traits clientTraits     = Traits.of(StructrTraits.MQTT_CLIENT);
		final Traits subscriberTraits = Traits.of(StructrTraits.MESSAGE_SUBSCRIBER);
		final MqttClient publisher    = connectTestClient();

		final String subscriberId;
		final String clientId;

		try (final Tx tx = app.tx()) {

			final NodeInterface subscriber = app.create(StructrTraits.MESSAGE_SUBSCRIBER);
			final NodeInterface client     = app.create(StructrTraits.MQTT_CLIENT, "client");

			subscriber.setProperty(subscriberTraits.key(MessageSubscriberTraitDefinition.TOPIC_PROPERTY), topic);
			subscriber.setProperty(subscriberTraits.key(MessageSubscriberTraitDefinition.CALLBACK_PROPERTY), APPENDING_CALLBACK);

			client.setProperty(clientTraits.key(MQTTClientTraitDefinition.MAIN_BROKER_URL_PROPERTY), "tcp://localhost:1");
			client.setProperty(clientTraits.key(MQTTClientTraitDefinition.FALLBACK_BROKER_URLS_PROPERTY), new String[] { brokerURL });
			client.setProperty(clientTraits.key(MQTTClientTraitDefinition.QOS_PROPERTY), 1);
			client.setProperty(clientTraits.key(MessageClientTraitDefinition.SUBSCRIBERS_PROPERTY), List.of(subscriber));

			subscriberId = subscriber.getUuid();
			clientId     = client.getUuid();

			tx.success();
		}

		try {

			// asserts that the client is connected
			setEnabled(clientId, true);

			publisher.publish(topic, message("via-fallback"));

			tryWithTimeout(() -> "via-fallback;".equals(getSubscriberName(subscriberId)), () -> fail("The client must receive the message through the fallback broker"), 30_000, 100);

		} finally {

			publisher.disconnect();
		}
	}

	@Test
	public void testTerminatedTransactionOfAScriptCallbackIsRepeated() throws Exception {

		// the whole path, from the broker through the subscriber callback, see also testTerminatedCallbackTransactionIsRepeated
		final String previous = allowRetryExceptionInScripts();

		try {

			final String topic         = uniqueTopic();
			final ClientSetup setup    = createEnabledClient(topic, false, failingOnceCallback());
			final MqttClient publisher = connectTestClient();

			try {

				publisher.publish(topic, message("terminated"));

				tryWithTimeout(() -> "terminated;".equals(getSubscriberName(setup.subscriber())), () -> fail("The message must be processed in the repeated transaction"), 30_000, 100);

				sleep(1_000);

				assertEquals("The message must be processed once", "terminated;", getSubscriberName(setup.subscriber()));

			} finally {

				publisher.disconnect();
			}

		} finally {

			Settings.AllowedHostClasses.setValue(previous);
		}
	}

	// ----- private methods -----
	private record ClientSetup(String client, String subscriber) {}

	/**
	 * Creates an MQTTClient with QoS 1 and a subscriber with the given callback for the given topic, and enables the
	 * client in a second transaction, so that it connects and subscribes to the topic of the committed subscriber.
	 */
	private ClientSetup createEnabledClient(final String topic, final boolean cleanSession, final String callback) throws FrameworkException {

		final Traits clientTraits     = Traits.of(StructrTraits.MQTT_CLIENT);
		final Traits subscriberTraits = Traits.of(StructrTraits.MESSAGE_SUBSCRIBER);

		final ClientSetup setup;

		try (final Tx tx = app.tx()) {

			// the subscriber has no name, the appending callback adds the messages to it
			final NodeInterface subscriber = app.create(StructrTraits.MESSAGE_SUBSCRIBER);
			final NodeInterface client     = app.create(StructrTraits.MQTT_CLIENT, "client");

			subscriber.setProperty(subscriberTraits.key(MessageSubscriberTraitDefinition.TOPIC_PROPERTY), topic);
			subscriber.setProperty(subscriberTraits.key(MessageSubscriberTraitDefinition.CALLBACK_PROPERTY), callback);

			client.setProperty(clientTraits.key(MQTTClientTraitDefinition.MAIN_BROKER_URL_PROPERTY), brokerURL);
			client.setProperty(clientTraits.key(MQTTClientTraitDefinition.QOS_PROPERTY), 1);
			client.setProperty(clientTraits.key(MQTTClientTraitDefinition.CLEAN_SESSION_PROPERTY), cleanSession);
			client.setProperty(clientTraits.key(MessageClientTraitDefinition.SUBSCRIBERS_PROPERTY), List.of(subscriber));

			setup = new ClientSetup(client.getUuid(), subscriber.getUuid());

			tx.success();
		}

		setEnabled(setup.client(), true);

		return setup;
	}

	/**
	 * Creates a subscriber with the given callback for the given topic and links it to the client.
	 */
	private void addSubscriber(final String clientId, final String topic, final String callback) throws FrameworkException {

		final Traits subscriberTraits = Traits.of(StructrTraits.MESSAGE_SUBSCRIBER);

		try (final Tx tx = app.tx()) {

			final NodeInterface subscriber = app.create(StructrTraits.MESSAGE_SUBSCRIBER);

			subscriber.setProperty(subscriberTraits.key(MessageSubscriberTraitDefinition.TOPIC_PROPERTY), topic);
			subscriber.setProperty(subscriberTraits.key(MessageSubscriberTraitDefinition.CALLBACK_PROPERTY), callback);
			subscriber.setProperty(subscriberTraits.key(MessageSubscriberTraitDefinition.CLIENTS_PROPERTY), List.of(app.getNodeById(StructrTraits.MQTT_CLIENT, clientId)));

			tx.success();
		}
	}

	private void setEnabled(final String clientId, final boolean enabled) throws FrameworkException {

		final Traits clientTraits = Traits.of(StructrTraits.MQTT_CLIENT);

		try (final Tx tx = app.tx()) {

			app.getNodeById(StructrTraits.MQTT_CLIENT, clientId).setProperty(clientTraits.key(MQTTClientTraitDefinition.IS_ENABLED_PROPERTY), enabled);

			tx.success();
		}

		try (final Tx tx = app.tx()) {

			final Object connected = app.getNodeById(StructrTraits.MQTT_CLIENT, clientId).getProperty(clientTraits.key(MQTTClientTraitDefinition.IS_CONNECTED_PROPERTY));

			assertEquals("The client must be connected exactly when it is enabled", enabled, Boolean.TRUE.equals(connected));

			tx.success();
		}
	}

	private MessageClient getClient(final String clientId) throws FrameworkException {

		return app.getNodeById(StructrTraits.MQTT_CLIENT, clientId).as(MessageClient.class);
	}

	/**
	 * A client for a connection without a node behind it, whose callback is given by the test.
	 */
	private static class TestClientInfo implements MQTTInfo {

		private final String uuid = UUID.randomUUID().toString().replace("-", "");
		private final Consumer<String> callback;
		private final String topic;

		TestClientInfo(final String topic, final Consumer<String> callback) {

			this.callback = callback;
			this.topic    = topic;
		}

		@Override
		public String getMainBrokerURL() {

			return brokerURL;
		}

		@Override
		public String[] getFallbackBrokerURLs() {

			return null;
		}

		@Override
		public int getQos() {

			return 1;
		}

		@Override
		public boolean getCleanSession() {

			return true;
		}

		@Override
		public String getUuid() {

			return uuid;
		}

		@Override
		public String getUsername() {

			return null;
		}

		@Override
		public String getPassword() {

			return null;
		}

		@Override
		public String[] getTopics() {

			return new String[] { topic };
		}

		@Override
		public void messageCallback(final String topic, final String message) {

			callback.accept(message);
		}

		@Override
		public void connectionStatusCallback(final boolean connected) {
		}
	}
}
