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

import io.restassured.RestAssured;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.test.web.StructrUiTest;
import org.structr.xmpp.XMPPContext;
import org.structr.xmpp.XMPPInfo;
import org.structr.xmpp.traits.definitions.XMPPClientTraitDefinition;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.fail;

public class XMPPTest extends StructrUiTest {

	@Test
	public void testMQTT() {

		final String clientType   = StructrTraits.XMPP_CLIENT;
		final Traits clientTraits = Traits.of(clientType);

		try (final Tx tx = app.tx()) {

			createAdminUser();

			tx.success();

		} catch (Throwable t) {

			t.printStackTrace();
		}

		try (final Tx tx = app.tx()) {

			app.create(clientType,
				new NodeAttribute<>(clientTraits.key(XMPPClientTraitDefinition.XMPP_USERNAME_PROPERTY),  "username"),
				new NodeAttribute<>(clientTraits.key(XMPPClientTraitDefinition.XMPP_PASSWORD_PROPERTY),  "password"),
				new NodeAttribute<>(clientTraits.key(XMPPClientTraitDefinition.XMPP_SERVICE_PROPERTY),   "service"),
				new NodeAttribute<>(clientTraits.key(XMPPClientTraitDefinition.XMPP_HOST_PROPERTY),      "host"),
				new NodeAttribute<>(clientTraits.key(XMPPClientTraitDefinition.XMPP_PORT_PROPERTY),      12345),
				new NodeAttribute<>(clientTraits.key(XMPPClientTraitDefinition.PRESENCE_MODE_PROPERTY),  "available")
			);

			tx.success();

		} catch (Throwable t) {

			t.printStackTrace();
		}

		// use RestAssured to check file
		RestAssured
			.given()
			.header(X_USER_HEADER, ADMIN_USERNAME)
			.header(X_PASSWORD_HEADER, ADMIN_PASSWORD)
			.expect()
			.statusCode(200)
			.body("result[0].type",                  equalTo(clientType))
			.body("result[0].isEnabled",             equalTo(false))
			.body("result[0].isConnected",           equalTo(false))
			.body("result[0].xmppUsername",          equalTo("username"))
			.body("result[0].xmppPassword",          equalTo("password"))
			.body("result[0].xmppService",           equalTo("service"))
			.body("result[0].xmppHost",              equalTo("host"))
			.body("result[0].xmppPort",              equalTo(12345))
			.body("result[0].presenceMode",          equalTo("available"))
			.when()
			.get("/" + clientType);
	}

	/**
	 * Ticket 1603: the client connected with SecurityMode.ifpossible, which offers TLS but continues
	 * in plaintext when the server does not answer the STARTTLS offer. An attacker in the network path
	 * only has to remove that offer from the stream features, and the client hands over the password.
	 *
	 * <p>This is that attack: a server that speaks just enough XMPP to answer the opening stream,
	 * offers SASL PLAIN and no STARTTLS, and records everything it is sent. With ifpossible the
	 * credentials arrive here; the connection must refuse instead.
	 */
	@Test
	public void testTheClientRefusesAServerThatOffersNoTls() throws Exception {

		try (final PlaintextXmppServer server = new PlaintextXmppServer()) {

			final XMPPTCPConnection connection = new XMPPTCPConnection(XMPPContext.getConnectionConfiguration(new TestClient(server.getPort())));

			try {

				connection.connect();

				fail("the client connected to a server that offers no TLS");

			} catch (SmackException.SecurityRequiredByClientException expected) {

				// this is what a stripped STARTTLS offer has to lead to

			} finally {

				connection.disconnect();
			}

			// connect() is refused before authentication, so nothing beyond the opening stream may arrive
			assertFalse("the client talked past the opening stream to a server that offers no TLS: " + server.getReceived(), server.getReceived().contains("<auth"));
		}
	}

	/**
	 * The service of an XMPP client is its XMPP domain, and Smack refuses to build a configuration
	 * without one - so leaving it unset made every connection attempt fail before it started.
	 */
	@Test
	public void testConnectionConfigurationCarriesTheServiceAsXmppDomain() throws Exception {

		final XMPPTCPConnectionConfiguration config = XMPPContext.getConnectionConfiguration(new TestClient(5222));

		assertEquals("example.com", config.getXMPPServiceDomain().toString());
	}

	/**
	 * A server that answers the opening stream, offers SASL PLAIN and never offers STARTTLS, and
	 * keeps everything it was sent so that a test can look for the credentials in it.
	 */
	private static class PlaintextXmppServer implements AutoCloseable {

		private final StringBuilder received = new StringBuilder();
		private final ServerSocket socket;
		private final Thread thread;

		public PlaintextXmppServer() throws IOException {

			socket = new ServerSocket(0);

			thread = new Thread(this::serve);

			thread.setDaemon(true);
			thread.start();
		}

		public int getPort() {

			return socket.getLocalPort();
		}

		public synchronized String getReceived() {

			return received.toString();
		}

		@Override
		public void close() throws IOException {

			socket.close();
		}

		private void serve() {

			try (final Socket connection = socket.accept()) {

				final InputStream in   = connection.getInputStream();
				final OutputStream out = connection.getOutputStream();
				final byte[] buffer    = new byte[8192];
				int read = in.read(buffer);

				record(buffer, read);

				out.write(("<?xml version='1.0'?>"
					+ "<stream:stream xmlns='jabber:client' xmlns:stream='http://etherx.jabber.org/streams'"
					+ " id='test' from='example.com' version='1.0'>"
					+ "<stream:features>"
					+ "<mechanisms xmlns='urn:ietf:params:xml:ns:xmpp-sasl'><mechanism>PLAIN</mechanism></mechanisms>"
					+ "</stream:features>").getBytes(StandardCharsets.UTF_8));
				out.flush();

				// from here on, anything the client sends is its authentication
				while ((read = in.read(buffer)) > 0) {

					record(buffer, read);
				}

			} catch (IOException ioex) {

				// the client hung up or the test closed the server, both are expected
			}
		}

		private synchronized void record(final byte[] buffer, final int length) {

			if (length > 0) {

				received.append(new String(buffer, 0, length, StandardCharsets.UTF_8));
			}
		}
	}

	private static class TestClient implements XMPPInfo {

		private final int port;

		public TestClient(final int port) {

			this.port = port;
		}

		@Override
		public String getUsername() {

			return "username";
		}

		@Override
		public String getPassword() {

			return "sup3rs3cret";
		}

		@Override
		public String getService() {

			return "example.com";
		}

		@Override
		public String getHostName() {

			return "127.0.0.1";
		}

		@Override
		public int getPort() {

			return port;
		}

		@Override
		public String getUuid() {

			return "0123456789abcdef0123456789abcdef";
		}
	}

}
