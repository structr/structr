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
package org.structr.messaging.traits.wrappers;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.util.Iterables;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.api.AbstractMethod;
import org.structr.core.api.Methods;
import org.structr.core.api.NamedArguments;
import org.structr.core.graph.NodeInterface;
import org.structr.core.script.Scripting;
import org.structr.core.traits.Traits;
import org.structr.core.traits.wrappers.AbstractNodeTraitWrapper;
import org.structr.messaging.engine.entities.MessageClient;
import org.structr.messaging.engine.entities.MessageSubscriber;
import org.structr.messaging.traits.definitions.MessageSubscriberTraitDefinition;
import org.structr.rest.RestMethodResult;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.EvaluationHints;

import java.util.HashMap;
import java.util.Map;

public class MessageSubscriberTraitWrapper extends AbstractNodeTraitWrapper implements MessageSubscriber {

	public MessageSubscriberTraitWrapper(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	public String getTopic() {
		return wrappedObject.getProperty(traits.key(MessageSubscriberTraitDefinition.TOPIC_PROPERTY));
	}

	public String getCallback() {
		return wrappedObject.getProperty(traits.key(MessageSubscriberTraitDefinition.CALLBACK_PROPERTY));
	}

	public Iterable<MessageClient> getClients() {

		final Iterable<NodeInterface> nodes = wrappedObject.getProperty(traits.key(MessageSubscriberTraitDefinition.CLIENTS_PROPERTY));

		return Iterables.map(n -> n.as(MessageClient.class), nodes);
	}

	@Override
	public void subscribeOnAllClients(final SecurityContext securityContext) {

		if (!StringUtils.isEmpty(this.getTopic()) && (this.getTopic() != null)) {

			Iterable<MessageClient> clientsList = this.getClients();
			clientsList.forEach(client -> {

				try {

					final AbstractMethod method = Methods.resolveMethod(client.getTraits(), "subscribeTopic");
					if (method != null) {

						final NamedArguments params = new NamedArguments();

						params.add("topic", this.getTopic());

						method.execute(securityContext, client, params, new EvaluationHints());
					}

				} catch (FrameworkException e) {

					final Logger logger = LoggerFactory.getLogger(MessageSubscriberTraitWrapper.class);
					logger.error("Could not invoke subscribeTopic on MessageClient: " + e.getMessage());
				}
			});
		}
	}

	@Override
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
