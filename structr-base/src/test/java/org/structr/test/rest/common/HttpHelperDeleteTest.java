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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.jose4j.lang.ByteUtil.concat;
import static org.testng.AssertJUnit.*;

/**
 * Test for {@link HttpHelper}'s delete() Method
 */
public class HttpHelperDeleteTest extends HttpHelperTestBase {

	@Test
	public void deleteAddressOnly() {

		registerContext("/internal", 200, "DELETED".getBytes(StandardCharsets.UTF_8), null);

		final String loopbackUrl = urlForPath("/internal");

		try {

			final Map<String, Object> returnData = HttpHelper.delete(loopbackUrl);
			final Object body = returnData.get(HttpHelper.FIELD_BODY);
			final Object statusCode = returnData.get(HttpHelper.FIELD_STATUS);

			assertEquals("Expected the return string body, got: " + body, "DELETED", body.toString());
			assertEquals("Expected status code 200, got " + statusCode, "200", statusCode);

		} catch (FrameworkException fex) {
			fail("Caught a Framework Exception. Got status " + fex.getStatus() + ": " + fex.getMessage());
		}
	}

	@Test
	public void delete404DoesNotThrow() {

		registerContext("/internal", 404, "NOT FOUND".getBytes(StandardCharsets.UTF_8), null);

		final String loopbackUrl = urlForPath("/internal");

		try {

			final Map<String, Object> returnData = HttpHelper.delete(loopbackUrl);
			final Object statusCode = returnData.get(HttpHelper.FIELD_STATUS);

			assertEquals("Expected status code 404, got " + statusCode, "404", statusCode);

		} catch (FrameworkException fex) {
			fail("Caught a Framework Exception. Got status " + fex.getStatus() + ": " + fex.getMessage());
		}
	}

	@Test
	public void delete500DoesNotThrow() {

		registerContext("/internal", 500, "SERVER ERROR".getBytes(StandardCharsets.UTF_8), null);

		final String loopbackUrl = urlForPath("/internal");

		try {

			final Map<String, Object> returnData = HttpHelper.delete(loopbackUrl);
			final Object statusCode = returnData.get(HttpHelper.FIELD_STATUS);

			assertEquals("Expected status code 500, got " + statusCode, "500", statusCode);

		} catch (FrameworkException fex) {
			fail("Caught a Framework Exception. Got status " + fex.getStatus() + ": " + fex.getMessage());
		}
	}

	@Test
	public void deleteStripsBOMWhenPresent() {

		final byte[] bom = new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };

		registerContext("/internal", 200, concat(bom, "TESTING CONTEXT".getBytes(StandardCharsets.UTF_8)), null);

		final String loopbackUrl = urlForPath("/internal");

		try {

			final Map<String, Object> returnData = HttpHelper.delete(loopbackUrl, null, null, null, null, null,null, new HashMap<>(), true, null, null, null, "UTF-8");
			final Object body = returnData.get(HttpHelper.FIELD_BODY);

			assertEquals("Expected BOM to be stripped from body", "TESTING CONTEXT", body.toString());

		} catch (FrameworkException fex) {
			fail("Caught a Framework Exception. Got status " + fex.getStatus() + ": " + fex.getMessage());
		}
	}

	@Test
	public void deleteWithNullBodySendsNoEntity() {

		registerBodyCapturingContext("/internal", 200, "OK".getBytes(StandardCharsets.UTF_8));

		final String loopbackUrl = urlForPath("/internal");

		try {

			HttpHelper.delete(loopbackUrl, null, null, null, null, null, null, new HashMap<>(), true, null, null, null, null);

			assertEquals("Expected plain DELETE with no body to be sent", "DELETE", capturedMethod);
			assertNull("Expected no request body to have been sent", capturedBody);

		} catch (FrameworkException fex) {
			fail("Caught a Framework Exception. Got status " + fex.getStatus() + ": " + fex.getMessage());
		}
	}

	@Test
	public void deleteWithBlankStringBodySendsNoEntity() {

		registerBodyCapturingContext("/internal", 200, "OK".getBytes(StandardCharsets.UTF_8));

		final String loopbackUrl = urlForPath("/internal");

		try {

			HttpHelper.delete(loopbackUrl, null, null, null, null, null, null, new HashMap<>(), true, null, "   ", null, null);

			assertNull("Expected a blank string body to be treated as no body", capturedBody);

		} catch (FrameworkException fex) {
			fail("Caught a Framework Exception. Got status " + fex.getStatus() + ": " + fex.getMessage());
		}
	}

	@Test
	public void deleteWithEmptyByteArrayBodySendsNoEntity() {

		registerBodyCapturingContext("/internal", 200, "OK".getBytes(StandardCharsets.UTF_8));

		final String loopbackUrl = urlForPath("/internal");

		try {

			HttpHelper.delete(loopbackUrl, null, null, null, null, null, null, new HashMap<>(), true, null, new byte[0], null, null);

			assertNull("Expected an empty byte[] body to be treated as no body", capturedBody);

		} catch (FrameworkException fex) {
			fail("Caught a Framework Exception. Got status " + fex.getStatus() + ": " + fex.getMessage());
		}
	}

	@Test
	public void deleteWithStringBodySendsEntity() {

		registerBodyCapturingContext("/internal", 200, "OK".getBytes(StandardCharsets.UTF_8));

		final String loopbackUrl = urlForPath("/internal");

		try {

			HttpHelper.delete(loopbackUrl, null, null, null, null, null, null, new HashMap<>(), true, null, "{\"reason\":\"cleanup\"}", null, "UTF-8");

			assertEquals("Expected DELETE with a body to still use the DELETE method", "DELETE", capturedMethod);
			assertEquals("Expected the request body to have been sent as-is", "{\"reason\":\"cleanup\"}", new String(capturedBody, StandardCharsets.UTF_8));

		} catch (FrameworkException fex) {
			fail("Caught a Framework Exception. Got status " + fex.getStatus() + ": " + fex.getMessage());
		}
	}

	@Test
	public void deleteWithByteArrayBodySendsEntity() {

		registerBodyCapturingContext("/internal", 200, "OK".getBytes(StandardCharsets.UTF_8));

		final String loopbackUrl = urlForPath("/internal");
		final byte[] requestBody = "raw bytes".getBytes(StandardCharsets.UTF_8);

		try {

			HttpHelper.delete(loopbackUrl, null, null, null, null, null, null, new HashMap<>(), true, null, requestBody, null, null);

			assertEquals("Expected the byte[] body to have been sent as-is", "raw bytes", new String(capturedBody, StandardCharsets.UTF_8));

		} catch (FrameworkException fex) {
			fail("Caught a Framework Exception. Got status " + fex.getStatus() + ": " + fex.getMessage());
		}
	}

	@Test
	public void deleteWithInputStreamBodyIsNeverTreatedAsEmpty() {

		registerBodyCapturingContext("/internal", 200, "OK".getBytes(StandardCharsets.UTF_8));

		final String loopbackUrl = urlForPath("/internal");
		final InputStream requestBody = new ByteArrayInputStream(new byte[0]);

		try {

			HttpHelper.delete(loopbackUrl, null, null, null, null, null, null, new HashMap<>(), true, null, requestBody, null, null);

			assertEquals("Expected DELETE method to be used", "DELETE", capturedMethod);

		} catch (FrameworkException fex) {
			fail("Caught a Framework Exception. Got status " + fex.getStatus() + ": " + fex.getMessage());
		}
	}

	@Test
	public void deleteWithBodySendsContentTypeHeaderWhenProvided() {

		registerBodyCapturingContext("/internal", 200, "OK".getBytes(StandardCharsets.UTF_8));

		final String loopbackUrl = urlForPath("/internal");

		try {

			HttpHelper.delete(loopbackUrl, null, null, null, null, null, null, new HashMap<>(), true, null, "{}", "application/json", "UTF-8");

			final List<String> contentType = capturedHeaders.get("Content-type");
			assertNotNull("Expected a Content-Type header to be captured. All captured headers: " + capturedHeaders.keySet(), contentType);
			assertTrue("Expected Content-Type to reflect the requested type, got: " + contentType, contentType.get(0).startsWith("application/json"));

		} catch (FrameworkException fex) {
			fail("Caught a Framework Exception. Got status " + fex.getStatus() + ": " + fex.getMessage());
		}
	}

	@Test
	public void deleteWithoutBodyAndWithoutContentTypeSendsNoContentTypeHeader() {

		registerBodyCapturingContext("/internal", 200, "OK".getBytes(StandardCharsets.UTF_8));

		final String loopbackUrl = urlForPath("/internal");

		try {

			HttpHelper.delete(loopbackUrl, null, null, null, null, null, null, new HashMap<>(), true, null, null, null, null);

			assertNull("Expected no Content-Type header when no body/entity was sent", capturedHeaders.get("Content-Type"));

		} catch (FrameworkException fex) {
			fail("Caught a Framework Exception. Got status " + fex.getStatus() + ": " + fex.getMessage());
		}
	}

	@Test
	public void deleteSendsCustomRequestHeaders() {

		registerCapturingContext("/internal", 200, "OK".getBytes(StandardCharsets.UTF_8));

		final String loopbackUrl = urlForPath("/internal");
		final Map<String, String> requestHeaders = new HashMap<>();
		requestHeaders.put("X-test-header", "TEST VALUE");

		try {

			HttpHelper.delete(loopbackUrl, null, null, requestHeaders, true);

			final List<String> received = capturedHeaders.get("X-test-header");

			assertNotNull("Expected X-Test-Header to be captured. All captured headers: " + capturedHeaders.keySet(), received);
			assertEquals("Expected custom header to be sent", "TEST VALUE", received.get(0));

		} catch (FrameworkException fex) {
			fail("Caught a Framework Exception. Got status " + fex.getStatus() + ": " + fex.getMessage());
		}
	}

	@Test
	public void deleteSendsCookieHeader() {

		registerCapturingContext("/cookie", 200, "OK".getBytes(StandardCharsets.UTF_8));
		final String loopbackUrl = urlForPath("/cookie");

		try {

			HttpHelper.delete(loopbackUrl, null, null, null, null, null, "test_session=12345", new HashMap<>(), true);

			final List<String> received = capturedHeaders.get("Cookie");

			assertNotNull("Expected Cookie header to be captured", received);
			assertEquals("Expected custom cookie to be sent", "test_session=12345", received.get(0));

		} catch (FrameworkException fex) {
			fail("Caught a Framework Exception. Got status " + fex.getStatus() + ": " + fex.getMessage());
		}
	}

	@Test
	public void deleteDoesNotFollowRedirectsByDefault() {

		registerContext("/target", 200, "TARGET REACHED".getBytes(StandardCharsets.UTF_8), null);

		final Map<String, String> redirectHeaders = new HashMap<>();
		redirectHeaders.put("Location", urlForPath("/target"));
		registerContext("/redirect", 301, new byte[0], redirectHeaders);

		final String loopbackUrl = urlForPath("/redirect");

		try {

			final Map<String, Object> returnData = HttpHelper.delete(loopbackUrl, null, null, null, null, null, null, new HashMap<>(), true, null);
			final Object statusCode = returnData.get(HttpHelper.FIELD_STATUS);

			assertEquals("Expected DELETE to not follow redirects by default", "301", statusCode);

		} catch (FrameworkException fex) {
			fail("Caught a Framework Exception. Got status " + fex.getStatus() + ": " + fex.getMessage());
		}
	}

	@Test
	public void deleteFollowsRedirectsWhenConfigured() {

		registerContext("/target", 200, "TARGET REACHED".getBytes(StandardCharsets.UTF_8), null);

		final Map<String, String> redirectHeaders = new HashMap<>();
		redirectHeaders.put("Location", urlForPath("/target"));
		registerContext("/redirect", 301, new byte[0], redirectHeaders);

		final String loopbackUrl = urlForPath("/redirect");

		final Map<String, Object> config = new HashMap<>();
		config.put("redirects", true);

		try {

			final Map<String, Object> returnData = HttpHelper.delete(loopbackUrl, null, null, null, null, null, null, new HashMap<>(), true, config);
			final Object statusCode = returnData.get(HttpHelper.FIELD_STATUS);
			final Object body = returnData.get(HttpHelper.FIELD_BODY);

			assertEquals("Expected status 200 after following the redirect", "200", statusCode);
			assertEquals("Expected body from the redirect target", "TARGET REACHED", body.toString());

		} catch (FrameworkException fex) {
			fail("Caught a Framework Exception. Got status " + fex.getStatus() + ": " + fex.getMessage());
		}
	}

	@Test
	public void deleteToUnreachableAddressThrowsFrameworkException() {

		// nothing is listening on this port on loopback
		final String unreachableUrl = "http://127.0.0.1:1/nowhere";

		try {

			HttpHelper.delete(unreachableUrl);
			fail("Expected a FrameworkException for an unreachable address");

		} catch (FrameworkException fex) {

			assertEquals("Expected status 422 for a connection failure", 422, fex.getStatus());
		}
	}
}
