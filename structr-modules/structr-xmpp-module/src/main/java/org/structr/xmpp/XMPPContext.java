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
package org.structr.xmpp;

import org.jivesoftware.smack.*;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.java7.Java7SmackInitializer;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.packet.Presence.Mode;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.packet.RosterPacket;
import org.jivesoftware.smack.sm.predicates.ForEveryStanza;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.MultiUserChatManager;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Resourcepart;
import org.jxmpp.stringprep.XmppStringprepException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.xmpp.handler.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 *
 */
public class XMPPContext {

	private static final Logger logger                                 = LoggerFactory.getLogger(XMPPContext.class.getName());
	private static final Map<String, XMPPClientConnection> connections = new ConcurrentHashMap<>();
	private static final Map<String, TypeHandler> typeHandlers         = new HashMap<>();

	static {

		typeHandlers.put(Bind.class.getName(),          new BindTypeHandler());
		typeHandlers.put(RosterPacket.class.getName(),  new RosterPacketTypeHandler());
		typeHandlers.put(Presence.class.getName(),      new PresenceTypeHandler());
		typeHandlers.put(Message.class.getName(),       new MessageTypeHandler());
		typeHandlers.put(EmptyResultIQ.class.getName(), new EmptyResultIQTypeHandler());
	}

	/**
	 * Registers the boot-time initialization callback that connects the enabled clients. Called from
	 * {@code XMPPModule.onLoad()}: a static initializer would only run once something loads this class, which may happen
	 * after the initialization callbacks have run.
	 */
	public static void registerInitializationCallback() {

		Services.getInstance().registerInitializationCallback(() -> {

			if (Traits.exists(StructrTraits.XMPP_CLIENT)) {

				final App app = StructrApp.getInstance();

				try (final Tx tx = app.tx()) {

					for (final NodeInterface clientNode : app.nodeQuery(StructrTraits.XMPP_CLIENT).getAsList()) {

						final XMPPClient client = clientNode.as(XMPPClient.class);

						client.setIsConnected(false);

						// enable clients on startup
						if (client.getIsEnabled()) {

							XMPPContext.connect(client);

							client.setIsConnected(XMPPContext.isConnected(client.getUuid()));
						}
					}

					tx.success();

				} catch (Throwable t) {

					logger.error("Unable to access XMPP clients: {}", t.getMessage());
				}
			}
		});
	}

	/**
	 * Connects the given client, unless it is connected already: the callers run in transactions that can be repeated,
	 * and a repetition must not open a second connection for the same client.
	 */
	public static void connect(final XMPPInfo callback) throws FrameworkException {

		final XMPPClientConnection existing = connections.get(callback.getUuid());
		if (existing != null && existing.isConnected()) {

			return;
		}

		// a connection that was lost and is still trying to reconnect is replaced, it must not log in as a second session
		if (existing != null) {

			existing.disconnect();
		}

		new Java7SmackInitializer().initialize();

		try {

			final AbstractXMPPConnection connection = new XMPPTCPConnection(getConnectionConfiguration(callback));

			// subscription requests are answered by the onXMPPPresence method of the client, see XMPPClient.onPresence(),
			// not by Smack, which rejects them all by default
			Roster.getInstanceFor(connection).setSubscriptionMode(Roster.SubscriptionMode.manual);

			// a connection that is lost, e.g. because the server restarts, is restored every five seconds
			final ReconnectionManager reconnection = ReconnectionManager.getInstanceFor(connection);

			reconnection.setReconnectionPolicy(ReconnectionManager.ReconnectionPolicy.FIXED_DELAY);
			reconnection.setFixedDelay(5);
			reconnection.enableAutomaticReconnection();

			final StructrXMPPConnection structrConnection = new StructrXMPPConnection(callback, connection);

			connections.put(callback.getUuid(), structrConnection);

			connection.connect();

			try {

				// logged in here and not in the connection listener, because Smack logs in by itself when it restores the
				// connection, and a second login would fail
				connection.login();

			} catch (InterruptedException | IOException | SmackException | XMPPException ex) {

				// a client that cannot log in, e.g. because of a wrong password, is not connected, and Smack must not
				// keep trying to log in with the same credentials
				structrConnection.disconnect();

				throw ex;
			}

		} catch (InterruptedException | IOException | SmackException | XMPPException ex) {

			logger.warn("", ex);
		}
	}

	/**
	 * Builds the connection configuration for the given client. TLS is required, not merely attempted:
	 * with SecurityMode.ifpossible, an attacker in the network path strips the STARTTLS offer and the
	 * connection continues in plaintext, handing over the XMPP password and every message that follows.
	 *
	 * @param info the client to connect
	 * @return a configuration that refuses to connect without TLS
	 * @throws XmppStringprepException if the client carries a service name that is not a valid XMPP domain
	 */
	public static XMPPTCPConnectionConfiguration getConnectionConfiguration(final XMPPInfo info) throws XmppStringprepException {

		return XMPPTCPConnectionConfiguration.builder()
			.setUsernameAndPassword(info.getUsername(), info.getPassword())
			.setXmppDomain(info.getService())
			.setSecurityMode(ConnectionConfiguration.SecurityMode.required)
			.setHost(info.getHostName())
			.setPort(info.getPort())
			.build();
	}

	/**
	 * Returns the presence mode with the given name, or available for null.
	 */
	public static Mode getPresenceMode(final String name) {

		return name != null ? Mode.valueOf(name) : Mode.available;
	}

	/**
	 * @return whether the client with the given id has a connection that is connected
	 */
	public static boolean isConnected(final String id) {

		final XMPPClientConnection connection = connections.get(id);

		return connection != null && connection.isConnected();
	}

	public static XMPPClientConnection getClientForId(final String id) {

		return connections.get(id);
	}

	public static class StructrXMPPConnection implements ConnectionListener, XMPPClientConnection, StanzaListener {

		private AbstractXMPPConnection connection    = null;
		private Exception exception                  = null;
		private volatile boolean isAuthenticated     = false;
		private volatile boolean isConnected         = false;
		private String name                          = null;
		private String uuid                          = null;
		private String resource                      = null;
		private String jid                           = null;
		private volatile Mode presenceMode           = null;
		private volatile boolean authenticatedBefore = false;

		// the rooms the client has joined, with nickname and password, to join them again when the connection is restored
		private final Map<String, String[]> joinedRooms = new ConcurrentHashMap<>();

		public StructrXMPPConnection(final XMPPInfo info, final AbstractXMPPConnection connection) {

			this.connection   = connection;
			this.uuid         = info.getUuid();
			this.name         = info.getUsername() + "@" + info.getHostName();
			this.presenceMode = getPresenceMode(info.getPresenceMode());

			connection.addConnectionListener(this);
			connection.addAsyncStanzaListener(this, ForEveryStanza.INSTANCE);
		}

		@Override
		public void sendMessage(final String recipient, final String message) throws FrameworkException {

			if (isConnected) {

				try {

					final Message messageObject = new Message(JidCreate.entityBareFrom(recipient));
					messageObject.setBody(message);

					connection.sendStanza(messageObject);

				} catch (NotConnectedException | InterruptedException | XmppStringprepException nex) {

					throw new FrameworkException(422, "Not connected");
				}

			} else {

				throw new FrameworkException(422, "Not connected");
			}
		}

		@Override
		public void sendChatMessage(final String chatRoom, final String message, final String password) throws FrameworkException {

			if (isConnected) {

				try {

					final MultiUserChatManager manager = MultiUserChatManager.getInstanceFor(connection);
					if (manager != null) {

						final MultiUserChat chat = manager.getMultiUserChat(JidCreate.entityBareFrom(chatRoom));
						if (chat != null) {

							// join chat first
							if (!chat.isJoined()) {

								chat.join(Resourcepart.fromOrNull(name), password);

								joinedRooms.put(chatRoom, new String[] { name, password });
							}

							chat.sendMessage(message);
						}

					}

				} catch (XMPPErrorException | SmackException | XmppStringprepException | InterruptedException ex) {

					throw new FrameworkException(422, "Connection error: " + ex.getMessage());
				}

			} else {

				throw new FrameworkException(422, "Not connected");
			}
		}

		@Override
		public void joinChat(final String chatRoom, final String nickname, final String password) throws FrameworkException {

			if (isConnected) {

				try {

					final MultiUserChatManager manager = MultiUserChatManager.getInstanceFor(connection);
					if (manager != null) {

						final MultiUserChat chat = manager.getMultiUserChat(JidCreate.entityBareFrom(chatRoom));

						// joining a room the client is in already does nothing, so that a script can join its rooms regularly
						if (chat != null && !chat.isJoined()) {

							chat.join(Resourcepart.fromOrNull(nickname), Objects.requireNonNullElse(password, ""));

							joinedRooms.put(chatRoom, new String[] { nickname, password });
						}

					}

				} catch (XMPPErrorException | SmackException | InterruptedException | XmppStringprepException ex) {

					throw new FrameworkException(422, "Connection error: " + ex.getMessage());
				}

			} else {

				throw new FrameworkException(422, "Not connected");
			}
		}

		@Override
		public void setPresence(final Mode mode) throws FrameworkException {

			// remembered for the next login of this connection, see authenticated()
			this.presenceMode = mode;

			if (isConnected) {

				try {

					final Presence presence = new Presence(Presence.Type.available);
					presence.setMode(mode);

					connection.sendStanza(presence);

				} catch (NotConnectedException | InterruptedException nex) {

					throw new FrameworkException(422, "Not connected");
				}

			} else {

				throw new FrameworkException(422, "Not connected");
			}
		}

		@Override
		public void subscribe(final String to) throws FrameworkException {

			if (isConnected) {

				try {

					final Presence presence = new Presence(Presence.Type.subscribe);
					presence.setTo(JidCreate.bareFrom(to));

					connection.sendStanza(presence);

				} catch (NotConnectedException | InterruptedException | XmppStringprepException nex) {

					throw new FrameworkException(422, "Not connected");
				}

			} else {

				throw new FrameworkException(422, "Not connected");
			}
		}

		@Override
		public void confirmSubscription(final String subscriber) throws FrameworkException {

			if (isConnected) {

				try {

					final Presence presence = new Presence(Presence.Type.subscribed);
					presence.setTo(JidCreate.bareFrom(subscriber));

					connection.sendStanza(presence);

				} catch (NotConnectedException | InterruptedException | XmppStringprepException nex) {

					throw new FrameworkException(422, "Not connected");
				}

			} else {

				throw new FrameworkException(422, "Not connected");
			}
		}

		@Override
		public void denySubscription(final String subscriber) throws FrameworkException {

			if (isConnected) {

				try {

					final Presence presence = new Presence(Presence.Type.unsubscribed);
					presence.setTo(JidCreate.bareFrom(subscriber));

					connection.sendStanza(presence);

				} catch (NotConnectedException | InterruptedException | XmppStringprepException nex) {

					throw new FrameworkException(422, "Not connected");
				}

			} else {

				throw new FrameworkException(422, "Not connected");
			}
		}

		@Override
		public void unsubscribe(final String to) throws FrameworkException {

			if (isConnected) {

				try {

					final Presence presence = new Presence(Presence.Type.unsubscribe);
					presence.setTo(JidCreate.bareFrom(to));

					connection.sendStanza(presence);

				} catch (NotConnectedException | InterruptedException | XmppStringprepException nex) {

					throw new FrameworkException(422, "Not connected");
				}

			} else {

				throw new FrameworkException(422, "Not connected");
			}
		}

		@Override
		public Exception getException() {

			return exception;
		}

		@Override
		public boolean isConnected() {

			return isConnected;
		}

		@Override
		public boolean isAuthenticated() {

			return isAuthenticated;
		}

		@Override
		public void disconnect() {

			// also stops a reconnection that is under way, a disabled or deleted client must not log in again
			ReconnectionManager.getInstanceFor(connection).disableAutomaticReconnection();

			if (isConnected) {

				connection.disconnect();
			}
		}

		public String getUuid() {

			return uuid;
		}

		public void setJID(final String jid) {

			this.jid = jid;
		}

		public void setResource(final String resource) {

			this.resource = resource;
		}

		// ----- interface ConnectionListener -----
		@Override
		public void connected(final XMPPConnection xmppc) {

			isConnected = true;
		}

		@Override
		public void authenticated(final XMPPConnection xmppc, final boolean resumed) {

			isAuthenticated = true;

			// the server delivers messages to an account only while it is available, also those it kept while the
			// account was offline, so the client announces its presence as soon as it has logged in
			try {

				setPresence(presenceMode);

			} catch (FrameworkException fex) {

				logger.warn("Unable to announce the presence of {}: {}", name, fex.getMessage());
			}

			recordConnected(true);

			// a login after the first one restores a lost connection, a resumed stream is still in its rooms
			if (authenticatedBefore && !resumed) {

				rejoinRooms();
			}

			authenticatedBefore = true;
		}

		/**
		 * Joins the rooms of the client again after the connection was restored. Smack still considers the client an
		 * occupant and would not join, so each room is left first. The server does not know the lost session anymore and
		 * does not answer, but Smack forgets the room either way, at the latest after the reply timeout. Runs in a thread
		 * of its own to keep the reconnection free.
		 */
		private void rejoinRooms() {

			if (joinedRooms.isEmpty()) {

				return;
			}

			new Thread(() -> {

				final MultiUserChatManager manager = MultiUserChatManager.getInstanceFor(connection);

				for (final Map.Entry<String, String[]> room : joinedRooms.entrySet()) {

					try {

						final MultiUserChat chat = manager.getMultiUserChat(JidCreate.entityBareFrom(room.getKey()));

						if (chat.isJoined()) {

							try {

								chat.leave();

							} catch (Exception expected) {

								// no answer for the lost session, see above
							}
						}

						chat.join(Resourcepart.fromOrNull(room.getValue()[0]), Objects.requireNonNullElse(room.getValue()[1], ""));

					} catch (Exception ex) {

						logger.warn("Unable to join room {} again after the connection was restored: {}", room.getKey(), ex.getMessage());
					}
				}

			}, "XMPP rejoin " + name).start();
		}

		@Override
		public void connectionClosed() {

			// e.g. the server ended the session on purpose, which Smack does not restore
			isConnected = false;

			recordConnected(false);
		}

		@Override
		public void connectionClosedOnError(final Exception excptn) {

			isConnected = false;
			this.exception = excptn;

			recordConnected(false);
		}

		/**
		 * Records the state of the connection on the client when the connection changes by itself, i.e. when it is lost,
		 * closed by the server or restored. A change that a transaction causes, like enabling the client, is recorded by
		 * that transaction.
		 */
		private void recordConnected(final boolean connected) {

			if (TransactionCommand.inTransaction()) {

				return;
			}

			final App app = StructrApp.getInstance();

			try (final Tx tx = app.tx()) {

				final NodeInterface client = app.getNodeById(StructrTraits.XMPP_CLIENT, uuid);

				// a client that was disabled while its connection was restored stays disconnected
				if (client != null && (!connected || client.as(XMPPClient.class).getIsEnabled())) {

					client.as(XMPPClient.class).setIsConnected(connected);
				}

				tx.success();

			} catch (FrameworkException fex) {

				logger.warn("Unable to record the connection state of {}: {}", name, fex.getMessage());
			}
		}
		@Override
		public void processStanza(Stanza stanza) throws NotConnectedException, InterruptedException, SmackException.NotLoggedInException {

			final TypeHandler handler = typeHandlers.get(stanza.getClass().getName());
			if (handler != null) {

				handler.handle(this, stanza);

			} else {

				logger.warn("No type handler for type {}", stanza.getClass().getName());
			}
		}
	}
}
