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
package org.structr.messaging.traits.definitions;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.entity.Relation;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.property.*;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.TraitsInstance;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.core.traits.operations.graphobject.OnCreation;
import org.structr.core.traits.operations.graphobject.OnDeletion;
import org.structr.core.traits.operations.graphobject.OnModification;
import org.structr.messaging.engine.entities.MessageClient;
import org.structr.messaging.implementation.kafka.entity.KafkaClient;
import org.structr.messaging.traits.operations.MessageClientOperations;
import org.structr.messaging.traits.wrappers.KafkaClientTraitWrapper;
import org.structr.rest.RestMethodResult;

import java.util.Map;
import java.util.Set;

public class KafkaClientTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String SERVERS_PROPERTY  = "servers";
	public static final String GROUP_ID_PROPERTY = "groupId";
	public static final String ENABLED_PROPERTY  = "enabled";

	public KafkaClientTraitDefinition() {
		super(StructrTraits.KAFKA_CLIENT);
	}

	@Override
	public Map<Class, LifecycleMethod> createLifecycleMethods(TraitsInstance traitsInstance) {

		return Map.of(

			OnCreation.class,
			new OnCreation() {

				@Override
				public void onCreation(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

					final KafkaClient client = graphObject.as(KafkaClient.class);

					client.refreshConfiguration();
					client.setup();
				}
			},

			OnModification.class,
			new OnModification() {

				@Override
				public void onModification(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

					final KafkaClient client = graphObject.as(KafkaClient.class);
					final Traits traits = client.getTraits();

					if (modificationQueue.isPropertyModified(client, traits.key(SERVERS_PROPERTY)) || modificationQueue.isPropertyModified(client, traits.key(GROUP_ID_PROPERTY))) {
						client.refreshConfiguration();
					}
				}
			},

			OnDeletion.class,
			new OnDeletion() {

				@Override
				public void onDeletion(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer, final PropertyMap properties) throws FrameworkException {
					graphObject.as(KafkaClient.class).close();
				}
			}
		);
	}

	@Override
	public Map<Class, FrameworkMethod> getFrameworkMethods() {

		return Map.of(

			MessageClientOperations.class,
			new MessageClientOperations() {


				@Override
				public RestMethodResult sendMessage(final SecurityContext securityContext, final MessageClient messageClient, final String topic, final String message) throws FrameworkException {

					final KafkaClient client = messageClient.as(KafkaClient.class);

					getSuper().sendMessage(securityContext, client, topic, message);

					if (client.getProducer() == null && client.getServers() != null && client.getServers().length > 0) {

						client.setProducer(new KafkaProducer<>(client.getConfiguration(KafkaProducer.class)));

					} else if (client.getServers() == null || client.getServers().length == 0) {

						final Logger logger = LoggerFactory.getLogger(KafkaClientTraitDefinition.class);
						logger.error("Could not initialize producer. No servers configured.");
						return new RestMethodResult(422);
					}

					if (client.getProducer() != null) {

						client.getProducer().send(new ProducerRecord<>(topic, message));
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
			KafkaClient.class, (traits, node) -> new KafkaClientTraitWrapper(traits, node)
		);
	}

	@Override
	public Set<PropertyKey> createPropertyKeys(TraitsInstance traitsInstance) {

		final Property<String[]> serversProperty = new ArrayProperty(SERVERS_PROPERTY, String.class);
		final Property<String> groupIdProperty   = new StringProperty(GROUP_ID_PROPERTY);
		final Property<Boolean> enabledProperty  = new BooleanProperty(ENABLED_PROPERTY).defaultValue(false);

		return newSet(
			serversProperty,
			groupIdProperty,
			enabledProperty
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public,
			newSet(
					SERVERS_PROPERTY, GROUP_ID_PROPERTY, ENABLED_PROPERTY, MessageClientTraitDefinition.SUBSCRIBERS_PROPERTY
			),
			PropertyView.Ui,
			newSet(
					SERVERS_PROPERTY, GROUP_ID_PROPERTY, ENABLED_PROPERTY, MessageClientTraitDefinition.SUBSCRIBERS_PROPERTY
			)
		);
	}
}
