/**
 * Copyright (C) 2010-2020 Structr GmbH
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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.java7.Java7SmackInitializer;
import org.jivesoftware.smack.packet.Bind;
import org.jivesoftware.smack.packet.EmptyResultIQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Presence.Mode;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.roster.packet.RosterPacket;
import org.jivesoftware.smack.sm.predicates.ForEveryStanza;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smackx.muc.DiscussionHistory;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.MultiUserChatManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.service.InitializationCallback;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.Tx;
import org.structr.xmpp.handler.BindTypeHandler;
import org.structr.xmpp.handler.EmptyResultIQTypeHandler;
import org.structr.xmpp.handler.MessageTypeHandler;
import org.structr.xmpp.handler.PresenceTypeHandler;
import org.structr.xmpp.handler.RosterPacketTypeHandler;
import org.structr.xmpp.handler.TypeHandler;

/**
 *
 *
 */
public class XMPPContext {

	private static final Logger logger                                 = LoggerFactory.getLogger(XMPPContext.class.getName());
	private static final Map<String, XMPPClientConnection> connections = new HashMap<>();
	private static final Map<String, TypeHandler> typeHandlers         = new HashMap<>();

	static {

		typeHandlers.put(Bind.class.getName(),          new BindTypeHandler());
		typeHandlers.put(RosterPacket.class.getName(),  new RosterPacketTypeHandler());
		typeHandlers.put(Presence.class.getName(),      new PresenceTypeHandler());
		typeHandlers.put(Message.class.getName(),       new MessageTypeHandler());
		typeHandlers.put(EmptyResultIQ.class.getName(), new EmptyResultIQTypeHandler());
	}

	static {

		Services.getInstance().registerInitializationCallback(new InitializationCallback() {

			@Override
			public void initializationDone() {

				final App app = StructrApp.getInstance();

				try (final Tx tx = app.tx()) {

					for (final XMPPClient client : app.nodeQuery(XMPPClient.class).getAsList()) {

						client.setIsConnected(false);

						// enable clients on startup
						if (client.getIsEnabled()) {
							XMPPContext.connect(client);
						}
					}

					tx.success();

				} catch (Throwable t) {
					logger.error("Unable to access XMPP clients: {}", t.getMessage());
				}
			}
		});
	}

	public static void connect(final XMPPInfo callback) throws FrameworkException {

		new Java7SmackInitializer().initialize();

		final XMPPTCPConnectionConfiguration config = XMPPTCPConnectionConfiguration.builder()
			.setUsernameAndPassword(callback.getUsername(), callback.getPassword())
			.setSecurityMode(ConnectionConfiguration.SecurityMode.ifpossible)
//			.setSecurityMode(ConnectionConfiguration.SecurityMode.disabled)
			.setServiceName(callback.getService())
			.setHost(callback.getHostName())
			.setPort(callback.getPort())
			.build();

		try {

			final AbstractXMPPConnection connection = new XMPPTCPConnection(config);
			connections.put(callback.getUuid(), new StructrXMPPConnection(callback, connection));
			connection.connect();

		} catch (IOException | SmackException | XMPPException ex) {

			logger.warn("", ex);
		}
	}

	public static XMPPClientConnection getClientForId(final String id) {
		return connections.get(id);
	}

	public static class StructrXMPPConnection implements ConnectionListener, XMPPClientConnection, StanzaListener {

		private AbstractXMPPConnection connection = null;
		private Exception exception               = null;
		private boolean isAuthenticated           = false;
		private boolean isConnected               = false;
		private String name                       = null;
		private String uuid                       = null;
		private String resource                   = null;
		private String jid                        = null;

		public StructrXMPPConnection(final XMPPInfo info, final AbstractXMPPConnection connection) {

			this.connection = connection;
			this.uuid       = info.getUuid();
			this.name       = info.getUsername() + "@" + info.getHostName();

			connection.addConnectionListener(this);
			connection.addAsyncStanzaListener(this, ForEveryStanza.INSTANCE);
		}

		@Override
		public void sendMessage(final String recipient, final String message) throws FrameworkException {

			if (isConnected) {

				try {

					final Message messageObject = new Message(recipient);
					messageObject.setBody(message);

					connection.sendStanza(messageObject);

				} catch (NotConnectedException nex) {

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

						final MultiUserChat chat = manager.getMultiUserChat(chatRoom);
						if (chat != null) {

							// join chat first
							if (!chat.isJoined()) {

								chat.join(name, password);
							}

							chat.sendMessage(message);
						}

					}

				} catch (XMPPErrorException | SmackException ex) {

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

						final MultiUserChat chat = manager.getMultiUserChat(chatRoom);
						if (chat != null) {

							final DiscussionHistory history = new DiscussionHistory();
							final long timeout              = TimeUnit.SECONDS.toMillis(10);

							history.setMaxChars(0);
							history.setMaxStanzas(0);

							if (password != null) {

								chat.join(nickname, password, history, timeout);

							} else {

								chat.join(nickname, "", history, timeout);
							}
						}

					}

				} catch (XMPPErrorException | SmackException ex) {

					throw new FrameworkException(422, "Connection error: " + ex.getMessage());
				}

			} else {

				throw new FrameworkException(422, "Not connected");
			}
		}

		@Override
		public void setPresence(final Mode mode) throws FrameworkException {

			if (isConnected) {

				try {
					final Presence presence = new Presence(Presence.Type.available);
					presence.setMode(mode);

					connection.sendStanza(presence);

				} catch (NotConnectedException nex) {

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
					presence.setTo(to);

					connection.sendStanza(presence);

				} catch (NotConnectedException nex) {

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
					presence.setTo(subscriber);

					connection.sendStanza(presence);

				} catch (NotConnectedException nex) {

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
					presence.setTo(subscriber);

					connection.sendStanza(presence);

				} catch (NotConnectedException nex) {

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
					presence.setTo(to);

					connection.sendStanza(presence);

				} catch (NotConnectedException nex) {

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

			try {
				connection.login();

			} catch (Exception ex) {

				logger.warn("", ex);
			}
		}

		@Override
		public void authenticated(final XMPPConnection xmppc, final boolean resumed) {
			isAuthenticated = true;
		}

		@Override
		public void connectionClosed() {
			isConnected = false;
		}

		@Override
		public void connectionClosedOnError(final Exception excptn) {

			isConnected = false;
			this.exception = excptn;
		}

		@Override
		public void reconnectionSuccessful() {
			isConnected = true;
		}

		@Override
		public void reconnectingIn(final int i) {
		}

		@Override
		public void reconnectionFailed(final Exception excptn) {

			isConnected = false;
			this.exception = excptn;
		}

		@Override
		public void processPacket(final Stanza packet) throws NotConnectedException {

			final TypeHandler handler = typeHandlers.get(packet.getClass().getName());
			if (handler != null) {

				handler.handle(this, packet);

			} else {

				logger.warn("No type handler for type {}", packet.getClass().getName());
			}
		}
	}
}
