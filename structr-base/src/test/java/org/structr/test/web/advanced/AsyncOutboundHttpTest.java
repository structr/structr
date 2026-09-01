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
import org.structr.common.error.FrameworkException;
import org.structr.core.script.polyglot.config.ScriptConfig;
import org.structr.schema.action.Actions;
import org.structr.test.common.StructrTest;
import org.testng.annotations.Test;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.fail;

/**
 * The JavaScript-only async variants of the outbound HTTP functions, {@code $.GET.async(...)}.
 *
 * <p><b>Concurrency is asserted with a latch, not a clock.</b> The server counts each request in and
 * then waits for the others before answering any of them, so a 200 is reachable only if the requests
 * really were in flight at the same time, and anything serial deadlocks itself into a 500. That makes
 * the assertion deterministic rather than a wall-clock comparison that goes flaky on a loaded CI box --
 * the rule {@code LoginFailureUniformityTest} states for the same reason.</p>
 *
 * <p>The server here is <b>not</b> {@code OutboundHttpApiTest}'s. That one creates the JDK HttpServer
 * with a null executor, which dispatches every exchange on one thread, so a concurrency test against it
 * would measure the server serialising rather than the client parallelising.</p>
 *
 * <p>Scripts run through {@code wrapJsInMain}, which is the shape a SchemaMethod uses and the only one
 * in which await keeps the script's return value.</p>
 */
public class AsyncOutboundHttpTest extends StructrTest {

	private final Map<String, Map<String, String>> headersByPath = new ConcurrentHashMap<>();

	// ----- helpers -----

	private Object wrapped(final String source) throws FrameworkException {

		return Actions.execute(securityContext, null, "${{" + source + "}}", Collections.EMPTY_MAP, "asyncHttpTest", null,
			ScriptConfig.builder().wrapJsInMain(true).build());
	}

	/** Evaluated as a module answering its completion value, as an inline ${{ }} does. */
	private Object unwrapped(final String source) throws FrameworkException {

		return Actions.execute(securityContext, null, "${{" + source + "}}", Collections.EMPTY_MAP, "asyncHttpTest", null,
			ScriptConfig.builder().wrapJsInMain(false).build());
	}

	/**
	 * A server that answers only once {@code expected} requests have arrived together.
	 *
	 * The handler waits at a barrier. If the caller issued its requests concurrently, all of them are
	 * inside the handler at the same moment, the barrier trips, and each answers 200 with the request
	 * path as its body. If the caller issued them one at a time, the first waits for peers that cannot
	 * arrive until it returns, times out, and <b>breaks</b> the barrier -- so every later request fails
	 * at it immediately and answers 500 as well.
	 *
	 * A barrier rather than a latch precisely because it breaks: a CountDownLatch never resets, so a
	 * second serial request would find the count already satisfied by the first one's own arrival and
	 * succeed, which would make the serial and concurrent cases indistinguishable.
	 *
	 * There is no third outcome and no timing assertion.
	 */
	private void withRendezvousServer(final int expected, final int timeoutSeconds, final PortConsumer body) {

		HttpServer server         = null;
		ExecutorService dispatcher = null;

		try {

			final CyclicBarrier arrived = new CyclicBarrier(expected);

			server     = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
			dispatcher = Executors.newFixedThreadPool(Math.max(2, expected));

			// without its own executor the JDK server handles one exchange at a time, which would make
			// every request in this class look serial regardless of what the client did
			server.setExecutor(dispatcher);

			server.createContext("/", exchange -> {

				final String path                 = exchange.getRequestURI().getPath();
				final Map<String, String> headers = new ConcurrentHashMap<>();

				for (final String name : exchange.getRequestHeaders().keySet()) {
					headers.put(name.toLowerCase(), exchange.getRequestHeaders().getFirst(name));
				}

				headersByPath.put(path, headers);

				boolean allArrived = false;

				try {

					arrived.await(timeoutSeconds, TimeUnit.SECONDS);

					allArrived = true;

				} catch (final TimeoutException | BrokenBarrierException noRendezvous) {

					// nobody else turned up in time, or an earlier request already gave up and broke the barrier

				} catch (final InterruptedException ie) {

					Thread.currentThread().interrupt();
				}

				final byte[] response = path.getBytes(StandardCharsets.UTF_8);

				exchange.getResponseHeaders().add("Content-Type", "text/plain");
				exchange.sendResponseHeaders(allArrived ? 200 : 500, response.length);

				try (final OutputStream out = exchange.getResponseBody()) {
					out.write(response);
				}
			});

			server.start();
			headersByPath.clear();

			body.accept(server.getAddress().getPort());

		} catch (final Exception e) {

			e.printStackTrace();
			fail("Unexpected exception: " + e.getMessage());

		} finally {

			if (server != null) {
				server.stop(0);
			}

			// server.stop() does not touch an executor it was given
			if (dispatcher != null) {
				dispatcher.shutdownNow();
			}
		}
	}

	@FunctionalInterface
	private interface PortConsumer {
		void accept(final int port) throws Exception;
	}

	// ----- the feature -----

	@Test
	public void testAsyncCallsRunConcurrently() {

		withRendezvousServer(3, 20, port -> {

			final String url = "'http://localhost:" + port + "/'";

			// all three started before anything is awaited: that is where the concurrency comes from, not
			// from the await, which only joins them
			final Object result = wrapped(
				  "const a = $.GET.async(" + url + ");"
				+ "const b = $.GET.async(" + url + ");"
				+ "const c = $.GET.async(" + url + ");"
				+ "const r = await Promise.all([a, b, c]);"
				+ "return r.map(x => x.status).join(',');");

			assertEquals("three async calls must be in flight at the same time", "200,200,200", result);
		});
	}

	@Test
	public void testSynchronousCallsDoNotRunConcurrently() {

		// The control for the test above. If this passed too, the rendezvous server would be proving
		// nothing -- it has to be able to tell the two apart.
		withRendezvousServer(2, 3, port -> {

			final String url = "'http://localhost:" + port + "/'";

			final Object result = wrapped(
				  "const a = $.GET(" + url + ");"
				+ "const b = $.GET(" + url + ");"
				+ "return a.status + ',' + b.status;");

			assertEquals("two synchronous calls cannot overlap, so the first times out and breaks the barrier for the second", "500,500", result);
		});
	}

	@Test
	public void testAsyncCallReadsTheHeadersItWasStartedWith() {

		withRendezvousServer(2, 20, port -> {

			final String slow    = "'http://localhost:" + port + "/slow'";
			final String release = "'http://localhost:" + port + "/release'";

			final Object result = wrapped(
				  "$.addHeader('X-Early', 'yes');"
				+ "const p = $.GET.async(" + slow + ");"
				+ "$.addHeader('X-Late', 'yes');"          // after the call was started: must not reach it
				+ "const r2 = $.GET(" + release + ");"      // lets the rendezvous complete
				+ "const r1 = await p;"
				+ "return r1.status + ',' + r2.status;");

			assertEquals("both requests must complete", "200,200", result);

			final Map<String, String> slowHeaders = headersByPath.get("/slow");

			assertTrue("the async request must carry a header set before it was started", slowHeaders.containsKey("x-early"));
			assertFalse("the async request must not carry a header set after it was started", slowHeaders.containsKey("x-late"));

			// the synchronous call that came later does see it, which is what makes the line above a
			// statement about the snapshot rather than about the header never being set
			assertTrue("the later synchronous request must carry the later header", headersByPath.get("/release").containsKey("x-late"));
		});
	}

	@Test
	public void testPromiseAllAnswersInArgumentOrder() {

		withRendezvousServer(3, 20, port -> {

			final String base = "'http://localhost:" + port;

			final Object result = wrapped(
				  "const a = $.GET.async(" + base + "/a');"
				+ "const b = $.GET.async(" + base + "/b');"
				+ "const c = $.GET.async(" + base + "/c');"
				+ "const r = await Promise.all([c, a, b]);"
				+ "return r.map(x => x.body).join(',');");

			assertEquals("Promise.all must answer in argument order, not completion order", "/c,/a,/b", result);
		});
	}

	@Test
	public void testFailureIsReportedExactlyAsTheSynchronousCallReportsIt() {

		try {

			// port 1 is not listening, so both forms fail in the same place for the same reason; what is
			// asserted is that they are indistinguishable, not what either of them says
			final Object result = wrapped(
				  "const dead = 'http://localhost:1/';"
				+ "let s, a;"
				+ "try { const r = $.GET(dead); s = 'ok:' + (r === null ? 'null' : r.status); } catch (e) { s = 'err:' + e; }"
				+ "try { const r = await $.GET.async(dead); a = 'ok:' + (r === null ? 'null' : r.status); } catch (e) { a = 'err:' + e; }"
				+ "return s + '||' + a;");

			final String[] outcomes = result.toString().split("\\|\\|");

			assertEquals("the two calls must produce one outcome each", 2, outcomes.length);
			assertEquals("an async failure must be indistinguishable from the synchronous one", outcomes[0], outcomes[1]);

		} catch (final FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());
		}
	}

	@Test
	public void testPendingCallReturnedWithoutAwaitIsStillResolved() {

		withRendezvousServer(1, 20, port -> {

			// An unwrapped snippet hands its completion value to PolyglotWrapper.unwrapThenable, which calls
			// then() once and treats "did not settle during that call" as an error, because there is no event
			// loop to settle it afterwards. The pending call joins inside then(), so it settles in time --
			// returning one without awaiting it is answered, not rejected.
			final Object result = unwrapped("$.GET.async('http://localhost:" + port + "/x')");

			assertEquals("a pending call returned without await must still be resolved", "/x",
				((org.structr.core.GraphObjectMap) result).toMap().get("body"));
		});
	}

	@Test
	public void testPendingCallReturnedWithoutAwaitReportsItsFailure() {

		try {

			// The same path, failing. unwrapThenable's caller re-raises only ThenableFailure and swallows
			// anything else into null, so a rejection that does not arrive through the reject callback would
			// silently answer null instead of reporting anything at all.
			//
			// The trigger is a bad URL, not an unreachable host: since the 7.0 outbound HTTP revision a call
			// that never reached the server resolves with status 0 rather than rejecting, so an unreachable
			// host no longer exercises this path at all. A wrong scheme is refused by
			// checkAddressAgainstWhitelist inside the call, on the async thread, which is the rejection this
			// test is about.
			unwrapped("$.GET.async('ftp://example.com/')");

			fail("a failing pending call returned without await must report the failure, not answer null");

		} catch (final FrameworkException expected) {

			assertEquals("the failure must keep the status of the refusal", 400, expected.getStatus());
		}
	}

	@Test
	public void testPendingCallToAnUnreachableHostResolvesWithStatusZero() {

		try {

			// The counterpart of the test above, pinning the other half of the 7.0 contract: a transport
			// failure is data, not an error. The async path must agree with the synchronous one, which
			// OutboundHttpApiTest asserts returns status 0 rather than throwing.
			final Object result = unwrapped("$.GET.async('http://localhost:1/')");

			assertNotNull("an unreachable host must resolve, not answer null", result);
			assertEquals("no response means status 0", "0",
				((org.structr.core.GraphObjectMap) result).toMap().get("status").toString());

		} catch (final FrameworkException fex) {

			fex.printStackTrace();
			fail("an unreachable host must resolve rather than throw: " + fex.getMessage());
		}
	}

	@Test
	public void testItIsAThenableAndNotAPromise() {

		withRendezvousServer(1, 20, port -> {

			final String url = "'http://localhost:" + port + "/x'";

			// Pinned because the seven functions' own notes say all three of these. If the pending result
			// ever becomes a real promise, this test fails and the documentation gets corrected with it.
			assertEquals("chaining directly off the pending result is not available", "undefined",
				wrapped("return typeof $.GET.async(" + url + ").catch;"));

			assertEquals("the documented chaining form works", "function",
				wrapped("return typeof Promise.resolve($.GET.async(" + url + ")).catch;"));

			assertEquals("await is unaffected", 200,
				wrapped("const r = await $.GET.async(" + url + "); return r.status;"));
		});
	}

	@Test
	public void testPromiseRaceSettlesInArgumentOrder() {

		withRendezvousServer(2, 20, port -> {

			// then() joins, so a race settles in iteration order rather than by which call finished first.
			// This is a limitation, not a feature -- it is pinned so that the note saying so on each of the
			// seven functions cannot quietly stop being true.
			final Object result = wrapped(
				  "const a = $.GET.async('http://localhost:" + port + "/first');"
				+ "const b = $.GET.async('http://localhost:" + port + "/second');"
				+ "const r = await Promise.race([a, b]);"
				+ "return r.body;");

			assertEquals("race answers the first argument, not the first to complete", "/first", result);
		});
	}

	@Test
	public void testPendingCallReturnedWithoutAwaitFromAWrappedSnippetIsResolvedToo() {

		withRendezvousServer(1, 20, port -> {

			// The other returned-without-await path. In a wrapped snippet the value goes to the async
			// arrow's own promise, which adopts the thenable rather than fulfilling with it, so the caller
			// must still see the response and not the handle. Same reachable mistake as the unwrapped case,
			// different machinery underneath -- worth its own test.
			final Object result = wrapped("return $.GET.async('http://localhost:" + port + "/w');");

			assertEquals("what the caller gets must be the response, not the pending call",
				"/w", ((org.structr.core.GraphObjectMap) result).toMap().get("body"));

			assertEquals("its status must be readable as usual", 200,
				((org.structr.core.GraphObjectMap) result).toMap().get("status"));
		});
	}

	@Test
	public void testTheAsyncVariantAnswersTheSameWrappedAndUnwrapped() {

		// Whether a snippet is wrapped is a stylistic choice Structr makes for the caller, so the async
		// variant must not behave differently across it. The only difference between the two dialects is
		// how the answer is given -- a return in the wrapped form, the completion value in the unwrapped one.

		// a barrier of one so a single call completes on its own rather than waiting for a peer
		withRendezvousServer(1, 20, port -> {

			final String a = "'http://localhost:" + port + "/a'";

			final String fromUnwrapped = unwrapped("$.GET.async(" + a + ")").toString();

			assertTrue("the comparison is only meaningful if the call actually succeeded", fromUnwrapped.contains("status=200"));
			assertEquals("a single pending call must answer the same either way",
				fromUnwrapped, wrapped("return $.GET.async(" + a + ");").toString());
		});

		// and the concurrent case, which is the one that matters: Promise.all is itself a thenable, so an
		// unwrapped snippet gets the full benefit without needing the await keyword at all
		withRendezvousServer(2, 20, port -> {

			final String pair = "Promise.all([$.GET.async('http://localhost:" + port + "/a'), $.GET.async('http://localhost:" + port + "/b')])";

			final String fromUnwrapped = unwrapped(pair).toString();

			assertTrue("the comparison is only meaningful if both calls actually succeeded",
				fromUnwrapped.contains("body=/a") && fromUnwrapped.contains("body=/b") && !fromUnwrapped.contains("status=500"));

			assertEquals("Promise.all of several calls must answer the same either way",
				fromUnwrapped, wrapped("return " + pair + ";").toString());
		});

		// NOT asserted here, deliberately: top-level `await` in an *unwrapped* snippet answers null, because
		// a module that uses it returns the module evaluation promise instead of its completion value. That
		// asymmetry predates this feature, applies to every await rather than to these functions, and is
		// recorded in docs/gotchas.md. Pinning it would be pinning a defect.
	}

	@Test
	public void testAnUnwrappedSnippetCanAwaitInsideAnAsyncFunction() {

		// Top-level await costs an unwrapped snippet its result, because a module that uses one answers the
		// module evaluation promise instead of a completion value -- see docs/gotchas.md. Inside an async
		// function there is no top-level await, so the module keeps its completion value, and that value is
		// a promise the host settles. This is the idiom that gives an unwrapped snippet the full feature,
		// and it is what the wrapped form does on the caller's behalf.

		// a barrier of one, because this makes a single call
		withRendezvousServer(1, 20, port -> {

			assertEquals("an unwrapped snippet must be able to await a single call inside an async function",
				200, unwrapped("(async () => { const r = await $.GET.async('http://localhost:" + port + "/a'); return r.status; })()"));
		});

		// a barrier of two, because this makes two and they must overlap
		withRendezvousServer(2, 20, port -> {

			assertEquals("and to await several of them concurrently", "/a,/b",
				unwrapped("(async () => { const r = await Promise.all(["
					+ "$.GET.async('http://localhost:" + port + "/a'), "
					+ "$.GET.async('http://localhost:" + port + "/b')"
					+ "]); return r.map(x => x.body).join(','); })()"));
		});
	}
}
