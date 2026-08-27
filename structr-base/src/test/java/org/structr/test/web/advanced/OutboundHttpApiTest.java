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
package org.structr.test.web.advanced;

import com.sun.net.httpserver.HttpServer;
import org.apache.commons.io.IOUtils;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObjectMap;
import org.structr.core.script.Scripting;
import org.structr.schema.action.ActionContext;
import org.structr.test.common.StructrTest;
import org.testng.annotations.Test;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * The 7.0 signature of the outbound HTTP functions.
 *
 * The assertions are made against a local server that records what it received, because the point of
 * the change is what goes over the wire: before 7.0 the contentType argument never reached the request
 * and every entity was sent as text/plain.
 */
public class OutboundHttpApiTest extends StructrTest {

	private final Map<String, String> lastHeaders = new LinkedHashMap<>();
	private String lastBody       = null;
	private String lastMethod     = null;
	private boolean challengeOnce = false;

	@Test
	public void testContentTypeReachesTheRequest() {

		withServer(port -> {

			final ActionContext ctx = new ActionContext(securityContext);

			evaluate(ctx, "${POST('http://localhost:" + port + "/', '{ \"a\": 1 }', 'application/json')}");
			assertEquals("POST must send the content type it was given", "application/json", mimeOf(lastHeaders.get("content-type")));

			evaluate(ctx, "${PUT('http://localhost:" + port + "/', 'x=1', 'application/x-www-form-urlencoded')}");
			assertEquals("PUT must send the content type it was given", "application/x-www-form-urlencoded", mimeOf(lastHeaders.get("content-type")));

			evaluate(ctx, "${PATCH('http://localhost:" + port + "/', '[]', 'application/json-patch+json')}");
			assertEquals("PATCH must send the content type it was given", "application/json-patch+json", mimeOf(lastHeaders.get("content-type")));

			evaluate(ctx, "${FETCH('http://localhost:" + port + "/', 'POST', 'body', 'text/csv')}");
			assertEquals("FETCH must send the content type it was given", "text/csv", mimeOf(lastHeaders.get("content-type")));
			assertEquals("FETCH must use the method it was given", "POST", lastMethod);
		});
	}

	@Test
	public void testHeadSendsCredentialsInTheRightOrder() {

		withServer(port -> {

			final ActionContext ctx = new ActionContext(securityContext);

			challengeOnce = true;

			evaluate(ctx, "${HEAD('http://localhost:" + port + "/', { username: 'user', password: 'secret' })}");

			assertEquals("HEAD must use the method it names", "HEAD", lastMethod);

			// the two used to be passed to HttpHelper the other way round, so the password was sent as
			// the user name
			final String authorization = lastHeaders.get("authorization");

			assertNotNull("credentials in the options object must produce an Authorization header", authorization);
			assertEquals("HEAD must send username:password, not password:username", "user:secret",
				new String(java.util.Base64.getDecoder().decode(authorization.substring("Basic ".length())), StandardCharsets.UTF_8));
		});
	}

	@Test
	public void testCharsetComesFromTheContentType() {

		withServer(port -> {

			final ActionContext ctx = new ActionContext(securityContext);

			evaluate(ctx, "${POST('http://localhost:" + port + "/', 'body', 'application/json; charset=ISO-8859-1')}");

			final String contentType = lastHeaders.get("content-type");

			assertEquals("the mime type must survive the charset", "application/json", mimeOf(contentType));
			assertTrue("the charset from the content type must be sent, got '" + contentType + "'",
				contentType != null && contentType.toLowerCase().contains("iso-8859-1"));
		});
	}

	@Test
	public void testOptionsCarryCredentialsAndHeaders() {

		withServer(port -> {

			final ActionContext ctx = new ActionContext(securityContext);

			// basic auth is sent after a challenge, so the server asks for it once
			challengeOnce = true;

			evaluate(ctx, "${POST('http://localhost:" + port + "/', 'body', 'text/plain', { username: 'user', password: 'secret', headers: { 'X-Custom': 'yes' } })}");

			assertNotNull("credentials in the options object must produce an Authorization header", lastHeaders.get("authorization"));
			assertEquals("headers from the options object must be sent", "yes", lastHeaders.get("x-custom"));
		});
	}

	@Test
	public void testPreemptiveBasicAuthIsSentWithoutAChallenge() {

		withServer(port -> {

			final ActionContext ctx = new ActionContext(securityContext);

			// the server never answers 401 here, which is the whole point: without preemptive the
			// credentials are never sent, because HttpClient waits for a challenge
			evaluate(ctx, "${POST('http://localhost:" + port + "/', 'body', 'text/plain', { username: 'user', password: 'secret' })}");
			assertNull("credentials must not be sent unasked by default", lastHeaders.get("authorization"));

			evaluate(ctx, "${POST('http://localhost:" + port + "/', 'body', 'text/plain', { username: 'user', password: 'secret', preemptive: true })}");

			final String authorization = lastHeaders.get("authorization");

			assertNotNull("preemptive must send the credentials on the first request", authorization);
			assertEquals("user:secret", new String(java.util.Base64.getDecoder().decode(authorization.substring("Basic ".length())), StandardCharsets.UTF_8));
		});
	}

	@Test
	public void testAnExplicitAuthorizationHeaderWinsOverPreemptive() {

		withServer(port -> {

			final ActionContext ctx = new ActionContext(securityContext);

			evaluate(ctx, "${POST('http://localhost:" + port + "/', 'body', 'text/plain', { username: 'user', password: 'secret', preemptive: true, headers: { 'Authorization': 'Bearer token' } })}");

			assertEquals("an Authorization header given explicitly must not be replaced", "Bearer token", lastHeaders.get("authorization"));
		});
	}

	@Test
	public void testAStringInTheOptionsPositionIsRejected() {

		withServer(port -> {

			final ActionContext ctx = new ActionContext(securityContext);

			// until 7.0 this position held the charset, so a string here is an unmigrated call. Failing
			// loudly beats sending the request with the wrong settings.
			final Object result = evaluate(ctx, "${POST('http://localhost:" + port + "/', 'body', 'application/json', 'UTF-8')}");

			assertNull("a string in the options position must not perform a request", result);
			assertNull("a string in the options position must not reach the server", lastMethod);
		});
	}

	@Test
	public void testResponseIsNotParsedUnlessAsked() {

		withServer(port -> {

			final ActionContext ctx = new ActionContext(securityContext);

			final GraphObjectMap raw = (GraphObjectMap) evaluate(ctx, "${POST('http://localhost:" + port + "/', 'body', 'application/json')}");
			assertTrue("the response body must be a string by default", raw.toMap().get("body") instanceof String);

			final GraphObjectMap parsed = (GraphObjectMap) evaluate(ctx, "${POST('http://localhost:" + port + "/', 'body', 'application/json', { parseResponse: true })}");
			assertTrue("parseResponse must parse the response body", parsed.toMap().get("body") instanceof Map);
		});
	}

	@Test
	public void testTimeoutIsHonouredByEveryVerb() {

		// a silently ignored timeout cannot be observed from a script: no error, no log, nothing in the
		// response, and the symptom is a hung call much later. One assertion per verb, so none of them
		// can quietly lose the option again.
		withSlowServer(port -> {

			final ActionContext ctx = new ActionContext(securityContext);

			for (final String script : new String[] {
				"${GET('http://localhost:" + port + "/', 'text/plain', { timeout: 1 })}",
				"${HEAD('http://localhost:" + port + "/', { timeout: 1 })}",
				"${DELETE('http://localhost:" + port + "/', { timeout: 1 })}",
				"${POST('http://localhost:" + port + "/', 'b', 'text/plain', { timeout: 1 })}",
				"${PUT('http://localhost:" + port + "/', 'b', 'text/plain', { timeout: 1 })}",
				"${PATCH('http://localhost:" + port + "/', 'b', 'text/plain', { timeout: 1 })}",
				"${FETCH('http://localhost:" + port + "/', 'POST', 'b', 'text/plain', { timeout: 1 })}"
			}) {

				final long start = System.currentTimeMillis();

				try {
					Scripting.evaluate(ctx, null, script, "test");

				} catch (FrameworkException expected) {
					// a timeout may surface as an exception, which is fine
				}

				final long elapsed = System.currentTimeMillis() - start;

				assertTrue("timeout was ignored by " + script + ", the call took " + elapsed + "ms", elapsed < 4000);
			}
		});
	}

	@Test
	public void testUnsupportedOptionsAreRefused() {

		withServer(port -> {

			final ActionContext ctx = new ActionContext(securityContext);

			// selector is a GET concept, so DELETE must say so rather than ignore it
			assertNull(evaluate(ctx, "${DELETE('http://localhost:" + port + "/', null, null, { selector: 'div' })}"));
			assertNull("a refused option must not perform a request", lastMethod);

			// and a plain typo
			assertNull(evaluate(ctx, "${POST('http://localhost:" + port + "/', 'b', 'text/plain', { timeOut: 5 })}"));
			assertNull("a refused option must not perform a request", lastMethod);

			// while a transport option is accepted everywhere
			evaluate(ctx, "${DELETE('http://localhost:" + port + "/', null, null, { timeout: 30 })}");
			assertEquals("DELETE", lastMethod);
		});
	}

	@Test
	public void testTheTwoBinaryDirectionsHaveTheirOwnNames() {

		withServer(port -> {

			final ActionContext ctx = new ActionContext(securityContext);

			// request side: POST sends the body as a binary stream
			evaluate(ctx, "${POST('http://localhost:" + port + "/', 'body', 'application/octet-stream', { binaryBody: true })}");
			assertEquals("POST", lastMethod);

			// response side belongs to GET, and the request-side name must not be accepted there
			lastMethod = null;

			assertNull(evaluate(ctx, "${GET('http://localhost:" + port + "/', 'text/plain', { binaryBody: true })}"));
			assertNull("binaryBody is not a GET option", lastMethod);
		});
	}

	@Test
	public void testEveryVerbReturnsTheSameShape() {

		withServer(port -> {

			final ActionContext ctx = new ActionContext(securityContext);

			// HEAD used to hand back HttpHelper's raw map, where the status is a String, so a script
			// comparing r.status to a number was right after POST and wrong after HEAD
			for (final String script : new String[] {
				"${GET('http://localhost:" + port + "/', 'text/plain')}",
				"${HEAD('http://localhost:" + port + "/')}",
				"${DELETE('http://localhost:" + port + "/')}",
				"${POST('http://localhost:" + port + "/', 'b', 'text/plain')}",
				"${PUT('http://localhost:" + port + "/', 'b', 'text/plain')}",
				"${PATCH('http://localhost:" + port + "/', 'b', 'text/plain')}",
				"${FETCH('http://localhost:" + port + "/', 'POST', 'b', 'text/plain')}"
			}) {

				final Object result = evaluate(ctx, script);

				assertTrue(script + " must return a response object", result instanceof GraphObjectMap);

				final Object status = ((GraphObjectMap) result).toMap().get("status");

				assertTrue(script + " must return an int status, got " + (status == null ? "null" : status.getClass().getSimpleName()),
					status instanceof Integer);
				assertEquals(script + " wrong status", 200, status);
			}
		});
	}

	@Test
	public void testDeleteCanSendABody() {

		withServer(port -> {

			final ActionContext ctx = new ActionContext(securityContext);

			evaluate(ctx, "${DELETE('http://localhost:" + port + "/', '{ \"id\": 1 }', 'application/json')}");

			assertEquals("DELETE", lastMethod);
			assertEquals("the body must reach the server", "{ \"id\": 1 }", lastBody);
			assertEquals("application/json", mimeOf(lastHeaders.get("content-type")));
		});
	}

	@Test
	public void testDeleteRefusesAnOptionsObjectInTheOldPosition() {

		withServer(port -> {

			final ActionContext ctx = new ActionContext(securityContext);

			// that position is the body now, so sending the object as one would be silent nonsense
			assertNull(evaluate(ctx, "${DELETE('http://localhost:" + port + "/', { parseResponse: true })}"));
			assertNull("no request may be made when the arguments are refused", lastMethod);

			// spelled the new way it works
			evaluate(ctx, "${DELETE('http://localhost:" + port + "/', null, null, { parseResponse: true })}");
			assertEquals("DELETE", lastMethod);
		});
	}

	// ----- private methods -----
	private Object evaluate(final ActionContext ctx, final String script) {

		try {
			return Scripting.evaluate(ctx, null, script, "test");

		} catch (FrameworkException fex) {

			fail("Unexpected exception while evaluating " + script + ": " + fex.getMessage());
		}

		return null;
	}

	/** The mime type without the charset, which the server reports as it was sent. */
	private String mimeOf(final String contentType) {

		return contentType != null ? contentType.split(";")[0].trim() : null;
	}

	/** A server that answers slowly, so a timeout has something to cut short. */
	private void withSlowServer(final PortConsumer body) {

		HttpServer server = null;

		try {

			server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);

			server.createContext("/", exchange -> {

				try { Thread.sleep(6000); } catch (InterruptedException iex) { Thread.currentThread().interrupt(); }

				exchange.sendResponseHeaders(200, -1);
				exchange.close();
			});

			server.start();

			body.accept(server.getAddress().getPort());

		} catch (Exception e) {

			e.printStackTrace();
			fail("Unexpected exception: " + e.getMessage());

		} finally {

			if (server != null) {
				server.stop(0);
			}
		}
	}

	private void withServer(final PortConsumer body) {

		HttpServer server = null;

		try {

			server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);

			server.createContext("/", exchange -> {

				lastHeaders.clear();
				lastMethod = exchange.getRequestMethod();

				for (final String name : exchange.getRequestHeaders().keySet()) {
					lastHeaders.put(name.toLowerCase(), exchange.getRequestHeaders().getFirst(name));
				}

				lastBody = IOUtils.toString(exchange.getRequestBody(), StandardCharsets.UTF_8);

				if (challengeOnce) {

					challengeOnce = false;

					exchange.getResponseHeaders().add("WWW-Authenticate", "Basic realm=\"test\"");
					exchange.sendResponseHeaders(401, -1);
					exchange.close();

					return;
				}

				final byte[] response = "{ \"ok\": true }".getBytes(StandardCharsets.UTF_8);

				exchange.getResponseHeaders().add("Content-Type", "application/json");
				exchange.sendResponseHeaders(200, response.length);

				try (final OutputStream out = exchange.getResponseBody()) {
					out.write(response);
				}
			});

			server.start();

			lastMethod    = null;
			challengeOnce = false;

			body.accept(server.getAddress().getPort());

		} catch (Exception e) {

			e.printStackTrace();
			fail("Unexpected exception: " + e.getMessage());

		} finally {

			if (server != null) {
				server.stop(0);
			}
		}
	}

	@FunctionalInterface
	private interface PortConsumer {
		void accept(final int port) throws Exception;
	}
}
