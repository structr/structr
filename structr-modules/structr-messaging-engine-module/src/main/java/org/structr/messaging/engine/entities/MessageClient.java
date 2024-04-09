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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.graph.Cardinality;
import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonSchema;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.rest.RestMethodResult;
import org.structr.schema.SchemaService;
import org.structr.schema.action.EvaluationHints;

import java.net.URI;
import org.structr.core.api.AbstractMethod;
import org.structr.core.api.Methods;
import org.structr.core.api.Arguments;

public interface MessageClient extends NodeInterface {

	class Impl {

		static {

			final JsonSchema schema = SchemaService.getDynamicSchema();
			final JsonObjectType type = schema.addType("MessageClient");
			final JsonObjectType sub = schema.addType("MessageSubscriber");

			type.setImplements(URI.create("https://structr.org/v1.1/definitions/MessageClient"));

			type.relate(sub, "HAS_SUBSCRIBER", Cardinality.ManyToMany, "clients", "subscribers");

			type.addViewProperty(PropertyView.Public, "subscribers");
			type.addViewProperty(PropertyView.Ui, "subscribers");

			type.addPropertyGetter("subscribers", Iterable.class);
			type.addPropertySetter("subscribers", Iterable.class).addException("FrameworkException");

			type.addMethod("sendMessage")
				.setReturnType(RestMethodResult.class.getName())
				.addParameter("ctx", SecurityContext.class.getName())
				.addParameter("topic", String.class.getName())
				.addParameter("message", String.class.getName())
				.setSource("return " + MessageClient.class.getName() + ".sendMessage(this, topic, message, ctx);")
				.addException(FrameworkException.class.getName())
				.setDoExport(true);

			type.addMethod("subscribeTopic")
				.setReturnType(RestMethodResult.class.getName())
				.addParameter("ctx", SecurityContext.class.getName())
				.addParameter("topic", String.class.getName())
				.setSource("return " + MessageClient.class.getName() + ".subscribeTopic(this, topic, ctx);")
				.addException(FrameworkException.class.getName())
				.setDoExport(true);

			type.addMethod("unsubscribeTopic")
				.setReturnType(RestMethodResult.class.getName())
				.addParameter("ctx", SecurityContext.class.getName())
				.addParameter("topic", String.class.getName())
				.setSource("return " + MessageClient.class.getName() + ".unsubscribeTopic(this, topic, ctx);")
				.addException(FrameworkException.class.getName())
				.setDoExport(true);
		}
	}

	Iterable<MessageSubscriber> getSubscribers();

	void setSubscribers(Iterable<MessageSubscriber> subs) throws FrameworkException;

	static RestMethodResult sendMessage(MessageClient thisClient, final String topic, final String message, final SecurityContext securityContext) throws FrameworkException {

		final App app = StructrApp.getInstance();
		try (final Tx tx = app.tx()) {

			final Iterable<MessageSubscriber> subscribers = thisClient.getSubscribers();
			if (subscribers != null) {

				subscribers.forEach(sub -> {

					String subTopic = sub.getProperty(StructrApp.key(MessageSubscriber.class, "topic"));
					if (subTopic != null && (subTopic.equals(topic) || subTopic.equals("*"))) {

						try {

							final AbstractMethod method = Methods.resolveMethod(sub.getClass(), "onMessage");
							if (method != null) {

								final Arguments params = new Arguments();

								params.add("topic", topic);
								params.add("message", message);

								method.execute(securityContext, sub, params, new EvaluationHints());
							}

						} catch (FrameworkException e) {

							final Logger logger = LoggerFactory.getLogger(MessageClient.class);
							logger.warn("Could not invoke 'onMessage' method on MessageSubscriber: " + e.getMessage());
						}
					}
				});
			}

			tx.success();
		}

		return new RestMethodResult(200);
	}

	static RestMethodResult subscribeTopic(MessageClient client, final String topic, final SecurityContext securityContext) throws FrameworkException {

		return new RestMethodResult(200);
	}

	static RestMethodResult unsubscribeTopic(MessageClient client, final String topic, final SecurityContext securityContext) throws FrameworkException {

		return new RestMethodResult(200);
	}

}
