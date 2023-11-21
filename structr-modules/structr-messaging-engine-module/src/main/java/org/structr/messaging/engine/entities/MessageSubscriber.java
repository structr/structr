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
package org.structr.messaging.engine.entities;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonSchema;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.graph.NodeInterface;
import org.structr.core.script.Scripting;
import org.structr.rest.RestMethodResult;
import org.structr.schema.SchemaService;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.EvaluationHints;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import org.structr.core.api.MethodCall;
import org.structr.core.api.MethodSignature;
import org.structr.core.api.Methods;

public interface MessageSubscriber extends NodeInterface {

	class Impl {

		static {

			final JsonSchema schema = SchemaService.getDynamicSchema();
			final JsonObjectType type = schema.addType("MessageSubscriber");

			type.setImplements(URI.create("https://structr.org/v1.1/definitions/MessageSubscriber"));

			type.addStringProperty("topic", PropertyView.Public, PropertyView.Ui).setIndexed(true);
			type.addStringProperty("callback", PropertyView.Public, PropertyView.Ui);

			type.addPropertyGetter("topic", String.class);
			type.addPropertyGetter("callback", String.class);
			type.addPropertyGetter("clients", Iterable.class);

			type.overrideMethod("onCreation", true, MessageSubscriber.class.getName() + ".onCreation(this, arg0, arg1);");
			type.overrideMethod("onModification", true, MessageSubscriber.class.getName() + ".onModification(this, arg0, arg1, arg2);");

			type.addMethod("onMessage")
				.setReturnType(RestMethodResult.class.getName())
				.addParameter("ctx", SecurityContext.class.getName())
				.addParameter("topic", String.class.getName())
				.addParameter("message", String.class.getName())
				.setSource("return " + MessageSubscriber.class.getName() + ".onMessage(this, topic, message, ctx);")
				.addException(FrameworkException.class.getName())
				.setDoExport(true);

			type.addViewProperty(PropertyView.Public, "clients");
			type.addViewProperty(PropertyView.Ui, "clients");
		}
	}

	String getTopic();
	String getCallback();
	Iterable<MessageClient> getClients();

	static void subscribeOnAllClients(MessageSubscriber thisSubscriber, final SecurityContext securityContext) {

		if (!StringUtils.isEmpty(thisSubscriber.getTopic()) && (thisSubscriber.getTopic() != null)) {
			Map<String, Object> params = new HashMap<>();
			params.put("topic", thisSubscriber.getTopic());

			Iterable<MessageClient> clientsList = thisSubscriber.getClients();
			clientsList.forEach(client -> {

				try {

					final MethodSignature signature = Methods.getMethodSignatureOrNull(client.getClass(), client, "subscribeTopic");
					if (signature != null) {

						final MethodCall call = signature.createCall(params);

						call.execute(securityContext, new EvaluationHints());
					}

				} catch (FrameworkException e) {

					final Logger logger = LoggerFactory.getLogger(MessageSubscriber.class);
					logger.error("Could not invoke subscribeTopic on MessageClient: " + e.getMessage());
				}
			});
		}
	}

	static void onCreation(MessageSubscriber thisSubscriber, final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

		if (thisSubscriber.getProperty(StructrApp.key(MessageSubscriber.class, "topic")) != null) {
			subscribeOnAllClients(thisSubscriber, securityContext);
		}

	}

	static void onModification(MessageSubscriber thisSubscriber, final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

		if (modificationQueue.isPropertyModified(thisSubscriber, StructrApp.key(MessageSubscriber.class, "topic")) || modificationQueue.isPropertyModified(thisSubscriber, StructrApp.key(MessageSubscriber.class, "topic"))) {
			subscribeOnAllClients(thisSubscriber, securityContext);
		}

	}

	static RestMethodResult onMessage(MessageSubscriber thisSubscriber, final String topic, final String message, final SecurityContext securityContext) throws FrameworkException {

		if (!StringUtils.isEmpty(thisSubscriber.getCallback())) {

			String script = "${" + thisSubscriber.getCallback().trim() + "}";

			Map<String, Object> params = new HashMap<>();
			params.put("topic", topic);
			params.put("message", message);

			ActionContext ac = new ActionContext(securityContext, params);
			ac.setConstant("topic", topic);
			ac.setConstant("message", message);

			// FIXME: the code source in this call should be the schema method that this subscriber was compiled from.
			Scripting.evaluate(ac, thisSubscriber, script, "onMessage", null);
		}

		return new RestMethodResult(200);
	}

}
