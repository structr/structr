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

import org.structr.core.traits.StructrTraits;
import org.structr.messaging.engine.relation.MessageClientHASMessageSubscriber;
import org.structr.messaging.traits.definitions.*;
import org.structr.test.web.StructrUiTest;
import org.testng.annotations.BeforeMethod;

public class MessagingTestBase extends StructrUiTest {

	@BeforeMethod(firstTimeOnly = true)
	public void createSchema() {

		StructrTraits.registerRelationshipType("MessageClientHASMessageSubscriber", new MessageClientHASMessageSubscriber());

		StructrTraits.registerNodeType("MessageClient",     new MessageClientTraitDefinition());
		StructrTraits.registerNodeType("MessageSubscriber", new MessageSubscriberTraitDefinition());
		StructrTraits.registerNodeType("KafkaClient",       new MessageClientTraitDefinition(), new KafkaClientTraitDefinition());
		StructrTraits.registerNodeType("MQTTClient",        new MessageClientTraitDefinition(), new MQTTClientTraitDefinition());
		StructrTraits.registerNodeType("PulsarClient",      new MessageClientTraitDefinition(), new PulsarClientTraitDefinition());
	}
}
