/*
 * Copyright (C) 2010-2025 Structr GmbH
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
package org.structr.messaging.traits.definitions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.api.*;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.EndNodes;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.TraitsInstance;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.messaging.engine.entities.MessageClient;
import org.structr.messaging.engine.entities.MessageSubscriber;
import org.structr.messaging.traits.operations.MessageClientOperations;
import org.structr.messaging.traits.wrappers.MessageClientTraitWrapper;
import org.structr.rest.RestMethodResult;
import org.structr.schema.action.EvaluationHints;

import java.util.Map;
import java.util.Set;

public class MessageClientTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String SUBSCRIBERS_PROPERTY = "subscribers";

	public MessageClientTraitDefinition() {
		super(StructrTraits.MESSAGE_CLIENT);
	}

	@Override
	public Set<AbstractMethod> getDynamicMethods() {

		return newSet(

			new JavaMethod("sendMessage", false, false) {

				@Override
				public Object execute(final SecurityContext securityContext, final GraphObject entity, final Arguments arguments, final EvaluationHints hints) throws FrameworkException {

					final String topic   = (String) arguments.get(0);
					final String message = (String) arguments.get(1);

					return entity.as(MessageClient.class).sendMessage(securityContext, topic, message);
				}
			},

			new JavaMethod("subscribeTopic", false, false) {

				@Override
				public Object execute(final SecurityContext securityContext, final GraphObject entity, final Arguments arguments, final EvaluationHints hints) throws FrameworkException {

					final String topic = (String) arguments.get(0);

					return entity.as(MessageClient.class).subscribeTopic(securityContext, topic);
				}
			},

			new JavaMethod("unsubscribeTopic", false, false) {

				@Override
				public Object execute(final SecurityContext securityContext, final GraphObject entity, final Arguments arguments, final EvaluationHints hints) throws FrameworkException {

					final String topic   = (String) arguments.get(0);

					return entity.as(MessageClient.class).unsubscribeTopic(securityContext, topic);
				}
			}
		);
	}

	@Override
	public Map<Class, FrameworkMethod> getFrameworkMethods() {

		return Map.of(

			MessageClientOperations.class,
			new MessageClientOperations() {

				public RestMethodResult sendMessage(final SecurityContext securityContext, final MessageClient client, final String topic, final String message) throws FrameworkException {

					final App app = StructrApp.getInstance();
					try (final Tx tx = app.tx()) {

						final Iterable<MessageSubscriber> subscribers = client.getSubscribers();
						if (subscribers != null) {

							subscribers.forEach(sub -> {

								String subTopic = sub.getTopic();

								if (subTopic != null && (subTopic.equals(topic) || subTopic.equals("*"))) {

									try {

										final AbstractMethod method = Methods.resolveMethod(sub.getTraits(), "onMessage");
										if (method != null) {

											final NamedArguments params = new NamedArguments();

											params.add("topic", topic);
											params.add("message", message);

											method.execute(securityContext, sub, params, new EvaluationHints());

										} else {

											throw new RuntimeException("No such method onMessage!");
										}

									} catch (FrameworkException e) {

										final Logger logger = LoggerFactory.getLogger(MessageClientTraitWrapper.class);
										logger.warn("Could not invoke 'onMessage' method on MessageSubscriber: " + e.getMessage());
									}
								}
							});
						}

						tx.success();
					}

					return new RestMethodResult(200);
				}

				@Override
				public RestMethodResult subscribeTopic(final SecurityContext securityContext, final MessageClient client, final String topic) throws FrameworkException {
					return new RestMethodResult(200);
				}

				@Override
				public RestMethodResult unsubscribeTopic(final SecurityContext securityContext, final MessageClient client, final String topic) throws FrameworkException {
					return new RestMethodResult(200);
				}
			}
		);
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(

			MessageClient.class, (traits, node) -> new MessageClientTraitWrapper(traits, node)
		);
	}

	@Override
	public Set<PropertyKey> createPropertyKeys(TraitsInstance traitsInstance) {

		final Property<Iterable<NodeInterface>> subscribersProperty = new EndNodes(traitsInstance, SUBSCRIBERS_PROPERTY, StructrTraits.MESSAGE_CLIENT_HAS_MESSAGE_SUBSCRIBER);

		return newSet(
			subscribersProperty
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public,
			newSet(
					SUBSCRIBERS_PROPERTY
			),
			PropertyView.Ui,
			newSet(
					SUBSCRIBERS_PROPERTY
			)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
