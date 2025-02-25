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
package org.structr.messaging.traits.definitions;

import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.helper.ValidationHelper;
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
import org.structr.core.traits.operations.graphobject.IsValid;
import org.structr.core.traits.operations.graphobject.OnCreation;
import org.structr.core.traits.operations.graphobject.OnDeletion;
import org.structr.core.traits.operations.graphobject.OnModification;
import org.structr.messaging.engine.entities.MessageClient;
import org.structr.messaging.implementation.mqtt.MQTTClientConnection;
import org.structr.messaging.implementation.mqtt.MQTTContext;
import org.structr.messaging.implementation.mqtt.entity.MQTTClient;
import org.structr.messaging.traits.operations.MessageClientOperations;
import org.structr.messaging.traits.wrappers.MQTTClientTraitWrapper;
import org.structr.rest.RestMethodResult;

import java.util.Map;
import java.util.Set;

public class MQTTClientTraitDefinition extends AbstractNodeTraitDefinition {

	public MQTTClientTraitDefinition() {
		super("MQTTClient");
	}

	@Override
	public Map<Class, LifecycleMethod> getLifecycleMethods() {

		return Map.of(

			IsValid.class,
			new IsValid() {

				@Override
				public Boolean isValid(final GraphObject obj, final ErrorBuffer errorBuffer) {

					return ValidationHelper.isValidPropertyNotNull(obj, obj.getTraits().key("mainBrokerURL"), errorBuffer);
				}
			},

			OnCreation.class,
			new OnCreation() {

				@Override
				public void onCreation(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

					final MQTTClient client = graphObject.as(MQTTClient.class);
					if (client.getIsEnabled()) {

						final Traits traits = client.getTraits();

						try {

							MQTTContext.connect(client);

						} catch (FrameworkException ex) {

							client.setProperty(traits.key("isEnabled"), false);
							client.setProperty(traits.key("isConnected"), false);
						}
					}
				}
			},

			OnModification.class,
			new OnModification() {

				@Override
				public void onModification(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

					final MQTTClient client = graphObject.as(MQTTClient.class);
					final Traits traits     = client.getTraits();

					if (modificationQueue.isPropertyModified(client, traits.key("mainBrokerURL")) || modificationQueue.isPropertyModified(client, traits.key("fallbackBrokerURLs"))) {

						MQTTContext.disconnect(client);
					}

					if (modificationQueue.isPropertyModified(client, traits.key("isEnabled")) || modificationQueue.isPropertyModified(client, traits.key("mainBrokerURL")) || modificationQueue.isPropertyModified(client, traits.key("fallbackBrokerURLs"))) {

						MQTTClientConnection connection = MQTTContext.getClientForId(client.getUuid());
						boolean enabled = client.getIsEnabled();
						if (!enabled) {

							if (connection != null && connection.isConnected()) {

								MQTTContext.disconnect(client);

								client.setProperty(traits.key("isConnected"), false);
							}

						} else {

							if (connection == null || !connection.isConnected()) {

								MQTTContext.connect(client);
								MQTTContext.subscribeAllTopics(client);
							}

							connection = MQTTContext.getClientForId(client.getUuid());
							if (connection != null) {

								if (connection.isConnected()) {

									client.setProperty(traits.key("isConnected"), true);

								} else {

									client.setProperty(traits.key("isConnected"), false);
								}
							}
						}
					}
				}
			},

			OnDeletion.class,
			new OnDeletion() {

				@Override
				public void onDeletion(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer, final PropertyMap properties) throws FrameworkException {

					final String uuid = properties.get(Traits.of(StructrTraits.GRAPH_OBJECT).key("id"));
					if (uuid != null) {

						MQTTContext.delete(uuid);
						final MQTTClientConnection connection = MQTTContext.getClientForId(uuid);
						if (connection != null) {

							connection.delete();
						}
					}
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
				public RestMethodResult sendMessage(final SecurityContext securityContext, final MessageClient client, final String topic, final String message) throws FrameworkException {

					if (client.getIsEnabled()) {

						final MQTTClientConnection connection = MQTTContext.getClientForId(client.getUuid());
						if (connection != null && connection.isConnected()) {

							connection.sendMessage(topic, message);

						} else {

							throw new FrameworkException(422, "Not connected.");
						}
					}

					return new RestMethodResult(200);
				}

				@Override
				public RestMethodResult subscribeTopic(final SecurityContext securityContext, final MessageClient client, final String topic) throws FrameworkException {

					if (client.getIsEnabled()) {

						final MQTTClientConnection connection = MQTTContext.getClientForId(client.getUuid());
						if (connection != null && connection.isConnected()) {

							connection.subscribeTopic(topic);
						}
					}

					return new RestMethodResult(200);
				}

				@Override
				public RestMethodResult unsubscribeTopic(final SecurityContext securityContext, final MessageClient client, final String topic) throws FrameworkException {

					if (client.getIsEnabled()) {

						final MQTTClientConnection connection = MQTTContext.getClientForId(client.getUuid());
						if (connection != null && connection.isConnected()) {

							connection.unsubscribeTopic(topic);
						}
					}

					return new RestMethodResult(200);
				}
			}
		);
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			MQTTClient.class, (traits, node) -> new MQTTClientTraitWrapper(traits, node)
		);
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final Property<String> mainBrokerURLProperty        = new StringProperty("mainBrokerURL").notNull();
		final Property<String[]> fallbackBrokerURLsProperty = new ArrayProperty<>("fallbackBrokerURLs", String.class);
		final Property<Integer> qosProperty                 = new IntProperty("qos").defaultValue(0);
		final Property<Boolean> isEnabledProperty           = new BooleanProperty("isEnabled");
		final Property<Boolean> isConnectedProperty         = new BooleanProperty("isConnected").readOnly();
		final Property<String> usernameProperty             = new StringProperty("username");
		final Property<String> passwordProperty             = new StringProperty("password");

		return newSet(
			mainBrokerURLProperty,
			fallbackBrokerURLsProperty,
			qosProperty,
			isEnabledProperty,
			isConnectedProperty,
			usernameProperty,
			passwordProperty
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public,
			newSet(
			"mainBrokerURL", "fallbackBrokerURLs", "qos", "isEnabled", "isConnected", "username", "password"
			),
			PropertyView.Ui,
			newSet(
				"mainBrokerURL", "fallbackBrokerURLs", "qos", "isEnabled", "isConnected", "username", "password"
			)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}