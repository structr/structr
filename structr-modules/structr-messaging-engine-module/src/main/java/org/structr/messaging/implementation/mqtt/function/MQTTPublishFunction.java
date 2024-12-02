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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.messaging.implementation.mqtt.entity.MQTTClient;
import org.structr.schema.action.ActionContext;

public class MQTTPublishFunction extends MessagingModuleFunction {

	private static final Logger logger = LoggerFactory.getLogger(MQTTPublishFunction.class.getName());

	public static final String ERROR_MESSAGE_MQTTPUBLISH    = "Usage: ${mqtt_publish(client, topic, message)}. Example ${mqtt_publish(client, 'myTopic', 'myMessage')}";
	public static final String ERROR_MESSAGE_MQTTPUBLISH_JS = "Usage: ${{Structr.mqtt_publish(client, topic, message)}}. Example ${{Structr.mqtt_publish(client, topic, message)}}";

	@Override
	public String getName() {
		return "mqtt_publish";
	}

	@Override
	public String getSignature() {
		return "client, topic, message";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		if (sources != null && sources.length == 3 && sources[0] != null && sources[1] != null && sources[2] != null) {

			MQTTClient client = null;
			if(sources[0] instanceof MQTTClient){
				client = (MQTTClient)sources[0];
			}

			if(client == null){

				logParameterError(caller, sources, ctx.isJavaScriptContext());
				return "";
			}

			client.sendMessage(ctx.getSecurityContext(), sources[1].toString(), sources[2].toString());

		} else {

			logParameterError(caller, sources, ctx.isJavaScriptContext());
		}

		return "";
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_MQTTPUBLISH_JS : ERROR_MESSAGE_MQTTPUBLISH);
	}

	@Override
	public String shortDescription() {
		return "Publishes message on given mqtt client with given topic.";
	}
}
