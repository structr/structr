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
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.View;
import org.structr.common.error.FrameworkException;
import org.structr.core.Export;
import org.structr.core.api.AbstractMethod;
import org.structr.core.api.Arguments;
import org.structr.core.api.Methods;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.Tx;
import org.structr.core.property.EndNodes;
import org.structr.core.property.Property;
import org.structr.messaging.engine.relation.MessageClientHASMessageSubscriber;
import org.structr.rest.RestMethodResult;
import org.structr.schema.action.EvaluationHints;

public class MessageClient extends AbstractNode {

	public static final Property<Iterable<MessageSubscriber>> subscribersProperty = new EndNodes<>("subscribers", MessageClientHASMessageSubscriber.class);

	public static final View defaultView = new View(MessageClient.class, PropertyView.Public,
		subscribersProperty
	);

	public static final View uiView = new View(MessageClient.class, PropertyView.Ui,
		subscribersProperty
	);

	public Iterable<MessageSubscriber> getSubscribers() {
		return getProperty(subscribersProperty);
	}

	public void setSubscribers(final Iterable<MessageSubscriber> subscribers) throws FrameworkException {
		setProperty(subscribersProperty, subscribers);
	}

	@Export
	public RestMethodResult sendMessage(final SecurityContext securityContext, final String topic, final String message) throws FrameworkException {

		final App app = StructrApp.getInstance();
		try (final Tx tx = app.tx()) {

			final Iterable<MessageSubscriber> subscribers = this.getSubscribers();
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

	@Export
	public RestMethodResult subscribeTopic(final SecurityContext securityContext, String topic) throws FrameworkException {
		return new RestMethodResult(200);
	}

	@Export
	public RestMethodResult unsubscribeTopic(final SecurityContext securityContext, String topic) throws FrameworkException {
		return new RestMethodResult(200);
	}

}
