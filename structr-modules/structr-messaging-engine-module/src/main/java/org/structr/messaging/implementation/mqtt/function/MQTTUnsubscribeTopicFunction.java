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
package org.structr.messaging.implementation.mqtt.function;

import org.structr.common.error.FrameworkException;
import org.structr.messaging.implementation.mqtt.entity.MQTTClient;
import org.structr.schema.action.ActionContext;

public class MQTTUnsubscribeTopicFunction extends MessagingModuleFunction {

	public static final String ERROR_MESSAGE_MQTTUNSUBSCRIBE    = "Usage: ${mqtt_unsubscribe(client, topic)}. Example ${mqtt_unsubscribe(client, 'myTopic')}";
	public static final String ERROR_MESSAGE_MQTTUNSUBSCRIBE_JS = "Usage: ${{Structr.mqtt_unsubscribe(client, topic)}}. Example ${{Structr.mqtt_unsubscribe(client, topic)}}";

	@Override
	public String getName() {
		return "mqtt_unsubscribe";
	}

	@Override
	public String getSignature() {
		return "client, topic";
	}

	@Override
	public Object apply(ActionContext ctx, Object caller, Object[] sources) throws FrameworkException {
		if (sources != null && sources.length == 2 && sources[0] != null && sources[1] != null) {

			MQTTClient client = null;
			if(sources[0] instanceof MQTTClient){
				client = (MQTTClient)sources[0];
			}

			if(client == null){

				logParameterError(caller, sources, ctx.isJavaScriptContext());
				return "";
			}

			MQTTClient.unsubscribeTopic(client, sources[1].toString());

		} else {

			logParameterError(caller, sources, ctx.isJavaScriptContext());
		}

		return "";
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_MQTTUNSUBSCRIBE_JS : ERROR_MESSAGE_MQTTUNSUBSCRIBE);
	}

	@Override
	public String shortDescription() {
		return "Unsubscribes given topic on given mqtt client.";
	}
}
