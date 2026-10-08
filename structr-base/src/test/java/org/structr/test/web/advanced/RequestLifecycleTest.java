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

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.read.ListAppender;
import io.restassured.RestAssured;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.LoggerFactory;
import org.structr.api.schema.JsonSchema;
import org.structr.api.schema.JsonType;
import org.structr.common.ExchangeBoundRequest;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.entity.Principal;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.schema.export.StructrSchema;
import org.structr.test.web.StructrUiTest;
import org.structr.web.auth.UiAuthenticator;
import org.structr.web.entity.dom.Content;
import org.structr.web.entity.dom.Page;
import org.structr.web.entity.dom.Template;
import org.testng.annotations.Test;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.fail;

/**
 * Scripts that run after the HTTP exchange that started them has completed must still see that
 * exchange's request data, and only that exchange's.
 *
 * Jetty recycles a request when its exchange completes and reuses the underlying channel for the next
 * request on the same connection. A request object read after that point either throws a
 * NullPointerException from inside Jetty or, once the next request has arrived, answers with the next
 * request's headers. Each test runs a script past the end of its own exchange and records what
 * $.getRequestHeader returns there.
 */
public class RequestLifecycleTest extends StructrUiTest {

	private static final String READ_HEADER_INTO_STORE =
		"let v; try { v = $.getRequestHeader('X-Test'); } catch (e) { v = 'error: ' + e; } $.applicationStorePut('%s', v);";

	@Test
	public void testScheduledFunctionReadsTheHeaderOfItsOwnRequest() {

		setupScheduleSchema();

		final String value = uniqueValue();

		RestAssured.basePath = "/structr/rest";

		RestAssured
			.given()
			.contentType("application/json; charset=UTF-8")
			.header("X-User", "A")
			.header("X-Password", "test")
			.header("X-Test", value)
			.expect()
			.statusCode(200)
			.when()
			.post("/RequestTest/scheduleHeaderRead");

		assertEquals("A scheduled function must see the header of the request that scheduled it", value, awaitStoreValue("header"));
	}

	@Test
	public void testScheduledFunctionReadsTheHeaderOfItsOwnRequestAsSuperuser() {

		setupScheduleSchema();

		final String value = uniqueValue();

		RestAssured.basePath = "/structr/rest";

		RestAssured
			.given()
			.contentType("application/json; charset=UTF-8")
			.header("X-User", "superadmin")
			.header("X-Password", "sehrgeheim")
			.header("X-Test", value)
			.expect()
			.statusCode(200)
			.when()
			.post("/RequestTest/scheduleHeaderRead");

		assertEquals("A scheduled function must see the header of the request that scheduled it", value, awaitStoreValue("header"));
	}

	@Test
	public void testScheduledFunctionDoesNotSeeTheNextRequestOnTheSameConnection() {

		setupScheduleSchema();

		final String firstValue  = uniqueValue();
		final String secondValue = uniqueValue();

		// one client, HTTP/1.1: the second request goes out on the connection the first one used
		final HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).connectTimeout(Duration.ofSeconds(10)).build();

		try {

			final HttpResponse<String> first = client.send(post("/structr/rest/RequestTest/scheduleHeaderRead", firstValue), HttpResponse.BodyHandlers.ofString());
			assertEquals(200, first.statusCode());

			// still being handled when the scheduled function reads its header
			final CompletableFuture<HttpResponse<String>> second = client.sendAsync(post("/structr/rest/RequestTest/slow", secondValue), HttpResponse.BodyHandlers.ofString());

			assertEquals("A scheduled function must not see the headers of a later request on the same connection", firstValue, awaitStoreValue("header"));
			assertEquals(200, second.get().statusCode());

		} catch (Exception ex) {

			logger.warn("", ex);
			fail("Unexpected exception: " + ex.getMessage());
		}
	}

	@Test
	public void testLifecycleMethodOfAnAsyncRenderReadsTheHeaderOfItsOwnRequest() {

		try (final Tx tx = app.tx()) {

			createAdminUser();

			final JsonSchema schema = StructrSchema.createFromDatabase(app);
			final JsonType type     = schema.addType("RenderCreated");

			// runs when the render thread's transaction closes, after the page output has been written
			type.addMethod("afterCreate", "{ $.sleep(300); " + String.format(READ_HEADER_INTO_STORE, "render") + " }");

			StructrSchema.replaceDatabaseSchema(app, schema);

			final Page page         = app.create(StructrTraits.PAGE, new NodeAttribute<>(Traits.of(StructrTraits.PAGE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "test"), new NodeAttribute<>(Traits.of(StructrTraits.PAGE).key(GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY), true)).as(Page.class);
			final Template template = app.create(StructrTraits.TEMPLATE, new NodeAttribute<>(Traits.of(StructrTraits.PAGE).key(GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY), true)).as(Template.class);

			template.setContent("${{ $.create('RenderCreated', { name: 'rendered' }); $.print('ok'); }}");
			page.appendChild(template);

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception: " + fex.getMessage());
		}

		clearStore();

		final String value = uniqueValue();

		RestAssured.basePath = "/";

		RestAssured
			.given()
			.header(X_USER_HEADER, ADMIN_USERNAME)
			.header(X_PASSWORD_HEADER, ADMIN_PASSWORD)
			.header("X-Test", value)
			.expect()
			.statusCode(200)
			.when()
			.get("/test");

		// read without waiting: the response of an async render completes only after its transaction has
		// committed, lifecycle methods included, just as a synchronous render does
		assertEquals("A lifecycle method run by an async render must finish, with the header of the page request, before the response completes",
			value, Services.getInstance().getApplicationStore().get("render"));
	}

	/**
	 * Ticket 1301: a client that abandoned an asynchronously rendered page made the write listener complete
	 * the exchange at once, while the render thread was still running. A script that failed after that point
	 * had its error reported with the session id of a recycled request, and Jetty answered getSession with a
	 * NullPointerException, which replaced the script's own error in the log.
	 */
	@Test
	public void testScriptErrorOfAnAbandonedAsyncRenderIsReportedAsItself() {

		final String value = uniqueValue();

		try (final Tx tx = app.tx()) {

			final Page page        = app.create(StructrTraits.PAGE, new NodeAttribute<>(Traits.of(StructrTraits.PAGE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "abandoned"), new NodeAttribute<>(Traits.of(StructrTraits.PAGE).key(GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY), true)).as(Page.class);
			final Template output  = app.create(StructrTraits.TEMPLATE, new NodeAttribute<>(Traits.of(StructrTraits.PAGE).key(GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY), true)).as(Template.class);
			final Template more    = app.create(StructrTraits.TEMPLATE, new NodeAttribute<>(Traits.of(StructrTraits.PAGE).key(GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY), true)).as(Template.class);
			final Template failing = app.create(StructrTraits.TEMPLATE, new NodeAttribute<>(Traits.of(StructrTraits.PAGE).key(GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY), true)).as(Template.class);

			// the beginning the client reads before leaving
			output.setContent("${{ $.print('x'.repeat(1024 * 1024)); }}");

			// more than the socket buffers hold while the client does not read, so the write listener waits
			// for a write that fails when the client leaves
			more.setContent("${{ $.sleep(200); $.print('y'.repeat(16 * 1024 * 1024)); }}");

			// fails after the client has left
			failing.setContent("${{ $.sleep(2000); throw new Error('" + value + "'); }}");

			page.appendChild(output);
			page.appendChild(more);
			page.appendChild(failing);

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception: " + fex.getMessage());
		}

		final Logger contentLogger                 = (Logger) LoggerFactory.getLogger(Content.class);
		final ListAppender<ILoggingEvent> appender = new ListAppender<>();

		appender.start();
		contentLogger.addAppender(appender);

		try {

			abandon("/abandoned");

			final ILoggingEvent event = awaitScriptingError(appender, value);

			assertNotNull("The script error of the abandoned render was not reported as itself", event);

		} finally {

			contentLogger.detachAppender(appender);
			appender.stop();
		}
	}

	@Test
	public void testExchangeBoundRequestOverridesEveryRequestMethod() {

		// an inherited wrapper method would read the recycled request unconditionally
		for (final Method method : HttpServletRequest.class.getMethods()) {

			if (Modifier.isStatic(method.getModifiers())) {
				continue;
			}

			try {

				ExchangeBoundRequest.class.getDeclaredMethod(method.getName(), method.getParameterTypes());

			} catch (NoSuchMethodException ex) {

				fail("ExchangeBoundRequest does not override " + method);
			}
		}
	}

	// ----- private methods -----
	private void setupScheduleSchema() {

		try (final Tx tx = app.tx()) {

			final Principal principal = app.create(StructrTraits.USER, "A").as(Principal.class);
			principal.setPassword("test");

			final JsonSchema schema = StructrSchema.createFromDatabase(app);
			final JsonType type     = schema.addType("RequestTest");

			// returns at once; the scheduled function reads the header after the response has been sent
			type.addMethod("scheduleHeaderRead", "{ $.schedule(() => { $.sleep(500); " + String.format(READ_HEADER_INTO_STORE, "header") + " }); }").setIsStatic(true);
			type.addMethod("slow", "{ $.sleep(1500); }").setIsStatic(true);

			StructrSchema.replaceDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception: " + fex.getMessage());
		}

		RestAssured.basePath = "/structr/rest";

		grant("RequestTest/scheduleHeaderRead", UiAuthenticator.AUTH_USER_POST, false);
		grant("RequestTest/slow", UiAuthenticator.AUTH_USER_POST, false);

		clearStore();
	}

	private HttpRequest post(final String path, final String headerValue) {

		return HttpRequest.newBuilder(URI.create("http://localhost:" + httpPort + path))
			.header("Content-Type", "application/json; charset=UTF-8")
			.header("X-User", "A")
			.header("X-Password", "test")
			.header("X-Test", headerValue)
			.POST(HttpRequest.BodyPublishers.ofString("{}"))
			.build();
	}

	/**
	 * A header value no other attempt can produce. Lifecycle methods and scheduled functions of an earlier
	 * attempt may still be running when a retry starts, and a fixed value would let a retry pass on what
	 * the attempt before it wrote.
	 */
	private String uniqueValue() {
		return UUID.randomUUID().toString();
	}

	private void clearStore() {

		Services.getInstance().getApplicationStore().remove("header");
		Services.getInstance().getApplicationStore().remove("render");
	}

	/**
	 * Requests a page, reads the beginning of the response, stops reading and then drops the connection,
	 * like a browser navigating away from a page that is still loading.
	 */
	private void abandon(final String path) {

		try (final Socket socket = new Socket(host, httpPort)) {

			final OutputStream out = socket.getOutputStream();
			final InputStream in   = socket.getInputStream();
			final byte[] buffer    = new byte[8192];
			int received           = 0;

			out.write(("GET " + path + " HTTP/1.1\r\nHost: " + host + "\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
			out.flush();

			while (received < 65536) {

				final int count = in.read(buffer);
				if (count < 0) {

					fail("The response ended before the page had been abandoned");
				}

				received += count;
			}

			// not reading lets the output that follows fill the socket buffers
			Thread.sleep(1000);

			// reset instead of an orderly close, so that the pending write fails at once
			socket.setSoLinger(true, 0);

		} catch (Exception ex) {

			logger.warn("", ex);
			fail("Unexpected exception: " + ex.getMessage());
		}
	}

	/**
	 * The error the render logs for a failed script, once it carries the script's message. An error that
	 * failed on the way, e.g. with an exception from the request, does not count.
	 */
	private ILoggingEvent awaitScriptingError(final ListAppender<ILoggingEvent> appender, final String message) {

		final long deadline = System.currentTimeMillis() + 10_000;

		while (System.currentTimeMillis() < deadline) {

			for (final ILoggingEvent event : List.copyOf(appender.list)) {

				for (IThrowableProxy proxy = event.getThrowableProxy(); proxy != null; proxy = proxy.getCause()) {

					if (NullPointerException.class.getName().equals(proxy.getClassName())) {

						fail("Reporting the script error failed with a NullPointerException: " + proxy.getMessage());
					}

					if (proxy.getMessage() != null && proxy.getMessage().contains(message)) {

						return event;
					}
				}
			}

			try { Thread.sleep(50); } catch (InterruptedException ignore) {}
		}

		return null;
	}

	private Object awaitStoreValue(final String key) {

		final long deadline = System.currentTimeMillis() + 10_000;

		while (System.currentTimeMillis() < deadline) {

			final Object value = Services.getInstance().getApplicationStore().get(key);
			if (value != null) {

				return value;
			}

			try { Thread.sleep(50); } catch (InterruptedException ignore) {}
		}

		return null;
	}
}
