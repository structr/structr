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

import org.structr.api.config.Settings;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.test.web.StructrUiTest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MessagingTestBase extends StructrUiTest {

	// no @BeforeClass: StructrUiTest.setup() already invokes createSchema(), and annotating the
	// override as well ran the whole schema registration twice for every class.
	public void createSchema() {

		/*
		already done in MessageEngineModule, is this really necessary?

		StructrTraits.registerRelationshipType(StructrTraits.MESSAGE_CLIENT_HAS_MESSAGE_SUBSCRIBER, new MessageClientHASMessageSubscriber());

		StructrTraits.registerNodeType(StructrTraits.MESSAGE_CLIENT,     new MessageClientTraitDefinition());
		StructrTraits.registerNodeType(StructrTraits.MESSAGE_SUBSCRIBER, new MessageSubscriberTraitDefinition());
		StructrTraits.registerNodeType(StructrTraits.KAFKA_CLIENT,       new MessageClientTraitDefinition(), new KafkaClientTraitDefinition());
		StructrTraits.registerNodeType(StructrTraits.MQTT_CLIENT,        new MessageClientTraitDefinition(), new MQTTClientTraitDefinition());
		StructrTraits.registerNodeType(StructrTraits.PULSAR_CLIENT,      new MessageClientTraitDefinition(), new PulsarClientTraitDefinition());

		// create new schema instance that includes the modified root schema
		TraitsManager.replaceCurrentInstance(TraitsManager.createCopyOfRootInstance());
		*/
	}

	/**
	 * The callback of the subscribers in the broker tests. It appends each message to the name of the subscriber, so
	 * that a message that is processed twice shows up twice. MQTT runs the callbacks of concurrent messages in parallel,
	 * where they would overwrite each other, so tests with several MQTT messages in flight create a group per message.
	 */
	protected static final String APPENDING_CALLBACK = "{ $.this.name = ($.this.name ?? '') + $.message + ';'; }";

	/**
	 * Returns a callback that appends the message to the name of the subscriber, but ends its first attempt with a
	 * RetryException, which is what a transaction that the database terminated, e.g. to resolve a deadlock, ends with.
	 * The attempts are counted in the application store, which a rollback does not undo. The script can only create the
	 * exception if the class is allowed, see {@link #allowRetryExceptionInScripts()}.
	 */
	protected static String failingOnceCallback() {

		final String key = "attempts-" + UUID.randomUUID();

		return "{ const attempts = ($.applicationStore['" + key + "'] ?? 0) + 1; $.applicationStore['" + key + "'] = attempts; "
			+ "if (attempts === 1) { throw new (Java.type('org.structr.api.RetryException'))('simulated'); } "
			+ "$.this.name = ($.this.name ?? '') + $.message + ';'; }";
	}

	/**
	 * Allows scripts to create a RetryException and returns the previous value of the setting, to be restored afterwards.
	 */
	protected static String allowRetryExceptionInScripts() {

		final String previous = Settings.AllowedHostClasses.getValue();

		Settings.AllowedHostClasses.setValue("org.structr.api.RetryException");

		return previous;
	}

	protected String getSubscriberName(final String uuid) {

		try (final Tx tx = app.tx()) {

			// the raw property, getName() falls back to the uuid when the name is not set
			final NodeInterface subscriber = app.getNodeById(StructrTraits.MESSAGE_SUBSCRIBER, uuid);
			final String name              = subscriber != null ? subscriber.getProperty(Traits.of(StructrTraits.MESSAGE_SUBSCRIBER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY)) : null;

			tx.success();

			return name;

		} catch (FrameworkException fex) {

			throw new RuntimeException(fex);
		}
	}

	/**
	 * Returns the names of the groups whose name starts with the given prefix. Tests whose callbacks create a group for
	 * each message they process count the processed messages with it. Group names are unique, so the callbacks append a
	 * random suffix after a #, otherwise a message that is processed twice would fail the second time instead of showing
	 * up twice. The suffix is cut off here.
	 */
	protected List<String> getGroupNames(final String prefix) {

		try (final Tx tx = app.tx()) {

			final List<String> names = new ArrayList<>();

			for (final NodeInterface group : app.nodeQuery(StructrTraits.GROUP).getAsList()) {

				final String name = group.getName();
				if (name != null && name.startsWith(prefix)) {

					names.add(name.substring(0, name.lastIndexOf('#')));
				}
			}

			tx.success();

			return names;

		} catch (FrameworkException fex) {

			throw new RuntimeException(fex);
		}
	}

	protected static void sleep(final long millis) {

		try {

			Thread.sleep(millis);

		} catch (InterruptedException iex) {

			Thread.currentThread().interrupt();
		}
	}
}
