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
package org.structr.test.xmpp;

import org.jivesoftware.smack.filter.StanzaTypeFilter;
import org.jivesoftware.smack.java7.Java7SmackInitializer;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.MultiUserChatManager;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Resourcepart;
import org.structr.api.RetryException;
import org.structr.api.schema.JsonSchema;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.script.Scripting;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.schema.action.ActionContext;
import org.structr.schema.export.StructrSchema;
import org.structr.test.web.StructrUiTest;
import org.structr.xmpp.XMPPContext;
import org.structr.xmpp.XMPPInfo;
import org.structr.xmpp.traits.definitions.XMPPClientTraitDefinition;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * Tests the delivery semantics of XMPPClient against a Prosody test container: incoming messages run the onXMPPMessage
 * method of the client in a transaction, outgoing messages are sent after the commit of the sending transaction.
 */
public class XMPPMessagingTest extends StructrUiTest {

	private static final String STRUCTR_USER     = "structr";
	private static final String STRUCTR_PASSWORD = "structr-test-password";
	private static final String PEER_USER        = "peer";
	private static final String PEER_PASSWORD    = "peer-test-password";
	private static final String STRUCTR_JID      = STRUCTR_USER + "@" + ProsodyTestSupport.DOMAIN;
	private static final String PEER_JID         = PEER_USER + "@" + ProsodyTestSupport.DOMAIN;

	// the callback of the client creates a group for each message, so that each processed message can be counted; group
	// names are unique, so a random suffix after a # keeps a message that is processed twice from failing the second time
	private static final String GROUP_CALLBACK = "{ $.create('Group', { name: $.arguments.message + '#' + Math.random() }); }";

	@Parameters("testDatabaseConnection")
	@BeforeClass(alwaysRun = true)
	@Override
	public void setup(@Optional final String testDatabaseConnection) {

		try {

			ProsodyTestSupport.start();
			ProsodyTestSupport.register(STRUCTR_USER, STRUCTR_PASSWORD);
			ProsodyTestSupport.register(PEER_USER, PEER_PASSWORD);

		} catch (Exception ex) {

			throw new RuntimeException("Unable to start the XMPP test server", ex);
		}

		super.setup(testDatabaseConnection);
	}

	@AfterClass(alwaysRun = true)
	@Override
	public void teardown() throws Exception {

		super.teardown();

		ProsodyTestSupport.stop();
	}

	@Test
	public void testIncomingMessageRunsTheCallbackOnce() throws Exception {

		final String prefix = uniquePrefix();
		final String client = createConnectedClient(GROUP_CALLBACK);
		final Peer peer     = new Peer();

		try {

			peer.send(STRUCTR_JID, prefix + "hello");

			tryWithTimeout(() -> getGroupNames(prefix).size() >= 1, () -> fail("The message must be processed"), 30_000, 100);

			sleep(2_000);

			assertEquals("The message must be processed exactly once", List.of(prefix + "hello"), getGroupNames(prefix));

		} finally {

			peer.close();
			setEnabled(client, false);
		}
	}

	@Test
	public void testSentMessageIsSentOnlyAfterCommit() throws Exception {

		final String prefix = uniquePrefix();
		final String client = createConnectedClient(GROUP_CALLBACK);
		final Peer peer     = new Peer();

		try {

			// rolled back: the message must never be sent
			try (final Tx tx = app.tx()) {

				sendFromStructr(client, prefix + "rolled-back");
			}

			try (final Tx tx = app.tx()) {

				sendFromStructr(client, prefix + "committed");

				sleep(1_000);

				assertTrue("The message must not be sent before the commit", peer.received(prefix).isEmpty());

				tx.success();
			}

			tryWithTimeout(() -> !peer.received(prefix).isEmpty(), () -> fail("The message must be sent after the commit"), 30_000, 100);

			sleep(1_000);

			assertEquals("Only the message of the committed transaction must be sent", List.of(prefix + "committed"), peer.received(prefix));

		} finally {

			peer.close();
			setEnabled(client, false);
		}
	}

	@Test
	public void testRepeatedTransactionSendsOnce() throws Exception {

		final String prefix          = uniquePrefix();
		final String client          = createConnectedClient(GROUP_CALLBACK);
		final AtomicInteger attempts = new AtomicInteger();
		final Peer peer              = new Peer();

		try {

			// the database terminating the first attempt is simulated by a RetryException
			RetryException.repeat(() -> {

				try (final Tx tx = app.tx()) {

					sendFromStructr(client, prefix + "repeated");

					if (attempts.incrementAndGet() == 1) {

						throw new RetryException("simulated");
					}

					tx.success();
				}
			});

			tryWithTimeout(() -> !peer.received(prefix).isEmpty(), () -> fail("The message must be sent after the commit"), 30_000, 100);

			sleep(1_000);

			assertEquals("The transaction must be repeated once", 2, attempts.get());
			assertEquals("The message must be sent once", List.of(prefix + "repeated"), peer.received(prefix));

		} finally {

			peer.close();
			setEnabled(client, false);
		}
	}

	@Test
	public void testReplyFromCallbackIsSentAfterTheCallbackCommitted() throws Exception {

		final String prefix              = uniquePrefix();
		final String callback            = "{ $.create('Group', { name: $.arguments.message + '#' + Math.random() }); $.this.doSendMessage({ recipient: '" + PEER_JID + "', message: 'reply:' + $.arguments.message }); }";
		final String client              = createConnectedClient(callback);
		final List<String> groupsAtReply = new CopyOnWriteArrayList<>();
		final Peer peer                  = new Peer(body -> groupsAtReply.addAll(getGroupNames(prefix)));

		try {

			peer.send(STRUCTR_JID, prefix + "request");

			tryWithTimeout(() -> !peer.received("reply:" + prefix).isEmpty(), () -> fail("The callback must send a reply"), 30_000, 100);

			sleep(1_000);

			assertEquals("The reply must be sent once", List.of("reply:" + prefix + "request"), peer.received("reply:" + prefix));
			assertEquals("The work of the callback must be committed when the reply arrives", List.of(prefix + "request"), groupsAtReply);

		} finally {

			peer.close();
			setEnabled(client, false);
		}
	}

	@Test
	public void testFailingCallbackDoesNotStopLaterMessages() throws Exception {

		final String prefix   = uniquePrefix();
		final String callback = "{ if ($.arguments.message.endsWith('failing')) { throw new Error('rejected by the test'); } $.create('Group', { name: $.arguments.message + '#' + Math.random() }); }";
		final String client   = createConnectedClient(callback);
		final Peer peer       = new Peer();

		try {

			peer.send(STRUCTR_JID, prefix + "before");
			peer.send(STRUCTR_JID, prefix + "failing");
			peer.send(STRUCTR_JID, prefix + "after");

			tryWithTimeout(() -> getGroupNames(prefix).size() >= 2, () -> fail("The messages around the failing one must be processed, got " + getGroupNames(prefix)), 30_000, 100);

			sleep(1_000);

			assertEquals("Each message must be processed once, the failing one not at all", Set.of(prefix + "before", prefix + "after"), new HashSet<>(getGroupNames(prefix)));
			assertEquals("No message may be processed twice", 2, getGroupNames(prefix).size());

		} finally {

			peer.close();
			setEnabled(client, false);
		}
	}

	@Test
	public void testMessageSentWhileDisconnectedIsDeliveredAfterReconnecting() throws Exception {

		// the server keeps messages for a user that is offline and delivers them when the user is available again

		final String prefix = uniquePrefix();
		final String client = createConnectedClient(GROUP_CALLBACK);
		final Peer peer     = new Peer();

		try {

			setEnabled(client, false);

			peer.send(STRUCTR_JID, prefix + "while-disconnected");

			sleep(1_000);

			assertTrue("A disconnected client must not process messages", getGroupNames(prefix).isEmpty());

			setEnabled(client, true);

			tryWithTimeout(() -> getGroupNames(prefix).size() >= 1, () -> fail("The message sent while the client was disconnected must be processed after reconnecting"), 30_000, 100);

			sleep(1_000);

			assertEquals("The message must be processed once", List.of(prefix + "while-disconnected"), getGroupNames(prefix));

		} finally {

			peer.close();
			setEnabled(client, false);
		}
	}

	@Test
	public void testBurstOfMessagesIsProcessedExactlyOnce() throws Exception {

		final int count     = 30;
		final String prefix = uniquePrefix();
		final String client = createConnectedClient(GROUP_CALLBACK);
		final Peer peer     = new Peer();

		try {

			for (int i = 0; i < count; i++) {

				peer.send(STRUCTR_JID, prefix + i);
			}

			tryWithTimeout(() -> getGroupNames(prefix).size() >= count, () -> fail("All messages must be processed, got " + getGroupNames(prefix).size()), 60_000, 200);

			sleep(2_000);

			final List<String> names = getGroupNames(prefix);

			assertEquals("Each message must be processed exactly once", count, names.size());
			assertEquals("No message may be processed twice", count, new HashSet<>(names).size());

		} finally {

			peer.close();
			setEnabled(client, false);
		}
	}

	@Test
	public void testRoomMessageIsSentOnlyAfterCommit() throws Exception {

		// joining waits for the server and happens right away, the message to the room is sent after the commit

		final String prefix          = uniquePrefix();
		final String room            = uniqueRoom();
		final String client          = createConnectedClient(GROUP_CALLBACK);
		final AtomicInteger attempts = new AtomicInteger();
		final Peer peer              = new Peer();

		try {

			peer.joinRoom(room);

			try (final Tx tx = app.tx()) {

				runScript(client, "${{ $.this.doJoinChat({ chatRoom: '" + room + "', nickname: 'structr' }); }}");

				tx.success();
			}

			// rolled back: the message must never be sent
			try (final Tx tx = app.tx()) {

				runScript(client, sendChatMessage(room, prefix + "rolled-back"));
			}

			try (final Tx tx = app.tx()) {

				runScript(client, sendChatMessage(room, prefix + "committed"));

				sleep(1_000);

				assertTrue("The message must not be sent before the commit", peer.receivedInRoom(prefix).isEmpty());

				tx.success();
			}

			// repeated: the message must be sent once
			RetryException.repeat(() -> {

				try (final Tx tx = app.tx()) {

					runScript(client, sendChatMessage(room, prefix + "repeated"));

					if (attempts.incrementAndGet() == 1) {

						throw new RetryException("simulated");
					}

					tx.success();
				}
			});

			tryWithTimeout(() -> peer.receivedInRoom(prefix).size() >= 2, () -> fail("The messages must be sent to the room, got " + peer.receivedInRoom(prefix)), 30_000, 100);

			sleep(1_000);

			assertEquals("Only the messages of the committed transactions must reach the room, each once", List.of(prefix + "committed", prefix + "repeated"), peer.receivedInRoom(prefix));

		} finally {

			peer.close();
			setEnabled(client, false);
		}
	}

	@Test
	public void testRoomMessageRunsTheCallback() throws Exception {

		// a message that another occupant sends to a room the client has joined runs the onXMPPMessage method

		final String prefix = uniquePrefix();
		final String room   = uniqueRoom();
		final String client = createConnectedClient(GROUP_CALLBACK);
		final Peer peer     = new Peer();

		try {

			// joining twice must do no harm, a script may join its rooms regularly
			for (int i = 0; i < 2; i++) {

				try (final Tx tx = app.tx()) {

					runScript(client, "${{ $.this.doJoinChat({ chatRoom: '" + room + "', nickname: 'structr' }); }}");

					tx.success();
				}
			}

			peer.joinRoom(room);
			peer.sendToRoom(prefix + "from-peer");

			tryWithTimeout(() -> getGroupNames(prefix).size() >= 1, () -> fail("The room message must be processed"), 30_000, 100);

			sleep(1_000);

			assertEquals("The room message must be processed once", List.of(prefix + "from-peer"), getGroupNames(prefix));

		} finally {

			peer.close();
			setEnabled(client, false);
		}
	}

	@Test
	public void testSubscriptionRequestIsSentOnlyAfterCommit() throws Exception {

		// the presence stanzas share the way of the messages: sent once the transaction has committed

		final String client = createConnectedClient(GROUP_CALLBACK);
		final Peer peer     = new Peer();
		final String script = "${{ $.this.doSubscribe({ recipient: '" + PEER_JID + "' }); }}";

		try {

			// rolled back: the request must never be sent
			try (final Tx tx = app.tx()) {

				runScript(client, script);
			}

			try (final Tx tx = app.tx()) {

				runScript(client, script);

				sleep(1_000);

				assertTrue("The request must not be sent before the commit", peer.subscriptionRequests().isEmpty());

				tx.success();
			}

			tryWithTimeout(() -> !peer.subscriptionRequests().isEmpty(), () -> fail("The subscription request must be sent after the commit"), 30_000, 100);

			sleep(1_000);

			assertEquals("The request of the committed transaction must arrive once", List.of(STRUCTR_JID), peer.subscriptionRequests());

		} finally {

			// the server keeps a request until it is answered and delivers it again at the next login of the peer
			peer.denySubscription(STRUCTR_JID);
			peer.close();
			setEnabled(client, false);
		}
	}

	@Test
	public void testSubscriptionRequestRunsTheCallbackWhichConfirmsIt() throws Exception {

		final String prefix   = uniquePrefix();
		final String callback = "{ $.create('Group', { name: '" + prefix + "' + $.arguments.type + ':' + $.arguments.sender + '#' + Math.random() }); "
			+ "if ($.arguments.type === 'subscribe') { $.this.doConfirmSubscription({ recipient: $.arguments.sender }); } }";
		final String client   = createConnectedClient(GROUP_CALLBACK, callback);
		final String user     = registerPeerUser();
		final Peer peer       = new Peer(user, PEER_PASSWORD, body -> {});

		try {

			peer.requestSubscription(STRUCTR_JID);

			tryWithTimeout(() -> peer.presences().contains("subscribed:" + STRUCTR_JID), () -> fail("The callback must confirm the subscription, got " + peer.presences()), 30_000, 100);

			assertEquals("The callback must receive the request once", List.of(prefix + "subscribe:" + user + "@" + ProsodyTestSupport.DOMAIN), getGroupNames(prefix + "subscribe:"));
			assertFalse("The request must not be rejected", peer.presences().contains("unsubscribed:" + STRUCTR_JID));

		} finally {

			peer.close();
			setEnabled(client, false);
		}
	}

	@Test
	public void testSubscriptionRequestCanBeDeniedByTheCallback() throws Exception {

		final String callback = "{ if ($.arguments.type === 'subscribe') { $.this.doDenySubscription({ recipient: $.arguments.sender }); } }";
		final String client   = createConnectedClient(GROUP_CALLBACK, callback);
		final String user     = registerPeerUser();
		final Peer peer       = new Peer(user, PEER_PASSWORD, body -> {});

		try {

			peer.requestSubscription(STRUCTR_JID);

			tryWithTimeout(() -> peer.presences().contains("unsubscribed:" + STRUCTR_JID), () -> fail("The callback must deny the subscription, got " + peer.presences()), 30_000, 100);

			assertFalse("The request must not be confirmed", peer.presences().contains("subscribed:" + STRUCTR_JID));

		} finally {

			peer.close();
			setEnabled(client, false);
		}
	}

	@Test
	public void testSubscriptionRequestWithoutCallbackIsRejected() throws Exception {

		// without an onXMPPPresence method, a request is rejected as before, so that it does not stay pending

		final String client = createConnectedClient(GROUP_CALLBACK);
		final String user   = registerPeerUser();
		final Peer peer     = new Peer(user, PEER_PASSWORD, body -> {});

		try {

			peer.requestSubscription(STRUCTR_JID);

			tryWithTimeout(() -> peer.presences().contains("unsubscribed:" + STRUCTR_JID), () -> fail("The request must be rejected, got " + peer.presences()), 30_000, 100);

		} finally {

			peer.close();
			setEnabled(client, false);
		}
	}

	@Test
	public void testClientCreatedEnabledWithoutPresenceModeReceivesMessages() throws Exception {

		// the client connects when it is created and announces itself as available once it has logged in, otherwise the
		// server would keep the messages for it offline

		final String prefix       = uniquePrefix();
		final Traits clientTraits = Traits.of(StructrTraits.XMPP_CLIENT);
		final Peer peer           = new Peer();

		final String clientId;

		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createFromDatabase(app);

			schema.getType(StructrTraits.XMPP_CLIENT).addMethod("onXMPPMessage", GROUP_CALLBACK);

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();
		}

		try (final Tx tx = app.tx()) {

			clientId = app.create(StructrTraits.XMPP_CLIENT,
				new NodeAttribute<>(clientTraits.key(XMPPClientTraitDefinition.XMPP_USERNAME_PROPERTY), STRUCTR_USER),
				new NodeAttribute<>(clientTraits.key(XMPPClientTraitDefinition.XMPP_PASSWORD_PROPERTY), STRUCTR_PASSWORD),
				new NodeAttribute<>(clientTraits.key(XMPPClientTraitDefinition.XMPP_SERVICE_PROPERTY),  ProsodyTestSupport.DOMAIN),
				new NodeAttribute<>(clientTraits.key(XMPPClientTraitDefinition.XMPP_HOST_PROPERTY),     ProsodyTestSupport.getHost()),
				new NodeAttribute<>(clientTraits.key(XMPPClientTraitDefinition.XMPP_PORT_PROPERTY),     ProsodyTestSupport.getPort()),
				new NodeAttribute<>(clientTraits.key(XMPPClientTraitDefinition.IS_ENABLED_PROPERTY),    true)
			).getUuid();

			tx.success();
		}

		try {

			peer.send(STRUCTR_JID, prefix + "to-new-client");

			tryWithTimeout(() -> getGroupNames(prefix).size() >= 1, () -> fail("The client created as enabled must receive the message"), 30_000, 100);

		} finally {

			peer.close();
			setEnabled(clientId, false);
		}
	}

	@Test
	public void testCallbackReceivesTheSenderAsText() throws Exception {

		// the sender is the full address with resource, as a string that a script can take apart
		final String prefix   = uniquePrefix();
		final String callback = "{ $.create('Group', { name: '" + prefix + "' + $.arguments.sender.split('/')[0] + '#' + Math.random() }); }";
		final String client   = createConnectedClient(callback);
		final Peer peer       = new Peer();

		try {

			peer.send(STRUCTR_JID, "who-am-i");

			tryWithTimeout(() -> getGroupNames(prefix).size() >= 1, () -> fail("The callback must receive the sender as text"), 30_000, 100);

			assertEquals(List.of(prefix + PEER_JID), getGroupNames(prefix));

		} finally {

			peer.close();
			setEnabled(client, false);
		}
	}

	@Test
	public void testLostConnectionIsRestored() throws Exception {

		final String prefix = uniquePrefix();
		final String client = createConnectedClient(GROUP_CALLBACK);
		final Peer peer     = new Peer();

		try {

			ProsodyTestSupport.closeSessions(STRUCTR_JID);

			tryWithTimeout(() -> !isConnected(client), () -> fail("The lost connection must be recorded"), 30_000, 100);
			tryWithTimeout(() -> isConnected(client), () -> fail("The connection must be restored"), 60_000, 200);

			peer.send(STRUCTR_JID, prefix + "after-reconnect");

			tryWithTimeout(() -> getGroupNames(prefix).size() >= 1, () -> fail("The restored connection must receive messages"), 30_000, 100);

		} finally {

			peer.close();
			setEnabled(client, false);
		}
	}

	@Test
	public void testClientDisabledWhileTheConnectionIsLostStaysDisconnected() throws Exception {

		final String prefix = uniquePrefix();
		final String client = createConnectedClient(GROUP_CALLBACK);
		final Peer peer     = new Peer();

		try {

			ProsodyTestSupport.closeSessions(STRUCTR_JID);

			tryWithTimeout(() -> !isConnected(client), () -> fail("The lost connection must be recorded"), 30_000, 100);

			setEnabled(client, false);

			// longer than the delay between two reconnection attempts
			sleep(12_000);

			assertFalse("A disabled client must not reconnect", isConnected(client));

			peer.send(STRUCTR_JID, prefix + "while-disabled");

			sleep(2_000);

			assertTrue("A disabled client must not receive messages", getGroupNames(prefix).isEmpty());

		} finally {

			peer.close();
		}
	}

	@Test
	public void testRoomsAreJoinedAgainWhenTheConnectionIsRestored() throws Exception {

		final String prefix = uniquePrefix();
		final String room   = uniqueRoom();
		final String client = createConnectedClient(GROUP_CALLBACK);
		final Peer peer     = new Peer();

		try {

			try (final Tx tx = app.tx()) {

				runScript(client, "${{ $.this.doJoinChat({ chatRoom: '" + room + "', nickname: 'structr' }); }}");

				tx.success();
			}

			peer.joinRoom(room);

			ProsodyTestSupport.closeSessions(STRUCTR_JID);

			tryWithTimeout(() -> !isConnected(client), () -> fail("The lost connection must be recorded"), 30_000, 100);
			tryWithTimeout(() -> isConnected(client), () -> fail("The connection must be restored"), 60_000, 200);

			// the client leaves the room before it joins again, which takes up to the reply timeout of five seconds
			sleep(7_000);

			peer.sendToRoom(prefix + "after-reconnect");

			tryWithTimeout(() -> getGroupNames(prefix).size() >= 1, () -> fail("The client must be in the room again after the connection is restored"), 30_000, 100);

		} finally {

			peer.close();
			setEnabled(client, false);
		}
	}

	@Test
	public void testClientWithWrongPasswordIsNotConnected() throws Exception {

		final Traits clientTraits = Traits.of(StructrTraits.XMPP_CLIENT);
		final String clientId;

		try (final Tx tx = app.tx()) {

			clientId = app.create(StructrTraits.XMPP_CLIENT,
				new NodeAttribute<>(clientTraits.key(XMPPClientTraitDefinition.XMPP_USERNAME_PROPERTY), STRUCTR_USER),
				new NodeAttribute<>(clientTraits.key(XMPPClientTraitDefinition.XMPP_PASSWORD_PROPERTY), "wrong-password"),
				new NodeAttribute<>(clientTraits.key(XMPPClientTraitDefinition.XMPP_SERVICE_PROPERTY),  ProsodyTestSupport.DOMAIN),
				new NodeAttribute<>(clientTraits.key(XMPPClientTraitDefinition.XMPP_HOST_PROPERTY),     ProsodyTestSupport.getHost()),
				new NodeAttribute<>(clientTraits.key(XMPPClientTraitDefinition.XMPP_PORT_PROPERTY),     ProsodyTestSupport.getPort())
			).getUuid();

			tx.success();
		}

		try (final Tx tx = app.tx()) {

			app.getNodeById(StructrTraits.XMPP_CLIENT, clientId).setProperty(clientTraits.key(XMPPClientTraitDefinition.IS_ENABLED_PROPERTY), true);

			tx.success();
		}

		assertFalse("A client that cannot log in must not count as connected", isConnected(clientId));

		// no reconnection keeps trying with the wrong password
		sleep(7_000);

		assertFalse("A client that cannot log in must stay disconnected", isConnected(clientId));
	}

	// ----- private methods -----
	private String createConnectedClient(final String callback) throws Exception {

		return createConnectedClient(callback, null);
	}

	/**
	 * Adds the given sources as onXMPPMessage and onXMPPPresence methods to XMPPClient, creates a client for the Structr
	 * account and enables it in a second transaction, so that it connects and announces its presence, which the server
	 * needs to deliver messages to it.
	 */
	private String createConnectedClient(final String callback, final String presenceCallback) throws Exception {

		final Traits clientTraits = Traits.of(StructrTraits.XMPP_CLIENT);
		final String clientId;

		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createFromDatabase(app);

			schema.getType(StructrTraits.XMPP_CLIENT).addMethod("onXMPPMessage", callback);

			if (presenceCallback != null) {

				schema.getType(StructrTraits.XMPP_CLIENT).addMethod("onXMPPPresence", presenceCallback);
			}

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();
		}

		try (final Tx tx = app.tx()) {

			final NodeInterface client = app.create(StructrTraits.XMPP_CLIENT,
				new NodeAttribute<>(clientTraits.key(XMPPClientTraitDefinition.XMPP_USERNAME_PROPERTY), STRUCTR_USER),
				new NodeAttribute<>(clientTraits.key(XMPPClientTraitDefinition.XMPP_PASSWORD_PROPERTY), STRUCTR_PASSWORD),
				new NodeAttribute<>(clientTraits.key(XMPPClientTraitDefinition.XMPP_SERVICE_PROPERTY),  ProsodyTestSupport.DOMAIN),
				new NodeAttribute<>(clientTraits.key(XMPPClientTraitDefinition.XMPP_HOST_PROPERTY),     ProsodyTestSupport.getHost()),
				new NodeAttribute<>(clientTraits.key(XMPPClientTraitDefinition.XMPP_PORT_PROPERTY),     ProsodyTestSupport.getPort()),
				new NodeAttribute<>(clientTraits.key(XMPPClientTraitDefinition.PRESENCE_MODE_PROPERTY), "available")
			);

			clientId = client.getUuid();

			tx.success();
		}

		setEnabled(clientId, true);

		return clientId;
	}

	private void setEnabled(final String clientId, final boolean enabled) throws FrameworkException {

		final Traits clientTraits = Traits.of(StructrTraits.XMPP_CLIENT);

		try (final Tx tx = app.tx()) {

			app.getNodeById(StructrTraits.XMPP_CLIENT, clientId).setProperty(clientTraits.key(XMPPClientTraitDefinition.IS_ENABLED_PROPERTY), enabled);

			tx.success();
		}

		try (final Tx tx = app.tx()) {

			final Object connected = app.getNodeById(StructrTraits.XMPP_CLIENT, clientId).getProperty(clientTraits.key(XMPPClientTraitDefinition.IS_CONNECTED_PROPERTY));

			assertEquals("The client must be connected exactly when it is enabled", enabled, Boolean.TRUE.equals(connected));

			tx.success();
		}
	}

	/**
	 * Sends a message to the peer through the doSendMessage method of the client, in the current transaction.
	 */
	private void sendFromStructr(final String clientId, final String message) throws FrameworkException {

		final NodeInterface client = app.getNodeById(StructrTraits.XMPP_CLIENT, clientId);

		Scripting.replaceVariables(new ActionContext(securityContext), client, "${{ $.this.doSendMessage({ recipient: '" + PEER_JID + "', message: '" + message + "' }); }}");
	}

	private boolean isConnected(final String clientId) {

		try (final Tx tx = app.tx()) {

			final Object connected = app.getNodeById(StructrTraits.XMPP_CLIENT, clientId).getProperty(Traits.of(StructrTraits.XMPP_CLIENT).key(XMPPClientTraitDefinition.IS_CONNECTED_PROPERTY));

			tx.success();

			return Boolean.TRUE.equals(connected);

		} catch (FrameworkException fex) {

			throw new RuntimeException(fex);
		}
	}

	private void runScript(final String clientId, final String script) throws FrameworkException {

		Scripting.replaceVariables(new ActionContext(securityContext), app.getNodeById(StructrTraits.XMPP_CLIENT, clientId), script);
	}

	private static String sendChatMessage(final String room, final String message) {

		return "${{ $.this.doSendChatMessage({ chatRoom: '" + room + "', message: '" + message + "' }); }}";
	}

	/**
	 * Registers a new account on the server for a peer of its own and returns its user name.
	 */
	private static String registerPeerUser() throws Exception {

		final String user = "peer" + UUID.randomUUID().toString().replace("-", "");

		ProsodyTestSupport.register(user, PEER_PASSWORD);

		return user;
	}

	private static String uniqueRoom() {

		return "room-" + UUID.randomUUID() + "@" + ProsodyTestSupport.MUC_DOMAIN;
	}

	private List<String> getGroupNames(final String prefix) {

		try (final Tx tx = app.tx()) {

			final List<String> names = new ArrayList<>();

			for (final NodeInterface group : app.nodeQuery(StructrTraits.GROUP).getAsList()) {

				final String name = group.getName();
				if (name != null && name.startsWith(prefix)) {

					names.add(name.substring(0, name.lastIndexOf('#')));
				}
			}

			tx.success();

			return names;

		} catch (FrameworkException fex) {

			throw new RuntimeException(fex);
		}
	}

	private static String uniquePrefix() {

		return "xmpp-" + UUID.randomUUID() + "-";
	}

	private static void sleep(final long millis) {

		try {

			Thread.sleep(millis);

		} catch (InterruptedException iex) {

			Thread.currentThread().interrupt();
		}
	}

	/**
	 * The other side of the conversation: a second account on the server that sends messages to the client of Structr
	 * and records the ones it receives.
	 */
	private static class Peer implements AutoCloseable {

		private final List<String> received             = new CopyOnWriteArrayList<>();
		private final List<String> receivedInRoom       = new CopyOnWriteArrayList<>();
		private final List<String> subscriptionRequests = new CopyOnWriteArrayList<>();
		private final List<String> presences            = new CopyOnWriteArrayList<>();
		private final XMPPTCPConnection connection;
		private MultiUserChat room                      = null;

		Peer() throws Exception {

			this(body -> {});
		}

		/**
		 * @param onReceive runs for each received message before it is recorded
		 */
		Peer(final Consumer<String> onReceive) throws Exception {

			this(PEER_USER, PEER_PASSWORD, onReceive);
		}

		/**
		 * A peer with an account of its own, for tests that leave state on the server, like a subscription.
		 */
		Peer(final String username, final String password, final Consumer<String> onReceive) throws Exception {

			new Java7SmackInitializer().initialize();

			connection = new XMPPTCPConnection(XMPPContext.getConnectionConfiguration(new PeerInfo(username, password)));

			connection.addAsyncStanzaListener(stanza -> {

				final String body = ((Message) stanza).getBody();
				if (body != null) {

					onReceive.accept(body);
					received.add(body);
				}

			}, StanzaTypeFilter.MESSAGE);

			connection.addAsyncStanzaListener(stanza -> {

				final Presence presence = (Presence) stanza;
				if (presence.getType() == Presence.Type.subscribe) {

					subscriptionRequests.add(presence.getFrom().asBareJid().toString());
				}

				presences.add(presence.getType().name() + ":" + presence.getFrom().asBareJid());

			}, StanzaTypeFilter.PRESENCE);

			// the requests are answered by the test, not by the roster
			Roster.getInstanceFor(connection).setSubscriptionMode(Roster.SubscriptionMode.manual);

			connection.connect();
			connection.login();

			// without an available presence, the server keeps messages for the peer offline
			connection.sendStanza(new Presence(Presence.Type.available));
		}

		void send(final String recipient, final String body) throws Exception {

			final Message message = new Message(JidCreate.entityBareFrom(recipient));

			message.setType(Message.Type.chat);
			message.setBody(body);

			connection.sendStanza(message);
		}

		/**
		 * Joins the given chat room, which the server creates if it does not exist yet.
		 */
		void joinRoom(final String roomJid) throws Exception {

			room = MultiUserChatManager.getInstanceFor(connection).getMultiUserChat(JidCreate.entityBareFrom(roomJid));

			room.addMessageListener(message -> {

				if (message.getBody() != null) {

					receivedInRoom.add(message.getBody());
				}
			});

			room.join(Resourcepart.from("peer"));
		}

		void sendToRoom(final String body) throws Exception {

			room.sendMessage(body);
		}

		/**
		 * @return the senders of the subscription requests the peer received, as bare JIDs
		 */
		List<String> subscriptionRequests() {

			return List.copyOf(subscriptionRequests);
		}

		void denySubscription(final String jid) throws Exception {

			sendPresence(Presence.Type.unsubscribed, jid);
		}

		void requestSubscription(final String jid) throws Exception {

			sendPresence(Presence.Type.subscribe, jid);
		}

		/**
		 * @return the presences the peer received, as type:sender
		 */
		List<String> presences() {

			return List.copyOf(presences);
		}

		private void sendPresence(final Presence.Type type, final String jid) throws Exception {

			final Presence presence = new Presence(type);

			presence.setTo(JidCreate.bareFrom(jid));

			connection.sendStanza(presence);
		}

		/**
		 * @return the messages received in the room that start with the given prefix
		 */
		List<String> receivedInRoom(final String prefix) {

			return receivedInRoom.stream().filter(body -> body.startsWith(prefix)).toList();
		}

		/**
		 * @return the received messages that start with the given prefix
		 */
		List<String> received(final String prefix) {

			return received.stream().filter(body -> body.startsWith(prefix)).toList();
		}

		@Override
		public void close() {

			connection.disconnect();
		}
	}

	private static class PeerInfo implements XMPPInfo {

		private final String username;
		private final String password;

		PeerInfo(final String username, final String password) {

			this.username = username;
			this.password = password;
		}

		@Override
		public String getUsername() {

			return username;
		}

		@Override
		public String getPassword() {

			return password;
		}

		@Override
		public String getService() {

			return ProsodyTestSupport.DOMAIN;
		}

		@Override
		public String getHostName() {

			return ProsodyTestSupport.getHost();
		}

		@Override
		public int getPort() {

			return ProsodyTestSupport.getPort();
		}

		@Override
		public String getUuid() {

			return username;
		}
	}
}
