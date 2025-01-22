/*
 * Copyright (C) 2010-2024 Structr GmbH
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
import org.structr.core.app.StructrApp;
import org.structr.core.graph.Tx;
import org.structr.core.script.Scripting;
import org.structr.messaging.engine.entities.MessageClient;
import org.structr.messaging.engine.entities.MessageSubscriber;
import org.structr.schema.action.ActionContext;
import org.structr.test.web.StructrUiTest;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.fail;

public class SimpleMessagingTest extends StructrUiTest {

	@Test
	public void test01() {

		try(final Tx tx = app.tx()) {

			MessageClient client1 = app.create("MessageClient", "client1");
			MessageSubscriber sub = app.create("MessageSubscriber", "sub");

			List<MessageSubscriber> subList = new ArrayList<>();
			subList.add(sub);

			client1.setProperty(StructrApp.key(MessageClient.class, "subscribers"), subList);
			sub.setProperty(StructrApp.key(MessageSubscriber.class, "topic"), "test");
			sub.setProperty(StructrApp.key(MessageSubscriber.class, "callback"), "set(this, 'name', retrieve('message'))");

			Scripting.replaceVariables(new ActionContext(securityContext, null), client1, "${{Structr.log('Sending message'); Structr.get('this').sendMessage('test','testmessage');}}");

			assertEquals("testmessage", sub.getName());

			tx.success();
		} catch (FrameworkException ex) {
			fail("Exception during test: " + ex.getMessage());
		}

	}

}
