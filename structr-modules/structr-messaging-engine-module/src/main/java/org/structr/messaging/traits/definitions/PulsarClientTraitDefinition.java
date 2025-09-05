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

import org.apache.pulsar.client.api.MessageRoutingMode;
import org.apache.pulsar.client.api.Producer;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
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
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.core.traits.operations.graphobject.OnCreation;
import org.structr.core.traits.operations.graphobject.OnDeletion;
import org.structr.core.traits.operations.graphobject.OnModification;
import org.structr.messaging.engine.entities.MessageClient;
import org.structr.messaging.traits.operations.MessageClientOperations;
import org.structr.messaging.traits.wrappers.PulsarClientTraitWrapper;
import org.structr.rest.RestMethodResult;

import java.util.Map;
import java.util.Set;

public class PulsarClientTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String SERVERS_PROPERTY  = "servers";
	public static final String ENABLED_PROPERTY  = "enabled";

	public PulsarClientTraitDefinition() {
		super(StructrTraits.PULSAR_CLIENT);
	}

	@Override
	public Map<Class, LifecycleMethod> getLifecycleMethods() {

		return Map.of(

			OnCreation.class,
			new OnCreation() {

				@Override
				public void onCreation(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {
					graphObject.as(org.structr.messaging.implementation.pulsar.PulsarClient.class).setup();
				}
			},

			OnModification.class,
			new OnModification() {

				@Override
				public void onModification(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

					final org.structr.messaging.implementation.pulsar.PulsarClient client = graphObject.as(org.structr.messaging.implementation.pulsar.PulsarClient.class);
					final Traits traits = client.getTraits();

					if (modificationQueue.isPropertyModified(client, traits.key(SERVERS_PROPERTY))) {

						PulsarClientTraitWrapper.ConsumerWorker cw = client.getConsumerWorker();
						if (cw != null) {

							cw.invalidateConsumer();
						}
					}
				}
			},

			OnDeletion.class,
			new OnDeletion() {

				@Override
				public void onDeletion(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer, final PropertyMap properties) throws FrameworkException {
					graphObject.as(org.structr.messaging.implementation.pulsar.PulsarClient.class).close();
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

					final org.structr.messaging.implementation.pulsar.PulsarClient client = messageClient.as(org.structr.messaging.implementation.pulsar.PulsarClient.class);

					if (client.getServers() == null || client.getServers().length == 0) {
						return new RestMethodResult(400, "PulsarClient " + client.getUuid() + " has no servers specified");
					}

					try (PulsarClient pulsarClient = PulsarClient.builder()
						.serviceUrl(String.join(",", client.getServers()))
						.build()
					) {

						try (Producer<byte[]> producer = pulsarClient.newProducer()
							.topic(topic)
							.messageRoutingMode(MessageRoutingMode.SinglePartition)
							.create()
						) {

							producer.send(message.getBytes());

							return new RestMethodResult(200);
						}

					} catch (PulsarClientException ex) {

						LoggerFactory.getLogger(PulsarClientTraitDefinition.class).error("Exception in PulsarClient.sendMessage.", ex);
						return new RestMethodResult(500);
					}
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

			org.structr.messaging.implementation.pulsar.PulsarClient.class, (traits, node) -> new PulsarClientTraitWrapper(traits, node)
		);
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final Property<String[]> serversProperty = new ArrayProperty(SERVERS_PROPERTY, String.class);
		final Property<Boolean> enabledProperty = new BooleanProperty(ENABLED_PROPERTY).defaultValue(false);

		return newSet(
			serversProperty,
			enabledProperty
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public,
			newSet(
				SERVERS_PROPERTY, ENABLED_PROPERTY
			),
			PropertyView.Ui,
			newSet(
				SERVERS_PROPERTY, ENABLED_PROPERTY
			)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
