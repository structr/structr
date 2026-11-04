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
package org.structr.test.rest.service;

import org.eclipse.jetty.server.ForwardedRequestCustomizer;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.RequestLog;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.structr.rest.service.RequestLogFormat;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/** Each request log format, written by Jetty for real requests, field by field. */
public class RequestLogFormatTest {

	private static final String GOOGLEBOT = "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)";
	private static final String REFERER   = "http://www.example.com/";
	private static final String BODY      = "hello";
	private static final String TIME      = "\\[\\d{2}/[A-Z][a-z]{2}/\\d{4}:\\d{2}:\\d{2}:\\d{2} \\+0000\\]";

	private final List<String> lines = new CopyOnWriteArrayList<>();
	private final LineWriter writer  = new LineWriter();
	private Server server            = null;

	@AfterMethod
	public void stopServer() throws Exception {

		if (server != null) {

			server.stop();
			server = null;
		}

		lines.clear();
	}

	@Test
	public void testUltimateIsApacheCombinedWithTheTimeTaken() throws Exception {

		final Matcher line = match(RequestLogFormat.ULTIMATE, get("/contact.html?a=b", true),
			"^66\\.249\\.66\\.1 - - " + TIME + " \"GET /contact\\.html\\?a=b HTTP/1\\.1\" 200 5 (\\d+) \"" + Pattern.quote(REFERER) + "\" \"" + Pattern.quote(GOOGLEBOT) + "\"$");

		// the handler takes at least 50 ms, logged in microseconds
		assertTrue("time taken in microseconds: " + line.group(1), Long.parseLong(line.group(1)) >= 50_000);
	}

	@Test
	public void testApacheIsApacheCombined() throws Exception {

		match(RequestLogFormat.APACHE, get("/contact.html", true),
			"^66\\.249\\.66\\.1 - - " + TIME + " \"GET /contact\\.html HTTP/1\\.1\" 200 5 \"" + Pattern.quote(REFERER) + "\" \"" + Pattern.quote(GOOGLEBOT) + "\"$");
	}

	@Test
	public void testLegacyIsUnchanged() throws Exception {

		// the date logback used to put in front, followed by what Jetty wrote
		match(RequestLogFormat.LEGACY, get("/contact.html", true),
			"^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2},\\d{3}" + TIME + " \"GET /contact\\.html HTTP/1\\.1\" 200 \\d+$");
	}

	@Test
	public void testMissingValuesAreDashes() throws Exception {

		// no referer, an empty body and no forwarded address: the connection's own address
		match(RequestLogFormat.ULTIMATE, get("/empty", false),
			"^127\\.0\\.0\\.1 - - " + TIME + " \"GET /empty HTTP/1\\.1\" 404 - \\d+ \"-\" \"" + Pattern.quote(GOOGLEBOT) + "\"$");
	}

	@Test
	public void testQuotesAndControlCharactersAreEscaped() throws Exception {

		// a quote in a header value would otherwise end the field early and shift every field after it
		start(RequestLogFormat.ULTIMATE);

		final HttpRequest request = HttpRequest.newBuilder(uri("/contact.html"))
			.version(HttpClient.Version.HTTP_1_1)
			.header("User-Agent", "evil \"agent\" \\ end")
			.header("Referer", "x\tz")
			.build();

		HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.discarding());

		final String line = awaitLine();

		assertTrue(line, line.endsWith(" \"x\\tz\" \"evil \\\"agent\\\" \\\\ end\""));
	}

	@Test
	public void testFormatNames() {

		assertEquals(RequestLogFormat.ULTIMATE, RequestLogFormat.of("ultimate"));
		assertEquals(RequestLogFormat.APACHE, RequestLogFormat.of("Apache"));
		assertEquals(RequestLogFormat.LEGACY, RequestLogFormat.of("legacy"));
		assertEquals("an unknown name falls back to the default", RequestLogFormat.ULTIMATE, RequestLogFormat.of("w3c"));
	}

	// ----- private methods -----
	private Matcher match(final RequestLogFormat format, final Requester requester, final String regex) throws Exception {

		start(format);
		requester.send();

		final String line     = awaitLine();
		final Matcher matcher = Pattern.compile(regex).matcher(line);

		if (!matcher.matches()) {

			fail(format.getName() + " line\n" + line + "\ndoes not match\n" + regex);
		}

		return matcher;
	}

	private String awaitLine() throws InterruptedException {

		final long deadline = System.currentTimeMillis() + 5_000;

		// the line is written after the response has been sent
		while (lines.isEmpty() && System.currentTimeMillis() < deadline) {

			Thread.sleep(10);
		}

		assertEquals("one line per request: " + lines, 1, lines.size());

		return lines.get(0);
	}

	private URI uri(final String path) {

		return URI.create("http://127.0.0.1:" + ((ServerConnector) server.getConnectors()[0]).getLocalPort() + path);
	}

	private Requester get(final String path, final boolean full) {

		return () -> {

			final HttpRequest.Builder builder = HttpRequest.newBuilder(uri(path))
				.version(HttpClient.Version.HTTP_1_1)
				.header("User-Agent", GOOGLEBOT);

			if (full) {

				builder.header("Referer", REFERER).header("X-Forwarded-For", "66.249.66.1");
			}

			HttpClient.newHttpClient().send(builder.build(), HttpResponse.BodyHandlers.discarding());
		};
	}

	private void start(final RequestLogFormat format) throws Exception {

		server = new Server();

		// the client address from X-Forwarded-For, configured as HttpService does with forwarded-for handling enabled
		final ForwardedRequestCustomizer forwarded = new ForwardedRequestCustomizer();
		forwarded.setForwardedHeader(null);

		final HttpConfiguration config = new HttpConfiguration();
		config.addCustomizer(forwarded);

		final ServerConnector connector = new ServerConnector(server, new HttpConnectionFactory(config));
		connector.setHost("127.0.0.1");
		connector.setPort(0);

		server.addConnector(connector);
		server.setRequestLog(format.createRequestLog(writer));
		server.setHandler(new Handler.Abstract() {

			@Override
			public boolean handle(final Request request, final Response response, final Callback callback) throws Exception {

				if ("/empty".equals(Request.getPathInContext(request))) {

					response.setStatus(404);
					callback.succeeded();

					return true;
				}

				Thread.sleep(50);

				response.setStatus(200);
				response.write(true, ByteBuffer.wrap(BODY.getBytes(StandardCharsets.UTF_8)), callback);

				return true;
			}
		});

		server.start();
	}

	/** Like Jetty's slf4j writer, which only has its logger once it has been started with the server. */
	private class LineWriter extends AbstractLifeCycle implements RequestLog.Writer {

		@Override
		public void write(final String line) {

			if (!isStarted()) {

				throw new IllegalStateException("request log writer written to before it was started");
			}

			lines.add(line);
		}
	}

	private interface Requester {

		void send() throws Exception;
	}
}
