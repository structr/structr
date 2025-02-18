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
package org.structr.xmpp.traits.definitions;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence.Mode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.AccessControllable;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.api.AbstractMethod;
import org.structr.core.api.Arguments;
import org.structr.core.api.JavaMethod;
import org.structr.core.api.Methods;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Relation;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.*;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.core.traits.operations.graphobject.OnCreation;
import org.structr.core.traits.operations.graphobject.OnDeletion;
import org.structr.core.traits.operations.graphobject.OnModification;
import org.structr.rest.RestMethodResult;
import org.structr.schema.action.EvaluationHints;
import org.structr.xmpp.XMPPClient;
import org.structr.xmpp.XMPPClientConnection;
import org.structr.xmpp.XMPPContext;
import org.structr.xmpp.traits.wrappers.XMPPClientTraitWrapper;

import java.util.Map;
import java.util.Set;

/**
 *
 *
 */
public class XMPPClientTraitDefinition extends AbstractNodeTraitDefinition {

	public XMPPClientTraitDefinition() {
		super("XMPPClient");
	}

	@Override
	public Map<Class, LifecycleMethod> getLifecycleMethods() {

		return Map.of(

			OnCreation.class,
			new OnCreation() {

				@Override
				public void onCreation(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

					final XMPPClient client = graphObject.as(XMPPClient.class);

					if (client.getIsEnabled()) {
						XMPPContext.connect(client);
					}
				}
			},

			OnModification.class,
			new OnModification() {
				@Override
				public void onModification(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

					final XMPPClient client         = graphObject.as(XMPPClient.class);
					XMPPClientConnection connection = XMPPContext.getClientForId(client.getUuid());
					boolean enabled                 = client.getIsEnabled();

					if (!enabled) {

						if (connection != null && connection.isConnected()) {
							connection.disconnect();
						}

					} else {

						if (connection == null || !connection.isConnected()) {
							XMPPContext.connect(client);
						}

						connection = XMPPContext.getClientForId(client.getUuid());
						if (connection != null) {

							if (connection.isConnected()) {

								client.setIsConnected(true);
								connection.setPresence(Mode.valueOf(client.getPresenceMode()));

							} else {

								client.setIsConnected(false);
							}
						}
					}
				}
			},

			OnDeletion.class,
			new OnDeletion() {

				@Override
				public void onDeletion(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer, final PropertyMap properties) throws FrameworkException {

					final XMPPClient client = graphObject.as(XMPPClient.class);
					final String uuid       = properties.get(client.getTraits().key("uuid"));

					if (uuid != null) {

						final XMPPClientConnection connection = XMPPContext.getClientForId(uuid);
						if (connection != null) {

							connection.disconnect();
						}
					}
				}
			}
		);
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			XMPPClient.class, (traits, node) -> new XMPPClientTraitWrapper(traits, node)
		);
	}

	@Override
	public Set<AbstractMethod> getDynamicMethods() {

		return newSet(

			new JavaMethod("doSendMessage", false, false) {

				@Override
				public Object execute(final SecurityContext securityContext, final GraphObject entity, final Arguments arguments, final EvaluationHints hints) throws FrameworkException {

					final XMPPClient client = entity.as(XMPPClient.class);
					if (client.getIsEnabled()) {

						final XMPPClientConnection connection = XMPPContext.getClientForId(client.getUuid());
						if (connection.isConnected()) {

							final Map<String, Object> data = arguments.toMap();
							final String recipient         = (String)data.get("recipient");
							final String message           = (String)data.get("message");

							connection.sendMessage(recipient, message);

						} else {

							throw new FrameworkException(422, "Not connected.");
						}
					}

					return new RestMethodResult(200);
				}
			},

			new JavaMethod("doSubscribe", false, false) {

				@Override
				public Object execute(final SecurityContext securityContext, final GraphObject entity, final Arguments arguments, final EvaluationHints hints) throws FrameworkException {

					final XMPPClient client = entity.as(XMPPClient.class);
					if (client.getIsEnabled()) {

						final XMPPClientConnection connection = XMPPContext.getClientForId(client.getUuid());
						if (connection.isConnected()) {

							final Map<String, Object> data = arguments.toMap();
							final String recipient         = (String)data.get("recipient");
							final String message           = (String)data.get("message");

							connection.subscribe(recipient);

						} else {

							throw new FrameworkException(422, "Not connected.");
						}
					}

					return new RestMethodResult(200);
				}
			},

			new JavaMethod("doUnsubscribe", false, false) {

				@Override
				public Object execute(final SecurityContext securityContext, final GraphObject entity, final Arguments arguments, final EvaluationHints hints) throws FrameworkException {

					final XMPPClient client = entity.as(XMPPClient.class);
					if (client.getIsEnabled()) {

						final XMPPClientConnection connection = XMPPContext.getClientForId(client.getUuid());
						if (connection.isConnected()) {

							final Map<String, Object> data = arguments.toMap();
							final String recipient         = (String)data.get("recipient");

							connection.unsubscribe(recipient);

						} else {

							throw new FrameworkException(422, "Not connected.");
						}
					}

					return new RestMethodResult(200);
				}
			},

			new JavaMethod("doConfirmSubscription", false, false) {

				@Override
				public Object execute(final SecurityContext securityContext, final GraphObject entity, final Arguments arguments, final EvaluationHints hints) throws FrameworkException {

					final XMPPClient client = entity.as(XMPPClient.class);

					if (client.getIsEnabled()) {

						final XMPPClientConnection connection = XMPPContext.getClientForId(client.getUuid());
						if (connection.isConnected()) {

							final Map<String, Object> data = arguments.toMap();
							final String recipient         = (String)data.get("recipient");

							connection.confirmSubscription(recipient);

						} else {

							throw new FrameworkException(422, "Not connected.");
						}
					}

					return new RestMethodResult(200);
				}
			},

			new JavaMethod("doDenySubscription", false, false) {

				@Override
				public Object execute(final SecurityContext securityContext, final GraphObject entity, final Arguments arguments, final EvaluationHints hints) throws FrameworkException {

					final XMPPClient client = entity.as(XMPPClient.class);
					if (client.getIsEnabled()) {

						final XMPPClientConnection connection = XMPPContext.getClientForId(client.getUuid());
						if (connection.isConnected()) {

							final Map<String, Object> data = arguments.toMap();
							final String recipient         = (String)data.get("recipient");

							connection.denySubscription(recipient);

						} else {

							throw new FrameworkException(422, "Not connected.");
						}
					}

					return new RestMethodResult(200);
				}
			},

			new JavaMethod("doJoinChat", false, false) {

				@Override
				public Object execute(final SecurityContext securityContext, final GraphObject entity, final Arguments arguments, final EvaluationHints hints) throws FrameworkException {

					final XMPPClient client = entity.as(XMPPClient.class);
					if (client.getIsEnabled()) {

						final XMPPClientConnection connection = XMPPContext.getClientForId(client.getUuid());
						if (connection.isConnected()) {

							final Map<String, Object> data = arguments.toMap();
							final String chatRoom          = (String)data.get("chatRoom");
							final String nickname          = (String)data.get("nickname");
							final String password          = (String)data.get("password");

							connection.joinChat(chatRoom, nickname, password);

						} else {

							throw new FrameworkException(422, "Not connected.");
						}
					}

					return new RestMethodResult(200);
				}
			},

			new JavaMethod("doSendChatMessage", false, false) {

				@Override
				public Object execute(final SecurityContext securityContext, final GraphObject entity, final Arguments arguments, final EvaluationHints hints) throws FrameworkException {

					final XMPPClient client = entity.as(XMPPClient.class);
					if (client.getIsEnabled()) {

						final XMPPClientConnection connection = XMPPContext.getClientForId(client.getUuid());
						if (connection.isConnected()) {

							final Map<String, Object> data = arguments.toMap();
							final String chatRoom          = (String)data.get("chatRoom");
							final String message           = (String)data.get("message");
							final String password          = (String)data.get("password");

							connection.sendChatMessage(chatRoom, message, password);

						} else {

							throw new FrameworkException(422, "Not connected.");
						}
					}

					return new RestMethodResult(200);
				}
			}
		);
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final Property<Iterable<NodeInterface>> pendingRequestsProperty = new EndNodes("pendingRequests", "XMPPClientRequest");
		final Property<String> xmppHandleProperty                       = new FunctionProperty("xmppHandle").readFunction("concat(this.xmppUsername, '@', this.xmppHost)").typeHint("String").indexed();
		final Property<String> xmppUsernameProperty                     = new StringProperty("xmppUsername").indexed();
		final Property<String> xmppPasswordProperty                     = new StringProperty("xmppPassword");
		final Property<String> xmppServiceProperty                      = new StringProperty("xmppService");
		final Property<String> xmppHostProperty                         = new StringProperty("xmppHost");
		final Property<Integer> xmppPortProperty                        = new IntProperty("xmppPort");
		final Property<String> presenceModeProperty                     = new EnumProperty("presenceMode", Mode.class);
		final Property<Boolean> isEnabledProperty                       = new BooleanProperty("isEnabled");
		final Property<Boolean> isConnectedProperty                     = new BooleanProperty("isConnected");

		return newSet(
			pendingRequestsProperty,
			xmppHandleProperty,
			xmppUsernameProperty,
			xmppPasswordProperty,
			xmppServiceProperty,
			xmppHostProperty,
			xmppPortProperty,
			presenceModeProperty,
			isEnabledProperty,
			isConnectedProperty
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public,
			newSet(
				"xmppHandle", "xmppUsername", "xmppPassword", "xmppService", "xmppHost", "xmppPort",
				"presenceMode", "isEnabled", "isConnected", "pendingRequests"
			),
			PropertyView.Ui,
			newSet(
				"xmppHandle", "xmppUsername", "xmppPassword", "xmppService", "xmppHost", "xmppPort",
				"presenceMode", "isEnabled", "isConnected", "pendingRequests"
			)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}

	// ----- static methods -----
	public static void onMessage(final String uuid, final Message message) {

		final App app = StructrApp.getInstance();
		try (final Tx tx = app.tx()) {

			final NodeInterface client = StructrApp.getInstance().getNodeById("XMPPClient", uuid);
			if (client != null) {

				final String callbackName   = "onXMPP" + message.getClass().getSimpleName();
				final AbstractMethod method = Methods.resolveMethod(client.getTraits(), callbackName);

				if (method != null) {

					final Arguments arguments = new Arguments();

					arguments.add("sender",  message.getFrom());
					arguments.add("message", message.getBody());

					method.execute(SecurityContext.getSuperUserInstance(), client, arguments, new EvaluationHints());
				}
			}

			tx.success();

		} catch (FrameworkException fex) {

			final Logger logger = LoggerFactory.getLogger(XMPPClientTraitDefinition.class);
			logger.warn("", fex);
		}
	}

	public static void onRequest(final String uuid, final IQ request) {

		final App app = StructrApp.getInstance();
		try (final Tx tx = app.tx()) {

			final NodeInterface client = StructrApp.getInstance().getNodeById("XMPPClient", uuid);
			if (client != null) {

				final Traits traits = Traits.of("XMPPRequest");

				app.create("XMPPRequest",
					new NodeAttribute(traits.key("client"),      client),
					new NodeAttribute(traits.key("sender"),      request.getFrom()),
					new NodeAttribute(traits.key("owner"),       client.as(AccessControllable.class).getOwnerNode()),
					new NodeAttribute(traits.key("content"),     request.toXML("").toString()),
					new NodeAttribute(traits.key("requestType"), request.getType())
				);
			}

			tx.success();

		} catch (FrameworkException fex) {

			final Logger logger = LoggerFactory.getLogger(XMPPClientTraitDefinition.class);
			logger.warn("", fex);
		}
	}
}
