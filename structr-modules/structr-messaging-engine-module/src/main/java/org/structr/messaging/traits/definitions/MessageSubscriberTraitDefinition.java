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

import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.api.AbstractMethod;
import org.structr.core.api.Arguments;
import org.structr.core.api.JavaMethod;
import org.structr.core.entity.Relation;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.StartNodes;
import org.structr.core.property.StringProperty;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.core.traits.operations.graphobject.OnCreation;
import org.structr.core.traits.operations.graphobject.OnModification;
import org.structr.messaging.engine.entities.MessageSubscriber;
import org.structr.messaging.traits.wrappers.MessageSubscriberTraitWrapper;
import org.structr.schema.action.EvaluationHints;

import java.util.Map;
import java.util.Set;

public class MessageSubscriberTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String CLIENTS_PROPERTY  =  "clients";
	public static final String TOPIC_PROPERTY    =  "topic";
	public static final String CALLBACK_PROPERTY =  "callback";

	public MessageSubscriberTraitDefinition() {
		super(StructrTraits.MESSAGE_SUBSCRIBER);
	}

	@Override
	public Set<AbstractMethod> getDynamicMethods() {

		return newSet(

			new JavaMethod("onMessage", false, false) {

				@Override
				public Object execute(final SecurityContext securityContext, final GraphObject entity, final Arguments arguments, final EvaluationHints hints) throws FrameworkException {

					final String topic   = (String) arguments.get(0);
					final String message = (String) arguments.get(1);

					return entity.as(MessageSubscriber.class).onMessage(securityContext, topic, message);
				}
			}
		);
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			MessageSubscriber.class, (traits, node) -> new MessageSubscriberTraitWrapper(traits, node)
		);
	}

	@Override
	public Map<Class, LifecycleMethod> getLifecycleMethods() {

		return Map.of(

			OnCreation.class,
			new OnCreation() {

				@Override
				public void onCreation(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

					final MessageSubscriber subscriber = graphObject.as(MessageSubscriber.class);
					if (subscriber.getTopic() != null) {

						subscriber.subscribeOnAllClients(securityContext);
					}
				}
			},

			OnModification.class,
			new OnModification() {

				@Override
				public void onModification(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

					final MessageSubscriber subscriber = graphObject.as(MessageSubscriber.class);
					final PropertyKey topicKey         = subscriber.getTraits().key(TOPIC_PROPERTY);

					if (modificationQueue.isPropertyModified(subscriber, topicKey)) {

						subscriber.subscribeOnAllClients(securityContext);
					}
				}
			}
		);
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final Property<Iterable<NodeInterface>> clientsProperty = new StartNodes(CLIENTS_PROPERTY, StructrTraits.MESSAGE_CLIENT_HAS_MESSAGE_SUBSCRIBER);
		final Property<String> topicProperty                    = new StringProperty(TOPIC_PROPERTY).indexed();
		final Property<String> callbackProperty                 = new StringProperty(CALLBACK_PROPERTY);

		return newSet(
			clientsProperty,
			topicProperty,
			callbackProperty
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public,
			newSet(
					TOPIC_PROPERTY, CALLBACK_PROPERTY, CLIENTS_PROPERTY
			),
			PropertyView.Ui,
			newSet(
					TOPIC_PROPERTY, CALLBACK_PROPERTY, CLIENTS_PROPERTY
			)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
