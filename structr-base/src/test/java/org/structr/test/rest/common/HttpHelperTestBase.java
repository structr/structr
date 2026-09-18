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
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class HttpHelperTestBase {

	protected HttpServer loopbackServer;
	protected int loopbackPort;
	protected final Map<String, List<String>> capturedHeaders = new ConcurrentHashMap<>();
	protected volatile byte[] capturedBody;
	protected volatile String capturedMethod;

	@BeforeMethod
	public void setUp() throws IOException {

		loopbackServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		loopbackServer.start();
		loopbackPort = loopbackServer.getAddress().getPort();

		capturedHeaders.clear();
	}

	@AfterMethod
	public void tearDown() {

		if (loopbackServer != null) {

			loopbackServer.stop(0);
		}
	}

	protected String urlForPath(final String path) {

		return "http://127.0.0.1:" + loopbackPort + path;
	}

	protected void registerContext(final String path, final int status, final byte[] body, final Map<String, String> responseHeaders) {

		loopbackServer.createContext(path, exchange -> {

			if (responseHeaders != null) {

				responseHeaders.forEach((k, v) -> exchange.getResponseHeaders().add(k, v));
			}

			exchange.sendResponseHeaders(status, body.length);
			exchange.getResponseBody().write(body);
			exchange.close();
		});
	}

	protected void registerCapturingContext(final String path, final int status, final byte[] body) {

		loopbackServer.createContext(path, exchange -> {

			capturedHeaders.putAll(exchange.getRequestHeaders());

			exchange.sendResponseHeaders(status, body.length);
			exchange.getResponseBody().write(body);
			exchange.close();
		});
	}

	protected void registerEchoContext(final String path, final int status) {

		loopbackServer.createContext(path, exchange -> {

			capturedHeaders.putAll(exchange.getRequestHeaders());
			final byte[] body = exchange.getRequestBody().readAllBytes();
			capturedBody = body;

			exchange.sendResponseHeaders(status, body.length);
			exchange.getResponseBody().write(body);
			exchange.close();
		});
	}

	protected void registerDelayedContext(final String path, final int status, final byte[] body, final int delayMillis) {

		loopbackServer.createContext(path, exchange -> {

			try {

				Thread.sleep(delayMillis);

			} catch (InterruptedException e) {

				Thread.currentThread().interrupt();
			}

			exchange.sendResponseHeaders(status, body.length);
			exchange.getResponseBody().write(body);
			exchange.close();
		});
	}

	protected void registerBodyCapturingContext(final String path, final int status, final byte[] responseBody) {

		loopbackServer.createContext(path, exchange -> {

			capturedHeaders.putAll(exchange.getRequestHeaders());
			capturedMethod = exchange.getRequestMethod();

			final byte[] requestBytes = exchange.getRequestBody().readAllBytes();
			capturedBody = requestBytes.length == 0 ? null : requestBytes;

			exchange.sendResponseHeaders(status, responseBody.length);
			exchange.getResponseBody().write(responseBody);
			exchange.close();
		});
	}

}
