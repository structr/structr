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
package org.structr.test.web.basic;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.test.web.StructrUiTest;
import org.structr.web.common.FileHelper;
import org.testng.annotations.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * Ticket 1197: a page that loads many resources over HTTP/2 filled the log with "Exception while processing
 * request: reset" from HtmlServlet, each followed by Jetty's "IllegalStateException: Committed".
 *
 * <p>A browser that reloads such a page abandons the resource requests still in flight and sends one
 * RST_STREAM for each. More than the connector's frame rate allows (128 per second for h2c) and Jetty takes
 * that for a rapid reset attack and closes the whole session, resetting the streams the browser still
 * wanted as well. A stream Jetty resets itself fails the next write with a plain java.io.EOFException
 * ("reset"), not with the EofException a client reset produces, so it slipped past doGet's EofException
 * handler into the IOException one, which logged it and called sendError on a response whose 304 status
 * line was already out.
 *
 * <p>The test plays the reload: downloads of a large file, abandoned in a burst while still transferring,
 * while conditional requests for a small file keep arriving on the same connection.
 */
public class HttpStreamResetTest extends StructrUiTest {

	private static final int ROUNDS           = 10;
	private static final int WAVES            = 3;
	private static final int STREAMS_PER_WAVE = 100;

	@Test
	public void testAbandonedResourceRequestsDoNotProduceErrorResponses() {

		createPublicFile("resource.woff2", "font/woff2", 1024);
		createPublicFile("large.bin", "application/octet-stream", 1024 * 1024);

		final Logger root                          = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
		final ListAppender<ILoggingEvent> appender = new ListAppender<>();
		final AtomicInteger closedSessions         = new AtomicInteger();

		appender.start();
		root.addAppender(appender);

		try {

			for (int i = 0; i < ROUNDS; i++) {

				reload(closedSessions);
			}

			// Jetty logs the IllegalStateException after the servlet returns, on a thread of its own
			Thread.sleep(1000);

		} catch (InterruptedException iex) {

			fail("Interrupted");

		} finally {

			root.detachAppender(appender);
			appender.stop();
		}

		assertTrue("The burst of RST_STREAM frames never made Jetty close the HTTP/2 session, so the test did not reproduce the reload", closedSessions.get() > 0);

		for (final ILoggingEvent event : List.copyOf(appender.list)) {

			final String message = event.getFormattedMessage();
			if (message != null && message.startsWith("Exception while processing request")) {

				fail("HtmlServlet treated a reset stream as an error: " + message);
			}

			for (IThrowableProxy proxy = event.getThrowableProxy(); proxy != null; proxy = proxy.getCause()) {

				if (IllegalStateException.class.getName().equals(proxy.getClassName()) && "Committed".equals(proxy.getMessage())) {

					fail("An error response was sent on a committed response: " + message);
				}
			}
		}
	}

	/**
	 * One page reload on a fresh HTTP/2 connection.
	 */
	private void reload(final AtomicInteger closedSessions) throws InterruptedException {

		final HttpClient client      = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build();
		final String resource        = baseUri + "resource.woff2";
		final String large           = baseUri + "large.bin";
		final AtomicBoolean reloaded = new AtomicBoolean();

		// h2c starts as an HTTP/1.1 upgrade, so the first request sets up the HTTP/2 connection for all others
		try {

			final HttpResponse<Void> response = client.send(HttpRequest.newBuilder(URI.create(resource)).build(), HttpResponse.BodyHandlers.discarding());

			assertEquals("The connection was not upgraded to HTTP/2", HttpClient.Version.HTTP_2, response.version());

		} catch (Exception ex) {

			fail("Unable to open an HTTP/2 connection: " + ex.getMessage());
		}

		final Thread revalidation = Thread.ofPlatform().start(() -> revalidate(client, resource, reloaded, closedSessions));

		for (int wave = 0; wave < WAVES; wave++) {

			final List<CompletableFuture<HttpResponse<Void>>> downloads = startDownloads(client, large);

			Thread.sleep(20);

			// cancelling a download that is still transferring sends RST_STREAM(CANCEL), like a browser leaving the page
			downloads.forEach(download -> download.cancel(true));
		}

		reloaded.set(true);
		revalidation.join();

		client.shutdownNow();
	}

	/**
	 * Downloads of a file large enough to still be transferring when they are abandoned. The future completes
	 * only with the whole body, so cancelling it in between cancels the stream.
	 */
	private List<CompletableFuture<HttpResponse<Void>>> startDownloads(final HttpClient client, final String url) {

		final List<CompletableFuture<HttpResponse<Void>>> futures = new ArrayList<>();

		for (int i = 0; i < STREAMS_PER_WAVE; i++) {

			futures.add(client.sendAsync(HttpRequest.newBuilder(URI.create(url)).build(), HttpResponse.BodyHandlers.discarding()));
		}

		return futures;
	}

	/**
	 * The browser's conditional requests for resources it has cached, answered with 304 by streamFile.
	 */
	private void revalidate(final HttpClient client, final String url, final AtomicBoolean reloaded, final AtomicInteger closedSessions) {

		final String ifModifiedSince = DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(ZoneOffset.UTC).plusDays(1));
		final Semaphore inFlight     = new Semaphore(20);
		final AtomicBoolean closed   = new AtomicBoolean();

		while (!reloaded.get() && !closed.get()) {

			inFlight.acquireUninterruptibly();

			client.sendAsync(HttpRequest.newBuilder(URI.create(url)).header("If-Modified-Since", ifModifiedSince).build(), HttpResponse.BodyHandlers.discarding()).whenComplete((response, failure) -> {

				if (failure != null && closed.compareAndSet(false, true)) {

					closedSessions.incrementAndGet();
				}

				inFlight.release();
			});
		}
	}

	private void createPublicFile(final String name, final String contentType, final int size) {

		try (final Tx tx = app.tx()) {

			final NodeInterface file = FileHelper.createFile(securityContext, new byte[size], contentType, StructrTraits.FILE, name, false);

			file.setProperty(Traits.of(StructrTraits.FILE).key(GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY), true);

			tx.success();

		} catch (Exception ex) {

			ex.printStackTrace();
			fail("Unexpected exception creating the file: " + ex.getMessage());
		}
	}
}
