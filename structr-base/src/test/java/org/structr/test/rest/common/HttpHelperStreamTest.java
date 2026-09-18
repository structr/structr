/*
 * Copyright (C) 2010-2026 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.test.rest.common;

import com.sun.net.httpserver.HttpServer;
import org.structr.api.config.Settings;
import org.structr.common.error.FrameworkException;
import org.structr.rest.common.HttpHelper;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * {@link HttpHelper#getAsStream} hands back a live {@link InputStream} whose client nothing else can
 * close: {@code configure} builds a fresh one per call and the caller never sees it. So the stream has
 * to own the response and the client, and a failed request has to be reported rather than answered with
 * {@code null} -- a null used to surface as an NPE at the call site, with the cause only in the log, and
 * it swallowed the outgoing-whitelist refusal along with it.
 *
 * <p><b>Do not add a test asserting that the socket closes -- it cannot discriminate, and one was
 * written and removed here on 2026-08-28.</b> {@code configure} adds {@code Connection: close} to every
 * request, so Apache tears the connection down itself as soon as the entity reaches EOF, before the
 * caller's {@code close()} is even called; measured against a raw {@code ServerSocket}, the server sees
 * the client hang up identically whether or not the stream wraps its client. What the old code leaked is
 * therefore the {@code CloseableHttpClient} and its connection manager as <i>objects</i>, freed at GC
 * instead of deterministically -- not a live socket. The closing contract is pinned structurally below
 * instead, which is the honest limit of a black-box test here.</p>
 *
 * <p>No database and no {@code StructrTest}: none of this needs one, and {@link HttpHelperSsrfTest}
 * next door establishes that shape for testing this class.</p>
 */
public class HttpHelperStreamTest {

	/** Generous, because it is only ever reached when the assertion is already failing. */
	private static final int TIMEOUT_MILLIS = 10_000;

	private String previousWhitelist;

	@BeforeMethod
	public void setUp() {

		previousWhitelist = Settings.OutgoingURLWhitelist.getValue();

		// the default posture; the whitelist test below narrows it deliberately
		Settings.OutgoingURLWhitelist.setValue("*");
	}

	@AfterMethod
	public void tearDown() {

		Settings.OutgoingURLWhitelist.setValue(previousWhitelist);
	}

	// ----- the stream owns its response and client -----

	@Test
	public void testTheReturnedStreamOwnsItsConnectionAndClosesCleanly() throws Exception {

		// Structural, deliberately: see the class javadoc for why a socket-level assertion cannot tell the
		// wrapped stream from the raw one. What is checkable is that the body handed out is the owning
		// wrapper rather than Apache's own EofSensorInputStream, and that closing it -- twice, since a
		// caller in a finally block may well do that -- neither throws nor is refused.
		try (final ServerSocket server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {

			server.setSoTimeout(TIMEOUT_MILLIS);
			respondOnce(server, "HTTP/1.1 200 OK\r\nContent-Length: 5\r\n\r\nhello");

			final Map<String, Object> result = HttpHelper.getAsStream(addressOf(server));

			assertEquals("200", result.get(HttpHelper.FIELD_STATUS));

			final InputStream body = (InputStream) result.get(HttpHelper.FIELD_BODY);

			assertEquals("The body must be the wrapper that owns the response and the client, not Apache's"
				+ " own entity stream", FilterInputStream.class, body.getClass().getSuperclass());

			assertEquals("hello", new String(body.readAllBytes(), StandardCharsets.UTF_8));

			// closing is what closes the response and the client behind them
			body.close();
			body.close();
		}
	}

	// ----- a failure is reported, not answered with null -----

	@Test
	public void testAnUnreachableAddressReportsTheFailure() {

		final String address = "http://" + InetAddress.getLoopbackAddress().getHostAddress() + ":" + unusedPort() + "/gone";

		try {

			final Map<String, Object> result = HttpHelper.getAsStream(address);
			fail("An unreachable address must report the failure, but answered " + result);

		} catch (final FrameworkException expected) {

			assertEquals(422, expected.getStatus());
			assertTrue("The failure must name the address it could not reach, got: " + expected.getMessage(), expected.getMessage() != null && expected.getMessage().contains(address));
		}
	}

	@Test
	public void testAFailingBinaryGetReportsTheFailureRatherThanAnsweringNull() {

		// getBinary is the caller that made this matter: it read the stream out of a map that could be
		// null, so a failed binary GET surfaced as an NPE in HttpGetFunction rather than as this.
		final String address = "http://" + InetAddress.getLoopbackAddress().getHostAddress() + ":" + unusedPort() + "/blob";

		try {

			final Map<String, Object> result = HttpHelper.getBinary(address, null, null, null, Map.of(), true);
			fail("A failing binary GET must report the failure, but answered " + result);

		} catch (final FrameworkException expected) {

			assertEquals(422, expected.getStatus());
			assertTrue("The failure must name the address, got: " + expected.getMessage(), expected.getMessage() != null && expected.getMessage().contains(address));

			// The status and the address alone do not discriminate: getBinary always caught the NPE that a
			// null result caused and re-wrapped it, so it reported a 422 naming the address either way, with
			// the actual reason replaced by "Cannot invoke Map.get(Object)". The cause chain is what says
			// whether the transport failure was reported or merely tripped over.
			for (Throwable cause = expected; cause != null; cause = cause.getCause()) {

				assertFalse("The reported cause chain must not contain a NullPointerException -- the failure"
					+ " must be reported, not tripped over: " + cause, cause instanceof NullPointerException);
			}
		}
	}

	@Test
	public void testARefusedAddressReportsTheWhitelistRatherThanBeingSwallowed() throws Exception {

		// checkAddressAgainstWhitelist throws a FrameworkException naming the setting to change. The old
		// catch-Throwable-and-return-null turned that deliberate refusal into a silent nothing, so the
		// caller could not tell a blocked address from an empty response.
		Settings.OutgoingURLWhitelist.setValue("https://example\\.com/.*");

		try (final ServerSocket server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {

			try {

				final Map<String, Object> result = HttpHelper.getAsStream(addressOf(server));
				fail("A refused address must report the refusal, but answered " + result);

			} catch (final FrameworkException expected) {

				assertEquals(422, expected.getStatus());
				assertTrue("The refusal must name the setting to change, got: " + expected.getMessage(),
					expected.getMessage() != null
						&& expected.getMessage().contains(Settings.OutgoingURLWhitelist.getKey()));
			}
		}
	}

	// ----- a response with no body at all -----

	@Test
	public void testABodylessResponseAnswersANullBodyAndItsStatus() throws Exception {

		// resp.getEntity() is null for a 204, so the old code hit an NPE inside its own try block and
		// answered null -- indistinguishable from a transport failure.
		HttpServer server = null;

		try {

			server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
			server.createContext("/empty", exchange -> {
				exchange.sendResponseHeaders(204, -1);
				exchange.close();
			});
			server.start();

			final String address = "http://" + server.getAddress().getAddress().getHostAddress()
				+ ":" + server.getAddress().getPort() + "/empty";

			final Map<String, Object> result = HttpHelper.getAsStream(address);

			assertEquals("A body-less response must still report its status", "204", result.get(HttpHelper.FIELD_STATUS));
			assertNull("A body-less response must answer a null body, not fail", result.get(HttpHelper.FIELD_BODY));

		} finally {

			if (server != null) {

				server.stop(0);
			}
		}
	}

	// ----- helpers -----

	private String addressOf(final ServerSocket server) {

		return "http://" + server.getInetAddress().getHostAddress() + ":" + server.getLocalPort() + "/blob";
	}

	/** Accepts one connection on a daemon thread, reads the request head, and writes the given response. */
	private void respondOnce(final ServerSocket server, final String response) {

		final Thread responder = new Thread(() -> {

			try (final Socket accepted = server.accept()) {

				accepted.setSoTimeout(TIMEOUT_MILLIS);
				drainRequestHeaders(accepted.getInputStream());

				accepted.getOutputStream().write(response.getBytes(StandardCharsets.US_ASCII));
				accepted.getOutputStream().flush();

				// hold the connection open until the client is done with it
				accepted.getInputStream().read();

			} catch (final Throwable t) {

				// the test's own assertions report the failure; nothing useful to do on this thread
			}
		});

		responder.setDaemon(true);
		responder.start();
	}

	/** A port nothing is listening on: bound to find a free one, then released. */
	private int unusedPort() {

		try (final ServerSocket probe = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {

			return probe.getLocalPort();

		} catch (final Exception ex) {

			throw new RuntimeException("Unable to find an unused port", ex);
		}
	}

	/** Reads the request head up to and including the blank line, so the response can be written. */
	private void drainRequestHeaders(final InputStream in) throws Exception {

		final ByteArrayOutputStream head = new ByteArrayOutputStream();
		int b;

		while ((b = in.read()) != -1) {

			head.write(b);

			final byte[] seen = head.toByteArray();
			if (seen.length >= 4 && seen[seen.length - 4] == '\r' && seen[seen.length - 3] == '\n' && seen[seen.length - 2] == '\r' && seen[seen.length - 1] == '\n') {

				return;
			}
		}
	}
}
