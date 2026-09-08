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
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.jose4j.lang.ByteUtil.concat;
import static org.testng.AssertJUnit.*;

/**
 * Test for {@link HttpHelper}'s post() Method
 */
public class HttpHelperPostTest extends HttpHelperTestBase {

	@Test
	public void postSendsBodyAndReceivesResponse() {

		registerEchoContext("/internal", 200);

		final String loopbackUrl = urlForPath("/internal");

		try {

			final Map<String, Object> returnData = HttpHelper.post(loopbackUrl, "TESTING CONTEXT");
			final Object body = returnData.get(HttpHelper.FIELD_BODY);
			final Object statusCode = returnData.get(HttpHelper.FIELD_STATUS);

			assertEquals("Expected the server to have received the request body", "TESTING CONTEXT", new String(capturedBody, StandardCharsets.UTF_8));
			assertEquals("Expected the echoed response body, got: " + body, "TESTING CONTEXT", body.toString());
			assertEquals("Expected status code 200, got " + statusCode, "200", statusCode);

		} catch (FrameworkException fex) {
			fail("Caught a Framework Exception. Got status " + fex.getStatus() + ": " + fex.getMessage());
		}
	}

	@Test
	public void postSendsRawBytesUnmodified() {

		registerEchoContext("/internal", 200);

		final String loopbackUrl = urlForPath("/internal");
		final byte[] payload = new byte[] { 0x00, 0x01, 0x02, (byte) 0xFF, (byte) 0xFE, 0x7F };

		try {

			final Map<String, Object> returnData = HttpHelper.post(loopbackUrl, payload);
			final Object statusCode = returnData.get(HttpHelper.FIELD_STATUS);

			assertArrayEquals("Expected the raw bytes to arrive at the server unmodified", payload, capturedBody);
			assertEquals("Expected status code 200, got " + statusCode, "200", statusCode);

		} catch (FrameworkException fex) {
			fail("Caught a Framework Exception. Got status " + fex.getStatus() + ": " + fex.getMessage());
		}
	}

	@Test
	public void postSendsInputStreamBodyChunked() {

		registerEchoContext("/internal", 200);

		final String loopbackUrl = urlForPath("/internal");
		final byte[] payload = "STREAMED CONTENT".getBytes(StandardCharsets.UTF_8);

		try {

			final Map<String, Object> returnData = HttpHelper.post(loopbackUrl, new ByteArrayInputStream(payload));
			final Object statusCode = returnData.get(HttpHelper.FIELD_STATUS);
			final List<String> transferEncoding = capturedHeaders.get("Transfer-encoding");

			assertNotNull("Expected the body to be sent chunked since an InputStream has no known length", transferEncoding);
			assertTrue("Expected chunked transfer encoding, got: " + transferEncoding, transferEncoding.get(0).equalsIgnoreCase("chunked"));
			assertArrayEquals("Expected the stream content to arrive unmodified", payload, capturedBody);
			assertEquals("Expected status code 200, got " + statusCode, "200", statusCode);

		} catch (FrameworkException fex) {
			fail("Caught a Framework Exception. Got status " + fex.getStatus() + ": " + fex.getMessage());
		}
	}

	@Test
	public void postWithEmptyStringBodySucceeds() {

		registerEchoContext("/internal", 200);

		final String loopbackUrl = urlForPath("/internal");

		try {

			final Map<String, Object> returnData = HttpHelper.post(loopbackUrl, "");
			final Object statusCode = returnData.get(HttpHelper.FIELD_STATUS);

			assertEquals("Expected status code 200 even with an empty body", "200", statusCode);
			assertEquals("Expected no bytes to have been captured", 0, capturedBody.length);

		} catch (FrameworkException fex) {
			fail("Caught a Framework Exception. Got status " + fex.getStatus() + ": " + fex.getMessage());
		}
	}

	@Test
	public void postWithCustomResponseCharset() {

		registerContext("/internal", 200, "café".getBytes(StandardCharsets.ISO_8859_1), null);

		final String loopbackUrl = urlForPath("/internal");

		try {

			final Map<String, Object> returnData = HttpHelper.post(loopbackUrl, "irrelevant body", null, null, null, null, null, null, new HashMap<>(), "ISO-8859-1", true, null, null);
			final Object body = returnData.get(HttpHelper.FIELD_BODY);
			final Object statusCode = returnData.get(HttpHelper.FIELD_STATUS);

			assertEquals("Expected the return string body, got: " + body, "café", body.toString());
			assertEquals("Expected status code 200, got " + statusCode, "200", statusCode);

		} catch (FrameworkException fex) {
			fail("Caught a Framework Exception. Got status " + fex.getStatus() + ": " + fex.getMessage());
		}
	}

	@Test
	public void postRequestBodyEncodedWithSpecifiedCharset() {

		registerEchoContext("/internal", 200);

		final String loopbackUrl = urlForPath("/internal");

		try {

			HttpHelper.post(loopbackUrl, "café", null, null, null, null, null, null, new HashMap<>(), "ISO-8859-1", true, null, null);

			assertArrayEquals("Expected the request body to be written to the wire using the specified charset", "café".getBytes(StandardCharsets.ISO_8859_1), capturedBody);

		} catch (FrameworkException fex) {
			fail("Caught a Framework Exception. Got status " + fex.getStatus() + ": " + fex.getMessage());
		}
	}

	@Test
	public void postWithNullCharsetUsesContentTypeHeader() {

		final Map<String, String> responseHeaders = new HashMap<>();
		responseHeaders.put("Content-Type", "text/plain; charset=ISO-8859-1");

		registerContext("/internal", 200, "café".getBytes(StandardCharsets.ISO_8859_1), responseHeaders);

		final String loopbackUrl = urlForPath("/internal");

		try {

			final Map<String, Object> returnData = HttpHelper.post(loopbackUrl, "irrelevant body", null, null, null, null, null, null, new HashMap<>(), null, true, null, null);
			final Object body = returnData.get(HttpHelper.FIELD_BODY);
			final Object statusCode = returnData.get(HttpHelper.FIELD_STATUS);

			assertEquals("Expected the return string body, got: " + body, "café", body.toString());
			assertEquals("Expected status code 200, got " + statusCode, "200", statusCode);

		} catch (FrameworkException fex) {
			fail("Caught a Framework Exception. Got status " + fex.getStatus() + ": " + fex.getMessage());
		}
	}

	@Test
	public void postStripsBOMFromResponseBody() {

		final byte[] bom = new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };
		registerContext("/internal", 200, concat(bom, "TESTING CONTEXT".getBytes(StandardCharsets.UTF_8)), null);

		final String loopbackUrl = urlForPath("/internal");

		try {

			final Map<String, Object> returnData = HttpHelper.post(loopbackUrl, "irrelevant body", null, null, null, null, null, null, new HashMap<>(), "UTF-8", true, null, null);
			final Object body = returnData.get(HttpHelper.FIELD_BODY);
			final Object statusCode = returnData.get(HttpHelper.FIELD_STATUS);

			assertEquals("Expected the BOM to have been stripped, got: " + body, "TESTING CONTEXT", body.toString());
			assertEquals("Expected status code 200, got " + statusCode, "200", statusCode);

		} catch (FrameworkException fex) {
			fail("Caught a Framework Exception. Got status " + fex.getStatus() + ": " + fex.getMessage());
		}
	}

	@Test
	public void post404DoesNotThrow() {

		registerContext("/internal", 404, "TESTING CONTEXT".getBytes(StandardCharsets.UTF_8), null);

		final String loopbackUrl = urlForPath("/internal");

		try {

			final Map<String, Object> returnData = HttpHelper.post(loopbackUrl, "body");
			final Object body = returnData.get(HttpHelper.FIELD_BODY);
			final Object statusCode = returnData.get(HttpHelper.FIELD_STATUS);

			assertEquals("Expected the return string body, got: " + body, "TESTING CONTEXT", body.toString());
			assertEquals("Expected status code 404, got " + statusCode, "404", statusCode);

		} catch (FrameworkException fex) {
			fail("Caught a Framework Exception. Got status " + fex.getStatus() + ": " + fex.getMessage());
		}
	}

	@Test
	public void post500DoesNotThrow() {

		registerContext("/internal", 500, "TESTING CONTEXT".getBytes(StandardCharsets.UTF_8), null);

		final String loopbackUrl = urlForPath("/internal");

		try {

			final Map<String, Object> returnData = HttpHelper.post(loopbackUrl, "body");
			final Object body = returnData.get(HttpHelper.FIELD_BODY);
			final Object statusCode = returnData.get(HttpHelper.FIELD_STATUS);

			assertEquals("Expected the return string body, got: " + body, "TESTING CONTEXT", body.toString());
			assertEquals("Expected status code 500, got " + statusCode, "500", statusCode);

		} catch (FrameworkException fex) {
			fail("Caught a Framework Exception. Got status " + fex.getStatus() + ": " + fex.getMessage());
		}
	}

	@Test
	public void postSendsCustomRequestHeaders() {

		registerCapturingContext("/internal", 200, "TESTING CONTEXT".getBytes(StandardCharsets.UTF_8));

		final String loopbackUrl = urlForPath("/internal");
		final Map<String, String> requestHeaders = new HashMap<>();
		requestHeaders.put("X-test-header", "TEST VALUE");

		try {

			HttpHelper.post(loopbackUrl, "body", null, null, requestHeaders, true);

			final List<String> received = capturedHeaders.get("X-test-header");

			assertNotNull("Expected X-Test-Header to be captured. All captured headers: " + capturedHeaders.keySet(), received);
			assertEquals("Expected custom header to be sent", "TEST VALUE", received.get(0));

		} catch (FrameworkException fex) {
			fail("Caught a Framework Exception. Got status " + fex.getStatus() + ": " + fex.getMessage());
		}
	}

	@Test
	public void postSendsCookieHeader() {

		registerCapturingContext("/cookie", 200, "COOKIE CONTEXT".getBytes(StandardCharsets.UTF_8));
		final String loopbackUrl = urlForPath("/cookie");

		try {

			HttpHelper.post(loopbackUrl, "body", null, null, null, null, null, "test_session=12345", new HashMap<>(), true);

			final List<String> received = capturedHeaders.get("Cookie");

			assertNotNull("Expected Cookie header to be captured", received);
			assertEquals("Expected custom cookie to be sent", "test_session=12345", received.get(0));

		} catch (FrameworkException fex) {
			fail("Caught a Framework Exception. Got status " + fex.getStatus() + ": " + fex.getMessage());
		}
	}

	@Test
	public void postResponseHeadersAreReturned() {

		final Map<String, String> responseHeaders = new HashMap<>();
		responseHeaders.put("X-custom-response-header", "response-value");

		registerContext("/internal", 200, "TESTING CONTEXT".getBytes(StandardCharsets.UTF_8), responseHeaders);

		final String loopbackUrl = urlForPath("/internal");

		try {

			final Map<String, Object> returnData = HttpHelper.post(loopbackUrl, "body");

			@SuppressWarnings("unchecked")
			final Map<String, String> headers = (Map<String, String>) returnData.get(HttpHelper.FIELD_HEADERS);

			assertNotNull("Expected headers to be present in the response data", headers);
			assertEquals("Expected the custom response header to be returned", "response-value", headers.get("X-custom-response-header"));

		} catch (FrameworkException fex) {
			fail("Caught a Framework Exception. Got status " + fex.getStatus() + ": " + fex.getMessage());
		}
	}

	@Test
	public void postDefaultContentTypeIsTextPlain() {

		registerCapturingContext("/internal", 200, "TESTING CONTEXT".getBytes(StandardCharsets.UTF_8));

		final String loopbackUrl = urlForPath("/internal");

		try {

			HttpHelper.post(loopbackUrl, "body", null, null, null, null, null, null, new HashMap<>(), "UTF-8", true, null, null);

			final List<String> contentType = capturedHeaders.get("Content-type");

			assertNotNull("Expected a Content-Type header to be sent", contentType);
			assertTrue("Expected default Content-Type to be text/plain, got: " + contentType, contentType.get(0).startsWith("text/plain"));
			assertTrue("Expected the configured charset to be present, got: " + contentType, contentType.get(0).contains("UTF-8"));

		} catch (FrameworkException fex) {
			fail("Caught a Framework Exception. Got status " + fex.getStatus() + ": " + fex.getMessage());
		}
	}

	@Test
	public void postSendsExplicitContentType() {

		registerCapturingContext("/internal", 200, "TESTING CONTEXT".getBytes(StandardCharsets.UTF_8));

		final String loopbackUrl = urlForPath("/internal");

		try {

			HttpHelper.post(loopbackUrl, "{\"a\":1}", null, null, null, null, null, null, new HashMap<>(), "UTF-8", true, null, "application/json");

			final List<String> contentType = capturedHeaders.get("Content-type");

			assertNotNull("Expected a Content-Type header to be sent", contentType);
			assertTrue("Expected the explicit content type to be sent, got: " + contentType, contentType.get(0).startsWith("application/json"));

		} catch (FrameworkException fex) {
			fail("Caught a Framework Exception. Got status " + fex.getStatus() + ": " + fex.getMessage());
		}
	}

	@Test
	public void postDoesNotFollowRedirectsByDefault() {

		final Map<String, String> redirectHeaders = new HashMap<>();
		redirectHeaders.put("Location", urlForPath("/target"));
		registerContext("/no-redirect", 301, new byte[0], redirectHeaders);

		final String loopbackUrl = urlForPath("/no-redirect");

		try {
			final Map<String, Object> returnData = HttpHelper.post(loopbackUrl, "body", null, null, null, null, null, null, new HashMap<>(), "UTF-8", true, null);

			final Object statusCode = returnData.get(HttpHelper.FIELD_STATUS);

			assertEquals("Expected status code 301 because redirects are not followed by default", "301", statusCode);

		} catch (FrameworkException fex) {
			fail("Caught a Framework Exception. Got status " + fex.getStatus() + ": " + fex.getMessage());
		}
	}

	@Test
	public void postFollowsRedirectsWhenConfigured() {

		registerContext("/target", 200, "TARGET REACHED".getBytes(StandardCharsets.UTF_8), null);

		final Map<String, String> redirectHeaders = new HashMap<>();
		redirectHeaders.put("Location", urlForPath("/target"));
		registerContext("/redirect", 301, new byte[0], redirectHeaders);

		final String loopbackUrl = urlForPath("/redirect");

		final Map<String, Object> config = new HashMap<>();
		config.put("redirects", true);

		try {

			final Map<String, Object> returnData = HttpHelper.post(loopbackUrl, "body", null, null, null, null, null, null, new HashMap<>(), "UTF-8", true, config, null);

			final Object body = returnData.get(HttpHelper.FIELD_BODY);
			final Object statusCode = returnData.get(HttpHelper.FIELD_STATUS);

			assertEquals("Expected status code 200 after successfully following the redirect", "200", statusCode);
			assertEquals("Expected the return string body from target", "TARGET REACHED", body.toString());

		} catch (FrameworkException fex) {
			fail("Caught a Framework Exception. Got status " + fex.getStatus() + ": " + fex.getMessage());
		}
	}


	@Test
	public void postThrowsFrameworkExceptionWhenServerUnreachable() {

		final String unreachableUrl = "http://127.0.0.1:1/unavailable";

		try {

			HttpHelper.post(unreachableUrl, "body");
			fail("Expected a FrameworkException to be thrown for an unreachable server");

		} catch (FrameworkException fex) {

			assertEquals("Expected status 422 for a connection failure", 422, fex.getStatus());
		}
	}

	@Test
	public void postRespectsConfiguredTimeout() {

		registerDelayedContext("/slow", 200, "TOO LATE".getBytes(StandardCharsets.UTF_8), 1000);

		final String loopbackUrl = urlForPath("/slow");

		final Map<String, Object> config = new HashMap<>();
		config.put("timeout", 100);

		try {

			HttpHelper.post(loopbackUrl, "body", null, null, null, null, null, null, new HashMap<>(), "UTF-8", true, config, null);
			fail("Expected a FrameworkException to be thrown because the server response exceeded the configured timeout");

		} catch (FrameworkException fex) {

			assertEquals("Expected status 422 for a timeout", 422, fex.getStatus());
		}
	}

}
