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
package org.structr.xmpp.traits.definitions;

import org.jivesoftware.smack.packet.Presence.Mode;
import org.slf4j.LoggerFactory;
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
import org.structr.core.graph.TransactionCommand;
import org.structr.core.property.*;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.TraitsInstance;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.core.traits.operations.graphobject.OnCreation;
import org.structr.core.traits.operations.graphobject.OnDeletion;
import org.structr.core.traits.operations.graphobject.OnModification;
import org.structr.rest.RestMethodResult;
import org.structr.schema.action.ActionContext;
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

	public static final String XMPP_HANDLE_PROPERTY   = "xmppHandle";
	public static final String XMPP_USERNAME_PROPERTY = "xmppUsername";
	public static final String XMPP_PASSWORD_PROPERTY = "xmppPassword";
	public static final String XMPP_SERVICE_PROPERTY  = "xmppService";
	public static final String XMPP_HOST_PROPERTY     = "xmppHost";
	public static final String XMPP_PORT_PROPERTY     = "xmppPort";
	public static final String PRESENCE_MODE_PROPERTY = "presenceMode";
	public static final String IS_ENABLED_PROPERTY    = "isEnabled";
	public static final String IS_CONNECTED_PROPERTY  = "isConnected";

	public XMPPClientTraitDefinition() {

		super(StructrTraits.XMPP_CLIENT);
	}

	@Override
	public Map<Class, LifecycleMethod> createLifecycleMethods(TraitsInstance traitsInstance) {

		return Map.of(

			OnCreation.class, new OnCreation() {

				@Override
				public void onCreation(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

					final XMPPClient client = graphObject.as(XMPPClient.class);
					if (client.getIsEnabled()) {

						XMPPContext.connect(client);

						client.setIsConnected(XMPPContext.isConnected(client.getUuid()));
					}
				}
			},

			OnModification.class, new OnModification() {
				@Override
				public void onModification(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

					final XMPPClient client         = graphObject.as(XMPPClient.class);
					final Traits traits             = client.getTraits();
					final boolean enabled           = client.getIsEnabled();
					XMPPClientConnection connection = XMPPContext.getClientForId(client.getUuid());

					// Only enabling and disabling change the connection. The connection records its state on the client
					// when it is lost or restored, and that change must not open a second connection next to the
					// reconnection that is under way, or stop the reconnection.
					if (modificationQueue.isPropertyModified(client, traits.key(IS_ENABLED_PROPERTY))) {

						if (!enabled) {

							// also while the connection is lost, to stop the reconnection
							if (connection != null) {

								connection.disconnect();
							}

							client.setIsConnected(false);

						} else {

							if (connection == null || !connection.isConnected()) {

								XMPPContext.connect(client);
							}

							connection = XMPPContext.getClientForId(client.getUuid());

							if (connection != null) {

								if (connection.isConnected()) {

									client.setIsConnected(true);
									connection.setPresence(XMPPContext.getPresenceMode(client.getPresenceMode()));

								} else {

									client.setIsConnected(false);
								}
							}
						}

					} else if (enabled && modificationQueue.isPropertyModified(client, traits.key(PRESENCE_MODE_PROPERTY))) {

						if (connection != null && connection.isConnected()) {

							connection.setPresence(XMPPContext.getPresenceMode(client.getPresenceMode()));
						}
					}
				}
			},

			OnDeletion.class, new OnDeletion() {

				@Override
				public void onDeletion(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer, final PropertyMap properties) throws FrameworkException {

					final XMPPClient client = graphObject.as(XMPPClient.class);
					final String uuid       = properties.get(client.getTraits().key(GraphObjectTraitDefinition.ID_PROPERTY));

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

		return Map.of(XMPPClient.class, (traits, node) -> new XMPPClientTraitWrapper(traits, node));
	}

	@Override
	public Set<AbstractMethod> getDynamicMethods() {

		return newSet(

			new JavaMethod("doSendMessage", false, false) {

				@Override
				public Object execute(final ActionContext actionContext, final GraphObject entity, final Arguments arguments) throws FrameworkException {

					final Map<String, Object> data = arguments.toMap();
					final String recipient         = (String)data.get("recipient");
					final String message           = (String)data.get("message");

					return sendAfterCommit(entity, connection -> connection.sendMessage(recipient, message));
				}
			},

			new JavaMethod("doSubscribe", false, false) {

				@Override
				public Object execute(final ActionContext actionContext, final GraphObject entity, final Arguments arguments) throws FrameworkException {

					final String recipient = (String)arguments.toMap().get("recipient");

					return sendAfterCommit(entity, connection -> connection.subscribe(recipient));
				}
			},

			new JavaMethod("doUnsubscribe", false, false) {

				@Override
				public Object execute(final ActionContext actionContext, final GraphObject entity, final Arguments arguments) throws FrameworkException {

					final String recipient = (String)arguments.toMap().get("recipient");

					return sendAfterCommit(entity, connection -> connection.unsubscribe(recipient));
				}
			},

			new JavaMethod("doConfirmSubscription", false, false) {

				@Override
				public Object execute(final ActionContext actionContext, final GraphObject entity, final Arguments arguments) throws FrameworkException {

					final String recipient = (String)arguments.toMap().get("recipient");

					return sendAfterCommit(entity, connection -> connection.confirmSubscription(recipient));
				}
			},

			new JavaMethod("doDenySubscription", false, false) {

				@Override
				public Object execute(final ActionContext actionContext, final GraphObject entity, final Arguments arguments) throws FrameworkException {

					final String recipient = (String)arguments.toMap().get("recipient");

					return sendAfterCommit(entity, connection -> connection.denySubscription(recipient));
				}
			},

			new JavaMethod("doJoinChat", false, false) {

				@Override
				public Object execute(final ActionContext actionContext, final GraphObject entity, final Arguments arguments) throws FrameworkException {

					// not deferred like the stanzas above, because joining waits for the answer of the server and reports
					// its errors to the caller
					final XMPPClient client = entity.as(XMPPClient.class);
					if (client.getIsEnabled()) {

						final XMPPClientConnection connection = XMPPContext.getClientForId(client.getUuid());
						if (connection != null && connection.isConnected()) {

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
				public Object execute(final ActionContext actionContext, final GraphObject entity, final Arguments arguments) throws FrameworkException {

					final Map<String, Object> data = arguments.toMap();
					final String chatRoom          = (String)data.get("chatRoom");
					final String message           = (String)data.get("message");
					final String password          = (String)data.get("password");

					return sendAfterCommit(entity, connection -> connection.sendChatMessage(chatRoom, message, password));
				}
			}
		);
	}

	/**
	 * Sends a stanza once the transaction that sends it has committed, so that a repeated transaction does not send it
	 * twice and a rolled back one not at all. A client that is not connected is reported to the caller right away, a
	 * failure while sending only in the log, since the transaction has committed by then.
	 */
	private static RestMethodResult sendAfterCommit(final GraphObject entity, final StanzaSender sender) throws FrameworkException {

		final XMPPClient client = entity.as(XMPPClient.class);
		if (client.getIsEnabled()) {

			final XMPPClientConnection connection = XMPPContext.getClientForId(client.getUuid());
			if (connection != null && connection.isConnected()) {

				afterCommit(() -> {

					try {

						sender.send(connection);

					} catch (FrameworkException fex) {

						LoggerFactory.getLogger(XMPPClientTraitDefinition.class).error("Unable to send XMPP stanza after the commit: {}", fex.getMessage());
					}
				});

			} else {

				throw new FrameworkException(422, "Not connected.");
			}
		}

		return new RestMethodResult(200);
	}

	/**
	 * Runs the given work once the transaction of the current thread has committed, and not at all if it is rolled back.
	 * Outside of a transaction, the work runs right away, because there is no commit to wait for. Errors of the work are
	 * only logged: the transaction cannot be rolled back anymore, and an exception would keep the procedures queued after
	 * it from running.
	 */
	private static void afterCommit(final Runnable work) {

		final Runnable guarded = () -> {

			try {

				work.run();

			} catch (RuntimeException rex) {

				LoggerFactory.getLogger(XMPPClientTraitDefinition.class).error("Unable to send XMPP stanza after the commit", rex);
			}
		};

		if (TransactionCommand.inTransaction()) {

			TransactionCommand.queuePostCommitProcedure(guarded);

		} else {

			guarded.run();
		}
	}

	@FunctionalInterface
	private interface StanzaSender {

		void send(final XMPPClientConnection connection) throws FrameworkException;
	}

	@Override
	public Set<PropertyKey> createPropertyKeys(TraitsInstance traitsInstance) {

		final Property<String> xmppHandleProperty   = new FunctionProperty(XMPP_HANDLE_PROPERTY).readFunction("concat(this.xmppUsername, '@', this.xmppHost)").typeHint("String").indexed();
		final Property<String> xmppUsernameProperty = new StringProperty(XMPP_USERNAME_PROPERTY).indexed();
		final Property<String> xmppPasswordProperty = new StringProperty(XMPP_PASSWORD_PROPERTY);
		final Property<String> xmppServiceProperty  = new StringProperty(XMPP_SERVICE_PROPERTY);
		final Property<String> xmppHostProperty     = new StringProperty(XMPP_HOST_PROPERTY);
		final Property<Integer> xmppPortProperty    = new IntProperty(XMPP_PORT_PROPERTY);
		final Property<String> presenceModeProperty = new EnumProperty(PRESENCE_MODE_PROPERTY, Mode.class);
		final Property<Boolean> isEnabledProperty   = new BooleanProperty(IS_ENABLED_PROPERTY);
		final Property<Boolean> isConnectedProperty = new BooleanProperty(IS_CONNECTED_PROPERTY);

		return newSet(
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
				XMPP_HANDLE_PROPERTY, XMPP_USERNAME_PROPERTY, XMPP_PASSWORD_PROPERTY, XMPP_SERVICE_PROPERTY, XMPP_HOST_PROPERTY, XMPP_PORT_PROPERTY,
				PRESENCE_MODE_PROPERTY, IS_ENABLED_PROPERTY, IS_CONNECTED_PROPERTY
			),
			PropertyView.Ui,
			newSet(
				XMPP_HANDLE_PROPERTY, XMPP_USERNAME_PROPERTY, XMPP_PASSWORD_PROPERTY, XMPP_SERVICE_PROPERTY, XMPP_HOST_PROPERTY, XMPP_PORT_PROPERTY,
				PRESENCE_MODE_PROPERTY, IS_ENABLED_PROPERTY, IS_CONNECTED_PROPERTY
			)
		);
	}

	@Override
	public Relation getRelation() {

		return null;
	}
}
