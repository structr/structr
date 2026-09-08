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

import org.structr.common.error.FrameworkException;
import org.structr.rest.common.HttpHelper;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.jose4j.lang.ByteUtil.concat;
import static org.testng.AssertJUnit.*;

/**
 * Test for {@link HttpHelper}'s get() Method
 */
public class HttpHelperGetTest extends HttpHelperTestBase{

	@Test
	public void getAddressOnly() {

		registerContext("/internal", 200, "TESTING CONTEXT".getBytes(StandardCharsets.UTF_8), null);

		final String loopbackUrl = urlForPath("/internal");

		try {

			Map<String, Object> returnData =  HttpHelper.get(loopbackUrl);
			Object body = returnData.get(HttpHelper.FIELD_BODY);
			Object statusCode = returnData.get(HttpHelper.FIELD_STATUS);

			assertEquals("Expected the return string body, got: " + body.toString(), "TESTING CONTEXT", body.toString());
			assertEquals("Expected status code 200, got " + statusCode.toString(), "200", statusCode);

		} catch (FrameworkException fex) {
			fail("Caught a Framework Exception. Got status " + fex.getStatus() + ": " + fex.getMessage());
		}
	}

	@Test
	public void getWithCustomCharset() {

		registerContext("/internal", 200, "café".getBytes(StandardCharsets.ISO_8859_1), null);

		final String loopbackUrl = urlForPath("/internal");

		try {

			Map<String, Object> returnData =  HttpHelper.get(loopbackUrl, "ISO-8859-1");
			Object body = returnData.get(HttpHelper.FIELD_BODY);
			Object statusCode = returnData.get(HttpHelper.FIELD_STATUS);

			assertEquals("Expected the return string body, got: " + body.toString(), "café", body.toString());
			assertEquals("Expected status code 200, got " + statusCode.toString(), "200", statusCode);

		} catch (FrameworkException fex) {
			fail("Caught a Framework Exception. Got status " + fex.getStatus() + ": " + fex.getMessage());
		}
	}

	@Test
	public void getWithNullCharsetUsesContentTypeHeader() {

		final Map<String, String> responseHeaders = new HashMap<>();
		responseHeaders.put("Content-Type", "text/plain; charset=ISO-8859-1");


		registerContext("/internal", 200, "café".getBytes(StandardCharsets.ISO_8859_1), responseHeaders);

		final String loopbackUrl = urlForPath("/internal");


		try {

			Map<String, Object> returnData =  HttpHelper.get(loopbackUrl);
			Object body = returnData.get(HttpHelper.FIELD_BODY);
			Object statusCode = returnData.get(HttpHelper.FIELD_STATUS);

			assertEquals("Expected the return string body, got: " + body.toString(), "café", body.toString());
			assertEquals("Expected status code 200, got " + statusCode.toString(), "200", statusCode);

		} catch (FrameworkException fex) {
			fail("Caught a Framework Exception. Got status " + fex.getStatus() + ": " + fex.getMessage());
		}
	}

	@Test
	public void getStripsBOMWhenPresent() {

		final byte[] bom = new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };
		registerContext("/internal", 200, concat(bom, "TESTING CONTEXT".getBytes(StandardCharsets.UTF_8)), null);

		final String loopbackUrl = urlForPath("/internal");


		try {

			Map<String, Object> returnData =  HttpHelper.get(loopbackUrl, "UTF-8");
			Object body = returnData.get(HttpHelper.FIELD_BODY);
			Object statusCode = returnData.get(HttpHelper.FIELD_STATUS);

			assertEquals("Expected the return string body, got: " + body.toString(), "TESTING CONTEXT", body.toString());
			assertEquals("Expected status code 200, got " + statusCode.toString(), "200", statusCode);

		} catch (FrameworkException fex) {
			fail("Caught a Framework Exception. Got status " + fex.getStatus() + ": " + fex.getMessage());
		}
	}

	@Test
	public void get404DoesNotThrow() {

		registerContext("/internal", 404, "TESTING CONTEXT".getBytes(StandardCharsets.UTF_8), null);

		final String loopbackUrl = urlForPath("/internal");

		try {

			Map<String, Object> returnData =  HttpHelper.get(loopbackUrl);
			Object body = returnData.get(HttpHelper.FIELD_BODY);
			Object statusCode = returnData.get(HttpHelper.FIELD_STATUS);

			assertEquals("Expected the return string body, got: " + body.toString(), "TESTING CONTEXT", body.toString());
			assertEquals("Expected status code 404, got " + statusCode.toString(), "404", statusCode);

		} catch (FrameworkException fex) {
			fail("Caught a Framework Exception. Got status " + fex.getStatus() + ": " + fex.getMessage());
		}
	}

	@Test
	public void get500DoesNotThrow() {

		registerContext("/internal", 500, "TESTING CONTEXT".getBytes(StandardCharsets.UTF_8), null);

		final String loopbackUrl = urlForPath("/internal");

		try {

			Map<String, Object> returnData = HttpHelper.get(loopbackUrl);
			Object body = returnData.get(HttpHelper.FIELD_BODY);
			Object statusCode = returnData.get(HttpHelper.FIELD_STATUS);

			assertEquals("Expected the return string body, got: " + body.toString(), "TESTING CONTEXT", body.toString());
			assertEquals("Expected status code 500, got " + statusCode.toString(), "500", statusCode);

		} catch (FrameworkException fex) {
			fail("Caught a Framework Exception. Got status " + fex.getStatus() + ": " + fex.getMessage());
		}

	}

	@Test
	public void getSendsCustomRequestHeaders () {

		registerCapturingContext("/internal", 200, "TESTING CONTEXT".getBytes(StandardCharsets.UTF_8));

		final String loopbackUrl = urlForPath("/internal");
		final Map<String, String> requestHeaders = new HashMap<>();
		requestHeaders.put("X-test-header", "TEST VALUE");

		try {

			HttpHelper.get(loopbackUrl, "UTF-8", requestHeaders, true);

			final List<String> received = capturedHeaders.get("X-test-header");

			assertNotNull("Expected X-Test-Header to be captured. All captured headers: " + capturedHeaders.keySet(), received);
			assertEquals("Expected custom header to be sent", "TEST VALUE", received.get(0));

		} catch (FrameworkException fex) {
			fail("Caught a Framework Exception. Got status " + fex.getStatus() + ": " + fex.getMessage());
		}
	}

	@Test
	public void getSendsCookieHeader() {

		registerCapturingContext("/cookie", 200, "COOKIE CONTEXT".getBytes(StandardCharsets.UTF_8));
		final String loopbackUrl = urlForPath("/cookie");

		try {
			HttpHelper.get(loopbackUrl, "UTF-8", null, null, null, "test_session=12345", new HashMap<>(), true);

			final List<String> received = capturedHeaders.get("Cookie");

			assertNotNull("Expected Cookie header to be captured", received);
			assertEquals("Expected custom cookie to be sent", "test_session=12345", received.get(0));

		} catch (FrameworkException fex) {
			fail("Caught a Framework Exception. Got status " + fex.getStatus() + ": " + fex.getMessage());
		}
	}

	@Test
	public void getFollowsRedirectsWhenConfigured() {

		registerContext("/target", 200, "TARGET REACHED".getBytes(StandardCharsets.UTF_8), null);

		final Map<String, String> requestHeaders = new HashMap<>();
		requestHeaders.put("Location", urlForPath("/target"));
		registerContext("/redirect", 301, new byte[0], requestHeaders);

		final String loopbackUrl = urlForPath("/redirect");

		final Map<String, Object> config = new HashMap<>();
		config.put("redirects", true);

		try {

			Map<String, Object> returnData = HttpHelper.get(loopbackUrl, "UTF-8", null, null, null, null, null, null, new HashMap<>(), true, config);

			Object body = returnData.get(HttpHelper.FIELD_BODY);
			Object statusCode = returnData.get(HttpHelper.FIELD_STATUS);

			assertEquals("Expected status code 200 after successfully following the redirect", "200", statusCode);
			assertEquals("Expected the return string body from target", "TARGET REACHED", body.toString());

		} catch (FrameworkException fex) {
			fail("Caught a Framework Exception. Got status " + fex.getStatus() + ": " + fex.getMessage());
		}
	}

	@Test
	public void getIgnoresRedirectsWhenConfiguredFalse() {

		final Map<String, String> redirectHeaders = new HashMap<>();
		redirectHeaders.put("Location", urlForPath("/target"));
		registerContext("/no-redirect", 301, new byte[0], redirectHeaders);

		final String loopbackUrl = urlForPath("/no-redirect");

		final Map<String, Object> config = new HashMap<>();
		config.put("redirects", false);

		try {

			Map<String, Object> returnData = HttpHelper.get(loopbackUrl, "UTF-8", null, null, null, null, null, null, new HashMap<>(), true, config);

			Object statusCode = returnData.get(HttpHelper.FIELD_STATUS);

			assertEquals("Expected status code 301 because redirect was ignored", "301", statusCode);

		} catch (FrameworkException fex) {
			fail("Caught a Framework Exception. Got status " + fex.getStatus() + ": " + fex.getMessage());
		}
	}

}
