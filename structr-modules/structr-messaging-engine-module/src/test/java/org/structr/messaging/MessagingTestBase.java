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

import org.structr.test.web.StructrUiTest;
import org.testng.annotations.BeforeMethod;

public class MessagingTestBase extends StructrUiTest {

	@BeforeMethod(firstTimeOnly = true)
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
}
