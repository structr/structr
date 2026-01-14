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
import org.structr.messaging.traits.definitions.MessageClientTraitDefinition;
import org.structr.messaging.traits.definitions.MessageSubscriberTraitDefinition;
import org.structr.schema.action.ActionContext;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.testng.AssertJUnit.assertEquals;
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

}
