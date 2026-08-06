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

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * Regression guard for the SSRF check in {@link HttpHelper#streamURLToFile}.
 *
 * {@link HttpHelper#validateUrl} must reject outbound requests to
 * loopback and other internal IP ranges under the default settings
 * ({@code SsrfProtection=true}, {@code OutgoingURLWhitelist="*"}), and
 * {@code streamURLToFile} must route through it so callers such as
 * DeploymentServlet inherit the check automatically.
 */
public class HttpHelperSsrfTest {

	private HttpServer loopbackServer;
	private int loopbackPort;
	private String previousWhitelist;
	private boolean previousSsrfProtection;
	private File tempOut;

	@BeforeMethod
	public void setUp() throws IOException {

		// Capture settings so we restore them after the test.
		previousWhitelist      = Settings.OutgoingURLWhitelist.getValue();
		previousSsrfProtection = Settings.SsrfProtection.getValue();

		// Use the documented defaults so the test reflects the default
		// deployment posture, not a test-only override.
		Settings.OutgoingURLWhitelist.setValue("*");
		Settings.SsrfProtection.setValue(true);

		// Start a tiny loopback-only HTTP server on a random port.
		loopbackServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		loopbackServer.createContext("/internal", exchange -> {
			final byte[] body = "INTERNAL-SECRET".getBytes(StandardCharsets.UTF_8);
			exchange.sendResponseHeaders(200, body.length);
			exchange.getResponseBody().write(body);
			exchange.close();
		});
		loopbackServer.start();
		loopbackPort = loopbackServer.getAddress().getPort();

		tempOut = File.createTempFile("structr-ssrf-test-", ".bin");
	}

	@AfterMethod
	public void tearDown() {

		if (loopbackServer != null) {

			loopbackServer.stop(0);
		}

		if (tempOut != null && tempOut.exists()) {

			tempOut.delete();
		}

		Settings.OutgoingURLWhitelist.setValue(previousWhitelist);
		Settings.SsrfProtection.setValue(previousSsrfProtection);
	}

	/**
	 * {@link HttpHelper#streamURLToFile} must reject loopback targets
	 * before any network call — the same contract
	 * {@link HttpHelper#validateUrl} enforces for {@link HttpHelper#fetch}.
	 *
	 * If this test fails, {@code streamURLToFile} has lost its
	 * {@code validateUrl()} call and the SSRF hole it closes has come back.
	 */
	@Test
	public void streamURLToFile_shouldRejectLoopbackWithDefaultSettings() throws IOException {

		final String loopbackUrl = "http://127.0.0.1:" + loopbackPort + "/internal";

		try {

			HttpHelper.streamURLToFile(loopbackUrl, tempOut);

			final byte[] written = Files.readAllBytes(tempOut.toPath());
			fail("Expected FrameworkException(403) blocking the loopback URL, "
				+ "but the request succeeded and wrote "
				+ written.length + " bytes: "
				+ new String(written, StandardCharsets.UTF_8));

		} catch (FrameworkException fex) {

			assertEquals("Expected 403 SSRF block, got status " + fex.getStatus() + ": " + fex.getMessage(), 403, fex.getStatus());
			assertTrue("Expected SSRF block message, got: " + fex.getMessage(), fex.getMessage() != null && fex.getMessage().contains("internal network addresses"));
		}
	}
}
