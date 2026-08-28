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
package org.structr.web.function;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.ParseException;
import org.apache.http.entity.ContentType;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObjectMap;
import org.structr.core.property.GenericProperty;
import org.structr.core.property.IntProperty;
import org.structr.core.property.StringProperty;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.docs.ontology.FunctionCategory;
import org.structr.rest.common.HttpHelper;
import org.structr.schema.action.ActionContext;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class HttpGetFunction extends UiAdvancedFunction {

	@Override
	public String getName() {

		return "GET";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		if (sources != null && sources.length >= 1 && sources[0] != null) {

			try {

				final String address      = sources[0].toString();
				final String contentType  = (sources.length >= 2 && sources[1] != null) ? sources[1].toString() : null;
				final HttpOptions options = HttpOptions.from("GET", sources, 2).accepting("GET", HttpOptions.SELECTOR, HttpOptions.BINARY_RESPONSE, HttpOptions.PARSE_RESPONSE);

				final String charset  = HttpOptions.charsetOf(contentType, null);
				final String username = options.getString(HttpOptions.USERNAME);
				final String password = options.getString(HttpOptions.PASSWORD);
				final String selector = options.getString(HttpOptions.SELECTOR);

				final GraphObjectMap response = new GraphObjectMap();
				final Map<String, Object> responseData;

				if ("text/html".equals(contentType)) {

					responseData = HttpHelper.get(address, charset, username, password, null, null, null, null,
						options.mergeHeaders(ctx.getHeaders()), options.getBoolean(HttpOptions.VALIDATE_CERTIFICATES, ctx.isValidateCertificates()), options.asRequestConfig());

					final String body  = responseData.get(HttpHelper.FIELD_BODY) != null ? (String) responseData.get(HttpHelper.FIELD_BODY) : "";
					final Document doc = Jsoup.parse(body);

					// the selector is an option now: it used to sit in the same position as the username,
					// so what the third argument meant depended on the content type
					if (selector != null) {

						final Elements elements = doc.select(selector);
						if (elements.size() > 1) {

							final List<String> parts = new ArrayList<>();

							for (final Element el : elements) {

								parts.add(el.outerHtml());
							}

							return parts;

						} else {

							response.setProperty(new StringProperty(HttpHelper.FIELD_BODY), elements.outerHtml());
						}

					} else {

						response.setProperty(new StringProperty(HttpHelper.FIELD_BODY), doc.html());
					}

				} else if (options.getBoolean(HttpOptions.BINARY_RESPONSE, false)) {

					// Stream binary data instead of buffering into byte[] to avoid the 2 GB limit
					responseData = getStreamFromUrl(ctx, address, charset, username, password);

					response.setProperty(new GenericProperty<InputStream>(HttpHelper.FIELD_BODY), (InputStream) responseData.get(HttpHelper.FIELD_BODY));

				} else {

					// HttpHelper.get directly, not getFromUrl: that helper passes no request config, so a
					// timeout given in the options would be accepted here and quietly do nothing
					responseData = HttpHelper.get(address, charset, username, password, null, null, null, null,
						options.mergeHeaders(ctx.getHeaders()), options.getBoolean(HttpOptions.VALIDATE_CERTIFICATES, ctx.isValidateCertificates()), options.asRequestConfig());

					if (options.getBoolean(HttpOptions.PARSE_RESPONSE, false)) {

						response.setProperty(new GenericProperty(HttpHelper.FIELD_BODY), new FromJsonFunction().apply(ctx, caller, new Object[] { responseData.get(HttpHelper.FIELD_BODY) }));

					} else {

						response.setProperty(new StringProperty(HttpHelper.FIELD_BODY), responseData.get(HttpHelper.FIELD_BODY));
					}
				}

				// Set status and headers
				final int statusCode = Integer.parseInt(responseData.get(HttpHelper.FIELD_STATUS) != null ? responseData.get(HttpHelper.FIELD_STATUS).toString() : "0");
				response.setProperty(new IntProperty(HttpHelper.FIELD_STATUS), statusCode);

				if (responseData.containsKey(HttpHelper.FIELD_HEADERS) && responseData.get(HttpHelper.FIELD_HEADERS) instanceof Map map) {

					response.setProperty(new GenericProperty<Map<String, String>>(HttpHelper.FIELD_HEADERS), GraphObjectMap.fromMap(map));
				}

				return response;

			} catch (IllegalArgumentException e) {

				logParameterError(caller, sources, e.getMessage(), ctx.isJavaScriptContext());

				return null;
			}
			
		} else {

			logParameterError(caller, sources, ctx.isJavaScriptContext());
		}

		return null;
	}

	@Override
	public boolean isAsyncCapable() {

		// argument parsing, one call into HttpHelper, and building a GraphObjectMap out of the response:
		// no graph access, no transaction, nothing read from the SecurityContext
		return true;
	}

	@Override
	public List<Signature> getSignatures() {

		return List.of(
			Signature.javaScript("url [, contentType [, options ]]"),
			Signature.structrScript("url [, contentType [, options ]]")
		);
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
			Parameter.mandatory("url", "URL to connect to"),
			Parameter.optional("contentType", "content type of the request; `text/html` parses the response with jsoup, see the `selector` option"),
			Parameter.optional("options", "object with optional settings: `username` and `password` for basic auth, `preemptive` to send them on the first request instead of waiting for a 401 challenge, `headers` merged over add_header(), `validateCertificates`, `parseResponse` to parse the response body as JSON, `selector` for a CSS selector applied to a `text/html` response, and `binary` to stream the response")
		);
	}

	@Override
	public List<Usage> getUsages() {

		return List.of(
			Usage.structrScript("Usage: ${GET(url [, contentType [, options ]])}. Example: ${GET('http://structr.org', 'text/html', { selector: 'div.content' })}"),
			Usage.javaScript("Usage: ${{ $.GET(url [, contentType [, options ]]) }}. Example: ${{ $.GET('http://structr.org', 'text/html', { selector: 'div.content' }) }}")
		);
	}

	@Override
	public String getShortDescription() {

		return "Sends an HTTP GET request to the given URL and returns the response headers and body.";
	}

	@Override
	public String getLongDescription() {

		return """
			This function can be used in a script to make an HTTP GET request **from within the Structr Server**, triggered by a frontend control like a button etc.

			When the `binary` option is set, the response body is returned as a streaming `InputStream` instead of a `byte[]` array. This removes the previous 2 GB file size limit for binary downloads. The stream can be passed directly to `setContent()` which will stream the data to the file storage without buffering the entire content in memory. This stream is consumed by `setContent()` and can not be read again.

			The `GET()` function will return a response object with the following structure:

			| Field | Description | Type |
			| --- | --- | --- |
			| status | HTTP status of the request | Integer |
			| headers | Response headers | Map |
			| body | Response body | String, InputStream or Map |
			""";
	}

	@Override
	public List<Example> getExamples() {

		return List.of(
			Example.structrScript("${GET('http://localhost:8082/structr/rest/User').body}", "Return an 'Access denied' error message with code 401 from the local Structr instance (depending on the configuration of that instance), because you cannot access the User collection from the outside without authentication."),
			Example.structrScript("""
				${
					(
					  addHeader('X-User', 'admin'),
					  addHeader('X-Password', 'admin'),
					  GET('http://localhost:8082/structr/rest/User').body
					)
				}
				""", "Return the list of users from the local Structr instance (depending on the configuration of that instance)."),
			Example.structrScript("${GET('https://www.example.com', 'text/html').body}", "Return the HTML source code of the front page of example.com."),
			Example.structrScript("${GET('https://www.example.com', 'text/html; charset=UTF-8').body}", "Return the HTML source code of the front page of example.com (since the server sends a charset in the response, the given charset parameter is overridden)."),
			Example.structrScript("${GET('https://www.example.com', 'text/html; charset=ISO-8859-1').body}", "Return the HTML source code of the front page of example.com (since the server sends a charset in the response, the given charset parameter is overridden)."),
			Example.structrScript("${GET('https://www.example.com', 'text/html', '#footer').body}", "Return the HTML content of the element with the ID 'footer' from example.com."),
			Example.structrScript("""
				${
					setContent(
						create('Image', 'name', 'exampleLogo.png'),
						GET('https://www.example.com/logo.png', 'application/octet-stream').body
					)
				}
				""", "Create a new file with the example logo in the local Structr instance."),
			Example.javaScript("""
				${{
					let file = ...;
					$.addHeader('Authorization', 'Bearer ...');
					$.setContent(
						file,
						$.GET('https://example.com/large-file.zip', 'application/octet-stream').body
					);
				}}
				""", "Stream a large binary file directly into a Structr File node without size limit.")
		);
	}

	@Override
	public List<String> getNotes() {

		return List.of(
			"7.0+: In JavaScript, `$.GET.async(...)` takes the same arguments but starts the request and returns immediately, so several requests can be in flight at once and `await Promise.all([...])` costs the slowest of them rather than their sum. It is awaitable, not a full promise: use `Promise.resolve($.GET.async(url)).catch(...)` to chain, and note that `Promise.race()` does not report the fastest. Only JavaScript has it - StructrScript always calls `GET()` synchronously.",
			"From version 3.5 onwards, GET() supports binary content by setting the `contentType` parameter to `application/octet-stream`. (This is helpful when creating files - see examples.)",
			"7.0+: `contentType` is the content type of the REQUEST, sent as the `Content-Type` header. Its charset is used to interpret the response, unless the server provides one of its own.",
			"The `username` and `password` options are intended for HTTP Basic Auth. For header authentication use the `headers` option or `addHeader()`.",
			"The `GET()` function will **not** be executed in the security context of the current user. The request will be made **by the Structr server**, without any user authentication or additional information. If you want to access external protected resources, you will need to authenticate the request using `addHeader()` (see the related articles for more information).",
			"As of Structr 6.0, it is possible to restrict HTTP calls based on a whitelist setting in structr.conf, `application.httphelper.urlwhitelist`. However the default behaviour in Structr is to allow all outgoing calls.",
			"v6.4+: When using `application/octet-stream`, the response body is returned as a streaming `InputStream` instead of a `byte[]` array. This removes the previous 2 GB file size limit and avoids buffering the entire response in memory. The stream is consumed when passed to `setContent()` and can not be read more than once."
		);
	}

	@Override
	public FunctionCategory getCategory() {

		return FunctionCategory.Http;
	}
}