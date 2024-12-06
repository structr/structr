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
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.View;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.Export;
import org.structr.core.api.AbstractMethod;
import org.structr.core.api.Arguments;
import org.structr.core.api.Methods;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.property.Property;
import org.structr.core.property.StartNodes;
import org.structr.core.property.StringProperty;
import org.structr.core.script.Scripting;
import org.structr.messaging.engine.relation.MessageClientHASMessageSubscriber;
import org.structr.rest.RestMethodResult;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.EvaluationHints;

import java.util.HashMap;
import java.util.Map;

public class MessageSubscriber extends AbstractNode {

	public static final Property<Iterable<MessageClient>> clientsProperty = new StartNodes<>("clients", MessageClientHASMessageSubscriber.class);

	public static final Property<String> topicProperty    = new StringProperty("topic").indexed();
	public static final Property<String> callbackProperty = new StringProperty("callback");

	public static final View defaultView = new View(MessageSubscriber.class, PropertyView.Public,
		topicProperty, callbackProperty, clientsProperty
	);

	public static final View uiView = new View(MessageSubscriber.class, PropertyView.Ui,
		topicProperty, callbackProperty, clientsProperty
	);

	public String getTopic() {
		return getProperty(topicProperty);
	}

	public String getCallback() {
		return getProperty(callbackProperty);
	}

	public Iterable<MessageClient> getClients() {
		return getProperty(clientsProperty);
	}

	public void subscribeOnAllClients(final SecurityContext securityContext) {

		if (!StringUtils.isEmpty(this.getTopic()) && (this.getTopic() != null)) {

			Iterable<MessageClient> clientsList = this.getClients();
			clientsList.forEach(client -> {

				try {

					final AbstractMethod method = Methods.resolveMethod(client.getClass(), "subscribeTopic");
					if (method != null) {

						final Arguments params = new Arguments();

						params.add("topic", this.getTopic());

						method.execute(securityContext, client, params, new EvaluationHints());
					}

				} catch (FrameworkException e) {

					final Logger logger = LoggerFactory.getLogger(MessageSubscriber.class);
					logger.error("Could not invoke subscribeTopic on MessageClient: " + e.getMessage());
				}
			});
		}
	}

	@Override
	public  void onCreation(final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

		super.onCreation(securityContext, errorBuffer);

		if (this.getProperty(StructrApp.key(MessageSubscriber.class, "topic")) != null) {
			subscribeOnAllClients(securityContext);
		}
	}

	@Override
	public  void onModification(final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

		super.onModification(securityContext, errorBuffer, modificationQueue);

		if (modificationQueue.isPropertyModified(this, StructrApp.key(MessageSubscriber.class, "topic")) || modificationQueue.isPropertyModified(this, StructrApp.key(MessageSubscriber.class, "topic"))) {
			subscribeOnAllClients(securityContext);
		}

	}

	@Export
	public  RestMethodResult onMessage(final SecurityContext securityContext, final String topic, final String message) throws FrameworkException {

		if (!StringUtils.isEmpty(this.getCallback())) {

			String script = "${" + this.getCallback().trim() + "}";

			Map<String, Object> params = new HashMap<>();
			params.put("topic", topic);
			params.put("message", message);

			ActionContext ac = new ActionContext(securityContext, params);
			ac.setConstant("topic", topic);
			ac.setConstant("message", message);

			// FIXME: the code source in this call should be the schema method that this subscriber was compiled from.
			Scripting.evaluate(ac, this, script, "onMessage", null);
		}

		return new RestMethodResult(200);
	}

}
