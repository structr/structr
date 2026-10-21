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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.Producer;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.SubscriptionInitialPosition;
import org.structr.api.RetryException;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.messaging.engine.entities.MessageClient;
import org.structr.messaging.traits.definitions.MessageClientTraitDefinition;
import org.structr.messaging.traits.definitions.MessageSubscriberTraitDefinition;
import org.structr.messaging.traits.definitions.PulsarClientTraitDefinition;
import org.structr.schema.action.ActionContext;
import org.testcontainers.pulsar.PulsarContainer;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.fail;

/**
 * Tests the delivery semantics of PulsarClient against a Pulsar test container: incoming messages are processed once
 * and not sent back to their topic, outgoing messages are sent after the commit of the sending transaction, and the
 * consumer follows the state of its client.
 */
public class PulsarTest extends MessagingTestBase {

	// the name of the subscription that the consumers of Structr use
	private static final String SUBSCRIPTION = "structr-pulsar-subscription";

	private static PulsarContainer pulsar = null;
	private static PulsarClient client    = null;
	private static HttpClient http        = null;

	@Parameters("testDatabaseConnection")
	@BeforeClass(alwaysRun = true)
	@Override
	public void setup(@Optional final String testDatabaseConnection) {

		pulsar = new PulsarContainer("apachepulsar/pulsar:4.0.5");

		pulsar.start();

		try {

			client = PulsarClient.builder().serviceUrl(pulsar.getPulsarBrokerUrl()).build();
			http   = HttpClient.newHttpClient();

		} catch (Exception ex) {

			throw new RuntimeException(ex);
		}

		super.setup(testDatabaseConnection);
	}

	@AfterClass(alwaysRun = true)
	@Override
	public void teardown() throws Exception {

		super.teardown();

		if (client != null) {

			client.close();
			client = null;
		}

		if (pulsar != null) {

			pulsar.stop();
			pulsar = null;
		}
	}

	@Test
	public void testIncomingMessageIsProcessedOnceAndNotSentBack() throws Exception {

		final String topic      = uniqueTopic();
		final ClientSetup setup = createConsumingClient(topic, APPENDING_CALLBACK);

		waitForConsumer(topic);

		send(topic, "first");

		tryWithTimeout(() -> "first;".equals(getSubscriberName(setup.subscriber())), () -> fail("The message must be processed"), 30_000, 100);

		sleep(2_000);

		assertEquals("The message must be processed exactly once", "first;", getSubscriberName(setup.subscriber()));
		assertEquals("The received message must not be sent back to its topic", List.of("first"), readTopic(topic, 1));
	}

	@Test
	public void testSentMessageIsSentOnlyAfterCommit() throws Exception {

		final String topic           = uniqueTopic();
		final String clientId        = createSendingClient();
		final AtomicInteger attempts = new AtomicInteger();

		// rolled back: the message must never be sent
		try (final Tx tx = app.tx()) {

			getClient(clientId).sendMessage(new ActionContext(securityContext), topic, "rolled-back");
		}

		try (final Tx tx = app.tx()) {

			getClient(clientId).sendMessage(new ActionContext(securityContext), topic, "committed");

			tx.success();
		}

		// repeated: the message must be sent once, the database terminating the first attempt is simulated by a RetryException
		RetryException.repeat(() -> {

			try (final Tx tx = app.tx()) {

				getClient(clientId).sendMessage(new ActionContext(securityContext), topic, "repeated");

				if (attempts.incrementAndGet() == 1) {

					throw new RetryException("simulated");
				}

				tx.success();
			}
		});

		assertEquals("The transaction must be repeated once", 2, attempts.get());
		assertEquals("Only the messages of the committed transactions must be sent, each once", List.of("committed", "repeated"), readTopic(topic, 2));
	}

	@Test
	public void testFailingCallbackDoesNotStopLaterMessages() throws Exception {

		final String topic      = uniqueTopic();
		final String callback   = "{ if ($.message === 'failing') { throw new Error('rejected by the test'); } $.this.name = ($.this.name ?? '') + $.message + ';'; }";
		final ClientSetup setup = createConsumingClient(topic, callback);

		waitForConsumer(topic);

		send(topic, "before");
		send(topic, "failing");
		send(topic, "after");

		tryWithTimeout(() -> "before;after;".equals(getSubscriberName(setup.subscriber())), () -> fail("The messages around the failing one must be processed, got " + getSubscriberName(setup.subscriber())), 30_000, 100);

		sleep(2_000);

		assertEquals("Each message must be processed once, the failing one not at all", "before;after;", getSubscriberName(setup.subscriber()));
	}

	@Test
	public void testReplyFromCallbackIsSentOnce() throws Exception {

		final String requestTopic = uniqueTopic();
		final String replyTopic   = uniqueTopic();
		final String callback     = "{ $.this.name = ($.this.name ?? '') + $.message + ';'; $.first($.this.clients).sendMessage({ topic: '" + replyTopic + "', message: 'reply:' + $.message }); }";
		final ClientSetup setup   = createConsumingClient(requestTopic, callback);

		waitForConsumer(requestTopic);

		send(requestTopic, "request");

		assertEquals("The callback must send the reply once", List.of("reply:request"), readTopic(replyTopic, 1));
		assertEquals("The request must be processed once", "request;", getSubscriberName(setup.subscriber()));
		assertEquals("Neither the request nor the reply must be sent back to the request topic", List.of("request"), readTopic(requestTopic, 1));
	}

	@Test
	public void testMessagesSentWhileDisabledAreProcessedAfterEnabling() throws Exception {

		// the subscription keeps the messages while the consumer is closed

		final String topic      = uniqueTopic();
		final ClientSetup setup = createConsumingClient(topic, APPENDING_CALLBACK);

		waitForConsumer(topic);

		setEnabled(setup.client(), false);

		tryWithTimeout(() -> getConsumerCount(topic) == 0, () -> fail("Disabling the client must close its consumer"), 30_000, 200);

		send(topic, "while-disabled");

		sleep(2_000);

		assertNull("A disabled client must not process messages", getSubscriberName(setup.subscriber()));

		setEnabled(setup.client(), true);

		tryWithTimeout(() -> "while-disabled;".equals(getSubscriberName(setup.subscriber())), () -> fail("The message sent while the client was disabled must be processed after enabling it"), 30_000, 100);
	}

	@Test
	public void testConsumerFollowsTopicChangeOfSubscriber() throws Exception {

		final String oldTopic   = uniqueTopic();
		final String newTopic   = uniqueTopic();
		final ClientSetup setup = createConsumingClient(oldTopic, APPENDING_CALLBACK);

		waitForConsumer(oldTopic);

		try (final Tx tx = app.tx()) {

			app.getNodeById(StructrTraits.MESSAGE_SUBSCRIBER, setup.subscriber()).setProperty(Traits.of(StructrTraits.MESSAGE_SUBSCRIBER).key(MessageSubscriberTraitDefinition.TOPIC_PROPERTY), newTopic);

			tx.success();
		}

		waitForConsumer(newTopic);

		send(newTopic, "on-new-topic");

		tryWithTimeout(() -> "on-new-topic;".equals(getSubscriberName(setup.subscriber())), () -> fail("The message on the new topic must be processed"), 30_000, 100);
	}

	@Test
	public void testDeletedClientStopsConsuming() throws Exception {

		final String topic      = uniqueTopic();
		final ClientSetup setup = createConsumingClient(topic, APPENDING_CALLBACK);

		waitForConsumer(topic);

		try (final Tx tx = app.tx()) {

			app.delete(app.getNodeById(StructrTraits.PULSAR_CLIENT, setup.client()));

			tx.success();
		}

		tryWithTimeout(() -> getConsumerCount(topic) == 0, () -> fail("Deleting the client must close its consumer"), 30_000, 200);

		send(topic, "after-deletion");

		sleep(2_000);

		assertNull("The subscriber of a deleted client must not receive messages", getSubscriberName(setup.subscriber()));
	}

	@Test
	public void testBurstOfMessagesIsProcessedExactlyOnce() throws Exception {

		final int count     = 50;
		final String prefix = "burst-" + UUID.randomUUID() + "-";
		final String topic  = uniqueTopic();

		createConsumingClient(topic, "{ $.create('Group', { name: $.message + '#' + Math.random() }); }");

		waitForConsumer(topic);

		try (final Producer<byte[]> producer = client.newProducer().topic(topic).create()) {

			for (int i = 0; i < count; i++) {

				producer.send((prefix + i).getBytes(StandardCharsets.UTF_8));
			}
		}

		tryWithTimeout(() -> getGroupNames(prefix).size() >= count, () -> fail("All messages must be processed, got " + getGroupNames(prefix).size()), 60_000, 200);

		sleep(2_000);

		final List<String> names = getGroupNames(prefix);

		assertEquals("Each message must be processed exactly once", count, names.size());
		assertEquals("No message may be processed twice", count, new HashSet<>(names).size());
	}

	@Test
	public void testNonAsciiPayloadIsKeptInBothDirections() throws Exception {

		final String text       = "Grüße aus Köln, ½ °C, 東京, 🌍";
		final String topic      = uniqueTopic();
		final String replyTopic = uniqueTopic();
		final ClientSetup setup = createConsumingClient(topic, APPENDING_CALLBACK);

		waitForConsumer(topic);

		// incoming
		send(topic, text);

		tryWithTimeout(() -> (text + ";").equals(getSubscriberName(setup.subscriber())), () -> fail("The incoming text must arrive unchanged, got " + getSubscriberName(setup.subscriber())), 30_000, 100);

		// outgoing
		try (final Tx tx = app.tx()) {

			getClient(setup.client()).sendMessage(new ActionContext(securityContext), replyTopic, text);

			tx.success();
		}

		assertEquals("The outgoing text must arrive unchanged", List.of(text), readTopic(replyTopic, 1));
	}

	// ----- private methods -----
	private record ClientSetup(String client, String subscriber) {}

	/**
	 * Creates an enabled PulsarClient with a subscriber with the given callback for the given topic.
	 */
	private ClientSetup createConsumingClient(final String topic, final String callback) throws FrameworkException {

		final Traits clientTraits     = Traits.of(StructrTraits.PULSAR_CLIENT);
		final Traits subscriberTraits = Traits.of(StructrTraits.MESSAGE_SUBSCRIBER);

		try (final Tx tx = app.tx()) {

			// the subscriber has no name, the appending callback adds the messages to it
			final NodeInterface subscriber   = app.create(StructrTraits.MESSAGE_SUBSCRIBER);
			final NodeInterface pulsarClient = app.create(StructrTraits.PULSAR_CLIENT, "client");

			subscriber.setProperty(subscriberTraits.key(MessageSubscriberTraitDefinition.TOPIC_PROPERTY), topic);
			subscriber.setProperty(subscriberTraits.key(MessageSubscriberTraitDefinition.CALLBACK_PROPERTY), callback);

			pulsarClient.setProperty(clientTraits.key(PulsarClientTraitDefinition.SERVERS_PROPERTY), new String[] { pulsar.getPulsarBrokerUrl() });
			pulsarClient.setProperty(clientTraits.key(PulsarClientTraitDefinition.ENABLED_PROPERTY), true);
			pulsarClient.setProperty(clientTraits.key(MessageClientTraitDefinition.SUBSCRIBERS_PROPERTY), List.of(subscriber));

			tx.success();

			return new ClientSetup(pulsarClient.getUuid(), subscriber.getUuid());
		}
	}

	/**
	 * Creates a PulsarClient that only sends, and returns its id.
	 */
	private String createSendingClient() throws FrameworkException {

		try (final Tx tx = app.tx()) {

			final NodeInterface pulsarClient = app.create(StructrTraits.PULSAR_CLIENT, "client");

			pulsarClient.setProperty(Traits.of(StructrTraits.PULSAR_CLIENT).key(PulsarClientTraitDefinition.SERVERS_PROPERTY), new String[] { pulsar.getPulsarBrokerUrl() });

			tx.success();

			return pulsarClient.getUuid();
		}
	}

	private void setEnabled(final String clientId, final boolean enabled) throws FrameworkException {

		try (final Tx tx = app.tx()) {

			app.getNodeById(StructrTraits.PULSAR_CLIENT, clientId).setProperty(Traits.of(StructrTraits.PULSAR_CLIENT).key(PulsarClientTraitDefinition.ENABLED_PROPERTY), enabled);

			tx.success();
		}
	}

	private MessageClient getClient(final String clientId) throws FrameworkException {

		return app.getNodeById(StructrTraits.PULSAR_CLIENT, clientId).as(MessageClient.class);
	}

	private void send(final String topic, final String value) throws Exception {

		try (final Producer<byte[]> producer = client.newProducer().topic(topic).create()) {

			producer.send(value.getBytes(StandardCharsets.UTF_8));
		}
	}

	/**
	 * Waits until the consumer of Structr is connected to the topic, so that it receives the messages sent from now on.
	 * A new subscription starts at the end of the topic, so a message sent earlier would be skipped.
	 */
	private void waitForConsumer(final String topic) {

		tryWithTimeout(() -> getConsumerCount(topic) > 0, () -> fail("The consumer must be connected to " + topic), 60_000, 200);
	}

	/**
	 * @return the number of consumers on the subscription of Structr, read from the statistics of the topic
	 */
	private int getConsumerCount(final String topic) {

		try {

			final HttpRequest request           = HttpRequest.newBuilder(URI.create(pulsar.getHttpServiceUrl() + "/admin/v2/persistent/public/default/" + topic + "/stats")).GET().build();
			final HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

			if (response.statusCode() != 200) {

				return 0;
			}

			final JsonObject subscriptions = JsonParser.parseString(response.body()).getAsJsonObject().getAsJsonObject("subscriptions");
			if (subscriptions == null || !subscriptions.has(SUBSCRIPTION)) {

				return 0;
			}

			final JsonElement consumers = subscriptions.getAsJsonObject(SUBSCRIPTION).get("consumers");

			return consumers instanceof JsonArray array ? array.size() : 0;

		} catch (Exception ex) {

			return 0;
		}
	}

	/**
	 * Reads the topic from the beginning until it contains the expected number of messages, then reads on for a while
	 * so that messages beyond the expected ones are found too.
	 */
	private List<String> readTopic(final String topic, final int expectedCount) throws Exception {

		final List<String> values = new ArrayList<>();

		try (final Consumer<byte[]> reader = client.newConsumer()
			.topic(topic)
			.subscriptionName("structr-test-reader-" + UUID.randomUUID())
			.subscriptionInitialPosition(SubscriptionInitialPosition.Earliest)
			.subscribe()) {

			final long deadline = System.currentTimeMillis() + 30_000;

			while (values.size() < expectedCount && System.currentTimeMillis() < deadline) {

				final Message<byte[]> message = reader.receive(500, TimeUnit.MILLISECONDS);
				if (message != null) {

					values.add(new String(message.getData(), StandardCharsets.UTF_8));
					reader.acknowledge(message);
				}
			}

			Message<byte[]> message;

			while ((message = reader.receive(2, TimeUnit.SECONDS)) != null) {

				values.add(new String(message.getData(), StandardCharsets.UTF_8));
				reader.acknowledge(message);
			}
		}

		return values;
	}

	private static String uniqueTopic() {

		return "structr-test-" + UUID.randomUUID();
	}
}
