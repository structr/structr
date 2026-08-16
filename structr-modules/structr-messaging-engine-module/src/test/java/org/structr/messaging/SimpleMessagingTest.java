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

import org.structr.common.error.FrameworkException;
import org.structr.core.graph.Tx;
import org.structr.core.script.Scripting;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.messaging.engine.entities.MessageClient;
import org.structr.messaging.engine.entities.MessageSubscriber;
import org.structr.messaging.traits.definitions.MQTTClientTraitDefinition;
import org.structr.messaging.traits.definitions.KafkaClientTraitDefinition;
import org.structr.messaging.traits.definitions.MessageClientTraitDefinition;
import org.structr.messaging.traits.definitions.MessageSubscriberTraitDefinition;
import org.structr.schema.action.ActionContext;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

public class SimpleMessagingTest extends MessagingTestBase  {

	@Test
	public void test01() {

		try(final Tx tx = app.tx()) {

			final List<MessageSubscriber> subList = new ArrayList<>();
			final MessageClient client1           = app.create(StructrTraits.MESSAGE_CLIENT, "client1").as(MessageClient.class);
			final MessageSubscriber sub           = app.create(StructrTraits.MESSAGE_SUBSCRIBER, "sub").as(MessageSubscriber.class);
			final Traits subscriberTraits         = Traits.of(StructrTraits.MESSAGE_SUBSCRIBER);
			final Traits clientTraits             = Traits.of(StructrTraits.MESSAGE_CLIENT);

			subList.add(sub);

			client1.setProperty(clientTraits.key(MessageClientTraitDefinition.SUBSCRIBERS_PROPERTY), subList);

			sub.setProperty(subscriberTraits.key(MessageSubscriberTraitDefinition.TOPIC_PROPERTY),    "test");
			sub.setProperty(subscriberTraits.key(MessageSubscriberTraitDefinition.CALLBACK_PROPERTY), "set(this, 'name', retrieve('message'))");

			Scripting.replaceVariables(new ActionContext(securityContext, null), client1, "${{Structr.log('Sending message'); Structr.get('this').sendMessage({ topic: 'test', message: 'testmessage' });}}");

			assertEquals("testmessage", sub.getName());

			tx.success();

		} catch (FrameworkException ex) {

			fail("Exception during test: " + ex.getMessage());
		}

	}

	@Test
	public void testIsEnabledForAllClientTypes() {

		// The client types spell the flag differently (MQTTClient/XMPPClient: isEnabled, KafkaClient and
		// PulsarClient: enabled). getIsEnabled() is declared on MessageClient and is called for all of
		// them (MessageEngineModule, KafkaClientTraitWrapper), so it has to work for each type.

		try (final Tx tx = app.tx()) {

			final MessageClient mqtt  = app.create(StructrTraits.MQTT_CLIENT,  "mqtt1").as(MessageClient.class);

			// required, but the client is never enabled here so it does not connect
			mqtt.setProperty(Traits.of(StructrTraits.MQTT_CLIENT).key(MQTTClientTraitDefinition.MAIN_BROKER_URL_PROPERTY), "tcp://localhost:1883");
			final MessageClient kafka = app.create(StructrTraits.KAFKA_CLIENT, "kafka1").as(MessageClient.class);

			// unset means not enabled
			assertEquals("MQTTClient must report its enabled state",  false, mqtt.getIsEnabled());
			assertEquals("KafkaClient must report its enabled state", false, kafka.getIsEnabled());

			// only the Kafka client is switched on here: enabling an MQTT client makes it connect to its
			// broker, which is not what this test is about
			kafka.setProperty(Traits.of(StructrTraits.KAFKA_CLIENT).key(KafkaClientTraitDefinition.ENABLED_PROPERTY), true);

			assertEquals("KafkaClient must report its enabled state", true, kafka.getIsEnabled());

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testSubscriberCallbackIsExecutedWithCodeSource() {

		// The callback runs with the subscriber as its code source, so that a scripting error is reported
		// with the node that holds the script (visible in the log as "MessageSubscriber[<uuid>]:onMessage").
		// This test guards the normal path: passing a code source must not change what the callback does.

		try (final Tx tx = app.tx()) {

			final List<MessageSubscriber> subList = new ArrayList<>();
			final MessageClient client            = app.create(StructrTraits.MESSAGE_CLIENT, "client2").as(MessageClient.class);
			final MessageSubscriber sub           = app.create(StructrTraits.MESSAGE_SUBSCRIBER, "sub2").as(MessageSubscriber.class);
			final Traits subscriberTraits         = Traits.of(StructrTraits.MESSAGE_SUBSCRIBER);

			subList.add(sub);

			client.setProperty(Traits.of(StructrTraits.MESSAGE_CLIENT).key(MessageClientTraitDefinition.SUBSCRIBERS_PROPERTY), subList);

			sub.setProperty(subscriberTraits.key(MessageSubscriberTraitDefinition.TOPIC_PROPERTY),    "test");
			sub.setProperty(subscriberTraits.key(MessageSubscriberTraitDefinition.CALLBACK_PROPERTY), "set(this, 'name', retrieve('message'))");

			sub.onMessage(new ActionContext(securityContext, null), "test", "callback-was-executed");

			assertEquals("The callback must be executed", "callback-was-executed", sub.getName());

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}
	}
}
