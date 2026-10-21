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

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.ConsumerGroupState;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.structr.api.RetryException;
import org.structr.api.config.Settings;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.messaging.engine.entities.MessageClient;
import org.structr.messaging.engine.entities.MessageSubscriber;
import org.structr.messaging.traits.definitions.KafkaClientTraitDefinition;
import org.structr.messaging.traits.definitions.MessageClientTraitDefinition;
import org.structr.messaging.traits.definitions.MessageSubscriberTraitDefinition;
import org.structr.schema.action.ActionContext;
import org.testcontainers.kafka.KafkaContainer;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.fail;

/**
 * Tests the delivery semantics of KafkaClient against a Kafka test container: incoming records are processed once,
 * not sent back to their topic, and their offsets are committed after processing; outgoing records are sent after
 * the commit of the sending transaction.
 */
public class KafkaTest extends MessagingTestBase {

	private static KafkaContainer kafka = null;
	private static Admin admin          = null;

	@Parameters("testDatabaseConnection")
	@BeforeClass(alwaysRun = true)
	@Override
	public void setup(@Optional final String testDatabaseConnection) {

		kafka = new KafkaContainer("apache/kafka:3.9.1");

		kafka.start();

		admin = Admin.create(Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers()));

		super.setup(testDatabaseConnection);
	}

	@AfterClass(alwaysRun = true)
	@Override
	public void teardown() throws Exception {

		super.teardown();

		if (admin != null) {

			admin.close();
			admin = null;
		}

		if (kafka != null) {

			kafka.stop();
			kafka = null;
		}
	}

	@Test
	public void testIncomingRecordIsProcessedOnceAndCommitted() throws Exception {

		final String topic      = createTopic();
		final String groupId    = uniqueGroupId();
		final String subscriber = createConsumingClient(topic, groupId, APPENDING_CALLBACK);

		waitForAssignment(groupId, topic);

		send(topic, "first");

		tryWithTimeout(() -> "first;".equals(getSubscriberName(subscriber)), () -> fail("The record must be processed"), 30_000, 100);
		tryWithTimeout(() -> getCommittedOffset(groupId, topic) == 1, () -> fail("The offset must be committed after the record is processed"), 30_000, 100);

		sleep(2_000);

		assertEquals("The record must be processed exactly once", "first;", getSubscriberName(subscriber));
		assertEquals("The received record must not be sent back to its topic", List.of("first"), readTopic(topic, 1));
	}

	@Test
	public void testSentRecordIsSentOnlyAfterCommit() throws Exception {

		final String topic           = createTopic();
		final String client          = createSendingClient();
		final AtomicInteger attempts = new AtomicInteger();

		// rolled back: the record must never be sent
		try (final Tx tx = app.tx()) {

			getClient(client).sendMessage(new ActionContext(securityContext), topic, "rolled-back");
		}

		try (final Tx tx = app.tx()) {

			getClient(client).sendMessage(new ActionContext(securityContext), topic, "committed");

			tx.success();
		}

		// repeated: the record must be sent once, the database terminating the first attempt is simulated by a RetryException
		RetryException.repeat(() -> {

			try (final Tx tx = app.tx()) {

				getClient(client).sendMessage(new ActionContext(securityContext), topic, "repeated");

				if (attempts.incrementAndGet() == 1) {

					throw new RetryException("simulated");
				}

				tx.success();
			}
		});

		assertEquals("The transaction must be repeated once", 2, attempts.get());
		assertEquals("Only the records of the committed transactions must be sent, each once", List.of("committed", "repeated"), readTopic(topic, 2));
	}

	@Test
	public void testFailingRecordDoesNotStopLaterRecords() throws Exception {

		final String topic      = createTopic();
		final String groupId    = uniqueGroupId();
		final String callback   = "{ if ($.message === 'failing') { throw new Error('rejected by the test'); } $.this.name = ($.this.name ?? '') + $.message + ';'; }";
		final String subscriber = createConsumingClient(topic, groupId, callback);

		waitForAssignment(groupId, topic);

		send(topic, "before");
		send(topic, "failing");
		send(topic, "after");

		tryWithTimeout(() -> "before;after;".equals(getSubscriberName(subscriber)), () -> fail("The records around the failing one must be processed, got " + getSubscriberName(subscriber)), 30_000, 100);

		// the failing record counts as processed, so the offset moves past it
		tryWithTimeout(() -> getCommittedOffset(groupId, topic) == 3, () -> fail("The offset must be committed past the failing record"), 30_000, 100);
	}

	@Test
	public void testReplyFromCallbackIsSentOnce() throws Exception {

		final String requestTopic = createTopic();
		final String replyTopic   = createTopic();
		final String groupId      = uniqueGroupId();
		final String callback     = "{ $.this.name = ($.this.name ?? '') + $.message + ';'; $.first($.this.clients).sendMessage({ topic: '" + replyTopic + "', message: 'reply:' + $.message }); }";
		final String subscriber   = createConsumingClient(requestTopic, groupId, callback);

		waitForAssignment(groupId, requestTopic);

		send(requestTopic, "request");

		assertEquals("The callback must send the reply once", List.of("reply:request"), readTopic(replyTopic, 1));
		assertEquals("The request must be processed once", "request;", getSubscriberName(subscriber));
		assertEquals("Neither the request nor the reply must be sent back to the request topic", List.of("request"), readTopic(requestTopic, 1));
	}

	@Test
	public void testRecordsSentWhileDisabledAreProcessedAfterEnabling() throws Exception {

		final String topic      = createTopic();
		final String groupId    = uniqueGroupId();
		final String subscriber = createConsumingClient(topic, groupId, APPENDING_CALLBACK);

		waitForAssignment(groupId, topic);

		setEnabled(subscriber, false);

		// a poll that is running when the client is disabled still delivers its records, the next one does not start
		sleep(2_000);

		send(topic, "while-disabled");

		sleep(2_000);

		assertNull("A disabled client must not process records", getSubscriberName(subscriber));

		setEnabled(subscriber, true);

		tryWithTimeout(() -> "while-disabled;".equals(getSubscriberName(subscriber)), () -> fail("The record sent while the client was disabled must be processed after enabling it"), 30_000, 100);
	}

	@Test
	public void testBurstOfRecordsIsProcessedExactlyOnce() throws Exception {

		final int count      = 50;
		final String prefix  = "burst-" + UUID.randomUUID() + "-";
		final String topic   = createTopic();
		final String groupId = uniqueGroupId();

		createConsumingClient(topic, groupId, "{ $.create('Group', { name: $.message + '#' + Math.random() }); }");

		waitForAssignment(groupId, topic);

		for (int i = 0; i < count; i++) {

			send(topic, prefix + i);
		}

		tryWithTimeout(() -> getGroupNames(prefix).size() >= count, () -> fail("All records must be processed, got " + getGroupNames(prefix).size()), 60_000, 200);
		tryWithTimeout(() -> getCommittedOffset(groupId, topic) == count, () -> fail("The offset must be committed after the last record"), 30_000, 100);

		sleep(2_000);

		final List<String> names = getGroupNames(prefix);

		assertEquals("Each record must be processed exactly once", count, names.size());
		assertEquals("No record may be processed twice", count, new HashSet<>(names).size());
	}

	@Test
	public void testConsumerFollowsTopicChangeOfSubscriber() throws Exception {

		final String oldTopic   = createTopic();
		final String newTopic   = createTopic();
		final String groupId    = uniqueGroupId();
		final String subscriber = createConsumingClient(oldTopic, groupId, APPENDING_CALLBACK);

		waitForAssignment(groupId, oldTopic);

		try (final Tx tx = app.tx()) {

			app.getNodeById(StructrTraits.MESSAGE_SUBSCRIBER, subscriber).setProperty(Traits.of(StructrTraits.MESSAGE_SUBSCRIBER).key(MessageSubscriberTraitDefinition.TOPIC_PROPERTY), newTopic);

			tx.success();
		}

		waitForAssignment(groupId, newTopic);

		send(newTopic, "on-new-topic");

		tryWithTimeout(() -> "on-new-topic;".equals(getSubscriberName(subscriber)), () -> fail("The record on the new topic must be processed"), 30_000, 100);
	}

	@Test
	public void testDeletedClientStopsConsuming() throws Exception {

		final String topic      = createTopic();
		final String groupId    = uniqueGroupId();
		final String subscriber = createConsumingClient(topic, groupId, APPENDING_CALLBACK);

		waitForAssignment(groupId, topic);

		try (final Tx tx = app.tx()) {

			for (final MessageClient client : app.getNodeById(StructrTraits.MESSAGE_SUBSCRIBER, subscriber).as(MessageSubscriber.class).getClients()) {

				app.delete(client);
			}

			tx.success();
		}

		// the consumer leaves the group when it is closed
		tryWithTimeout(() -> getGroupMemberCount(groupId) == 0, () -> fail("Deleting the client must close its consumer"), 30_000, 200);

		send(topic, "after-deletion");

		sleep(2_000);

		assertNull("The subscriber of a deleted client must not receive records", getSubscriberName(subscriber));
	}

	@Test
	public void testClientWithoutGroupIdReceivesRecords() throws Exception {

		// a client without a group id consumes in the group structr-<uuid of the client>
		final String topic      = createTopic();
		final String subscriber = createConsumingClient(topic, null, APPENDING_CALLBACK);
		final String groupId    = "structr-" + getClientIdOf(subscriber);

		waitForAssignment(groupId, topic);

		send(topic, "without-group-id");

		tryWithTimeout(() -> "without-group-id;".equals(getSubscriberName(subscriber)), () -> fail("A client without a group id must process the record"), 30_000, 100);
	}

	@Test
	public void testClientsShareTheRecordsOfTheirGroupAndEachGroupGetsAll() throws Exception {

		// clients with the same group id share the records of a topic, clients with different group ids each get all of
		// them, as the documentation describes it for distributing the processing over several instances
		final int count       = 10;
		final String prefix   = "groups-" + UUID.randomUUID() + "-";
		final String topic    = createTopic();
		final String shared   = uniqueGroupId();
		final String separate = uniqueGroupId();
		final String callback = "{ $.create('Group', { name: $.message + '#' + Math.random() }); }";

		createConsumingClient(topic, shared, callback);
		createConsumingClient(topic, shared, callback);
		createConsumingClient(topic, separate, callback);

		// a rebalance while records are processed may deliver them again, which is correct, but would spoil the count
		tryWithTimeout(() -> getGroupMemberCount(shared) == 2 && isStable(shared), () -> fail("Both clients of the shared group must join it"), 60_000, 200);

		waitForAssignment(shared, topic);
		waitForAssignment(separate, topic);

		for (int i = 0; i < count; i++) {

			send(topic, prefix + i);
		}

		tryWithTimeout(() -> getGroupNames(prefix).size() >= 2 * count, () -> fail("Each group must process all records, got " + getGroupNames(prefix).size()), 60_000, 200);

		sleep(2_000);

		final List<String> names = getGroupNames(prefix);

		assertEquals("Each record must be processed once per group", 2 * count, names.size());
		assertEquals("Each record must be processed in both groups", count, new HashSet<>(names).size());
	}

	@Test
	public void testTerminatedTransactionOfARecordIsRepeated() throws Exception {

		final String previous = allowRetryExceptionInScripts();

		try {

			final String topic      = createTopic();
			final String groupId    = uniqueGroupId();
			final String subscriber = createConsumingClient(topic, groupId, failingOnceCallback());

			waitForAssignment(groupId, topic);

			send(topic, "terminated");

			tryWithTimeout(() -> "terminated;".equals(getSubscriberName(subscriber)), () -> fail("The record must be processed in the repeated transaction"), 30_000, 100);

			sleep(1_000);

			assertEquals("The record must be processed once", "terminated;", getSubscriberName(subscriber));

		} finally {

			Settings.AllowedHostClasses.setValue(previous);
		}
	}

	// ----- private methods -----
	private String createTopic() throws Exception {

		final String topic = "structr-test-" + UUID.randomUUID();

		admin.createTopics(List.of(new NewTopic(topic, 1, (short)1))).all().get();

		return topic;
	}

	/**
	 * Creates an enabled KafkaClient with a subscriber with the given callback for the given topic, and returns the id
	 * of the subscriber.
	 */
	private String createConsumingClient(final String topic, final String groupId, final String callback) throws FrameworkException {

		final Traits clientTraits     = Traits.of(StructrTraits.KAFKA_CLIENT);
		final Traits subscriberTraits = Traits.of(StructrTraits.MESSAGE_SUBSCRIBER);

		try (final Tx tx = app.tx()) {

			// the subscriber has no name, the appending callback adds the records to it
			final NodeInterface subscriber = app.create(StructrTraits.MESSAGE_SUBSCRIBER);
			final NodeInterface client     = app.create(StructrTraits.KAFKA_CLIENT, "client");

			subscriber.setProperty(subscriberTraits.key(MessageSubscriberTraitDefinition.TOPIC_PROPERTY), topic);
			subscriber.setProperty(subscriberTraits.key(MessageSubscriberTraitDefinition.CALLBACK_PROPERTY), callback);

			client.setProperty(clientTraits.key(KafkaClientTraitDefinition.SERVERS_PROPERTY), new String[] { kafka.getBootstrapServers() });
			// without a group id, the client uses a group of its own
			if (groupId != null) {

				client.setProperty(clientTraits.key(KafkaClientTraitDefinition.GROUP_ID_PROPERTY), groupId);
			}

			client.setProperty(clientTraits.key(KafkaClientTraitDefinition.ENABLED_PROPERTY), true);
			client.setProperty(clientTraits.key(MessageClientTraitDefinition.SUBSCRIBERS_PROPERTY), List.of(subscriber));

			tx.success();

			return subscriber.getUuid();
		}
	}

	/**
	 * Creates a KafkaClient that only sends, and returns its id.
	 */
	private String createSendingClient() throws FrameworkException {

		try (final Tx tx = app.tx()) {

			final NodeInterface client = app.create(StructrTraits.KAFKA_CLIENT, "client");

			client.setProperty(Traits.of(StructrTraits.KAFKA_CLIENT).key(KafkaClientTraitDefinition.SERVERS_PROPERTY), new String[] { kafka.getBootstrapServers() });

			tx.success();

			return client.getUuid();
		}
	}

	/**
	 * Enables or disables the client of the given subscriber.
	 */
	private void setEnabled(final String subscriberId, final boolean enabled) throws FrameworkException {

		try (final Tx tx = app.tx()) {

			final NodeInterface subscriber = app.getNodeById(StructrTraits.MESSAGE_SUBSCRIBER, subscriberId);

			for (final MessageClient client : subscriber.as(MessageSubscriber.class).getClients()) {

				client.setProperty(Traits.of(StructrTraits.KAFKA_CLIENT).key(KafkaClientTraitDefinition.ENABLED_PROPERTY), enabled);
			}

			tx.success();
		}
	}

	/**
	 * Waits until the consumer of the group is assigned to the topic, so that it receives the records sent from now on.
	 * The consumer commits its position after each poll, so a committed offset shows that it is assigned.
	 */
	private void waitForAssignment(final String groupId, final String topic) {

		tryWithTimeout(() -> getCommittedOffset(groupId, topic) >= 0, () -> fail("The consumer must be assigned to the topic"), 60_000, 200);
	}

	/**
	 * @return the number of members of the consumer group, or -1 if it cannot be described
	 */
	private int getGroupMemberCount(final String groupId) {

		try {

			return admin.describeConsumerGroups(List.of(groupId)).all().get().get(groupId).members().size();

		} catch (Exception ex) {

			return -1;
		}
	}

	private String getClientIdOf(final String subscriberId) throws FrameworkException {

		try (final Tx tx = app.tx()) {

			final String clientId = app.getNodeById(StructrTraits.MESSAGE_SUBSCRIBER, subscriberId).as(MessageSubscriber.class).getClients().iterator().next().getUuid();

			tx.success();

			return clientId;
		}
	}

	private boolean isStable(final String groupId) {

		try {

			return admin.describeConsumerGroups(List.of(groupId)).all().get().get(groupId).state() == ConsumerGroupState.STABLE;

		} catch (Exception ex) {

			return false;
		}
	}

	private static String uniqueGroupId() {

		return "structr-test-" + UUID.randomUUID();
	}

	private MessageClient getClient(final String clientId) throws FrameworkException {

		return app.getNodeById(StructrTraits.KAFKA_CLIENT, clientId).as(MessageClient.class);
	}

	private void send(final String topic, final String value) throws Exception {

		final Map<String, Object> config = Map.of(
			ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
			ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName(),
			ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName()
		);

		try (final KafkaProducer<String, String> producer = new KafkaProducer<>(config)) {

			producer.send(new ProducerRecord<>(topic, value)).get();
		}
	}

	/**
	 * Reads the topic from the beginning until it contains the expected number of records, then reads on for a while
	 * so that records beyond the expected ones are found too.
	 */
	private List<String> readTopic(final String topic, final int expectedCount) {

		final Map<String, Object> config = Map.of(
			ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
			ConsumerConfig.GROUP_ID_CONFIG, "structr-test-reader-" + UUID.randomUUID(),
			ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
			ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName(),
			ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName()
		);

		final List<String> values = new ArrayList<>();

		try (final KafkaConsumer<String, String> consumer = new KafkaConsumer<>(config)) {

			consumer.subscribe(List.of(topic));

			final long deadline = System.currentTimeMillis() + 30_000;

			while (values.size() < expectedCount && System.currentTimeMillis() < deadline) {

				for (final ConsumerRecord<String, String> record : consumer.poll(Duration.ofMillis(500))) {

					values.add(record.value());
				}
			}

			for (final ConsumerRecord<String, String> record : consumer.poll(Duration.ofSeconds(2))) {

				values.add(record.value());
			}
		}

		return values;
	}

	/**
	 * @return the committed offset of the group on the single partition of the topic, or -1 if there is none
	 */
	private long getCommittedOffset(final String groupId, final String topic) {

		try {

			final Map<TopicPartition, OffsetAndMetadata> offsets = admin.listConsumerGroupOffsets(groupId).partitionsToOffsetAndMetadata().get();
			final OffsetAndMetadata offset                       = offsets.get(new TopicPartition(topic, 0));

			return offset != null ? offset.offset() : -1;

		} catch (Exception ex) {

			return -1;
		}
	}
}
