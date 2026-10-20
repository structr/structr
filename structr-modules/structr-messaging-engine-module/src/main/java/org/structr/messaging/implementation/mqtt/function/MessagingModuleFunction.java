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
package org.structr.messaging.implementation.mqtt.function;

import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.StructrTraits;
import org.structr.docs.ontology.FunctionCategory;
import org.structr.messaging.implementation.mqtt.entity.MQTTClient;
import org.structr.schema.action.Function;

public abstract class MessagingModuleFunction extends Function<Object, Object> {

	@Override
	public String getRequiredModule() {

        	return "messaging-module";
	}

	@Override
	public FunctionCategory getCategory() {

		return FunctionCategory.MQTT;
	}

	/**
	 * Returns the MQTT client that a script passed, or null. A script passes the node itself, not the MQTTClient wrapper
	 * that Java code works with, so the node is converted instead of being checked with instanceof.
	 */
	protected MQTTClient toMQTTClient(final Object source) {

		if (source instanceof NodeInterface node && node.is(StructrTraits.MQTT_CLIENT)) {

			return node.as(MQTTClient.class);
		}

		return null;
	}
}
