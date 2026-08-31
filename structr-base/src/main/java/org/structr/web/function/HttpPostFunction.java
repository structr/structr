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
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObjectMap;
import org.structr.core.property.ByteArrayProperty;
import org.structr.core.property.GenericProperty;
import org.structr.core.property.IntProperty;
import org.structr.core.property.StringProperty;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.docs.ontology.FunctionCategory;
import org.structr.rest.common.HttpHelper;
import org.structr.schema.action.ActionContext;

import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.List;
import java.util.Map;

public class HttpPostFunction extends UiAdvancedFunction {

	protected final String DEFAULT_CONTENT_TYPE = "application/json";
	protected final String DEFAULT_CHARSET      = "UTF-8";

	@Override
	public String getName() {

		return "POST";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndAllElementsNotNull(sources, 2);

			final String address      = sources[0].toString();
			final Object body         = HttpBody.of(sources[1]);
			final String contentType  = (sources.length >= 3 && sources[2] != null) ? sources[2].toString() : DEFAULT_CONTENT_TYPE;
			final HttpOptions options = HttpOptions.from("POST", sources, 3).accepting("POST", HttpOptions.BINARY_BODY, HttpOptions.PARSE_RESPONSE);

			HttpBody.checkRepeatable("POST", body, options);

			final String charset               = HttpOptions.charsetOf(contentType, DEFAULT_CHARSET);
			final Map<String, String> headers  = options.mergeHeaders(ctx.getHeaders());
			final String username              = options.getString(HttpOptions.USERNAME);
			final String password              = options.getString(HttpOptions.PASSWORD);
			final boolean validateCertificates = options.getBoolean(HttpOptions.VALIDATE_CERTIFICATES, ctx.isValidateCertificates());

			if (options.getBoolean(HttpOptions.BINARY_BODY, false)) {

				final Map<String, Object> binaryData = HttpHelper.postBinary(address, body, charset, username, password, headers, validateCertificates);
				final GraphObjectMap binaryResponse  = new GraphObjectMap();

				binaryResponse.setProperty(new ByteArrayProperty(HttpHelper.FIELD_BODY), binaryData.get(HttpHelper.FIELD_BODY));

				return binaryResponse;
			}

			final Map<String, Object> responseData = HttpHelper.post(address, body, username, password, null, null, null, null,
				headers, charset, validateCertificates, options.asRequestConfig(), contentType);

			return processResponseData(ctx, caller, responseData, options.getBoolean(HttpOptions.PARSE_RESPONSE, false));

		} catch (IllegalArgumentException e) {

			logParameterError(caller, sources, e.getMessage(), ctx.isJavaScriptContext());

			return null;
		}
	}

	protected GraphObjectMap processResponseData(final ActionContext ctx, final Object caller, final Map<String, Object> responseData, final boolean parseResponse) throws FrameworkException {

		final String responseBody = responseData.get(HttpHelper.FIELD_BODY) != null ? (String) responseData.get(HttpHelper.FIELD_BODY) : "";
		final GraphObjectMap response = new GraphObjectMap();

		if (parseResponse) {

			// explicit opt-in only: inferring this from either content type would make the return type
			// depend on the server rather than on the call
			response.setProperty(new GenericProperty(HttpHelper.FIELD_BODY), new FromJsonFunction().apply(ctx, caller, new Object[] { responseBody }));

		} else {

			response.setProperty(new StringProperty(HttpHelper.FIELD_BODY), responseBody);
		}

		// Set status and headers
		final int statusCode = Integer.parseInt(responseData.get(HttpHelper.FIELD_STATUS) != null ? responseData.get(HttpHelper.FIELD_STATUS).toString() : "0");
		response.setProperty(new IntProperty(HttpHelper.FIELD_STATUS), statusCode);

		if (responseData.containsKey(HttpHelper.FIELD_HEADERS) && responseData.get(HttpHelper.FIELD_HEADERS) instanceof Map map) {

			response.setProperty(new GenericProperty<Map<String, String>>(HttpHelper.FIELD_HEADERS), GraphObjectMap.fromMap(map));
		}

		return response;
	}

	@Override
	public boolean isAsyncCapable() {

		// argument parsing, one call into HttpHelper, and building a GraphObjectMap out of the response:
		// no graph access, no transaction, nothing read from the SecurityContext
		return true;
	}

	@Override
	public List<Signature> getSignatures() {

		return Signature.forAllScriptingLanguages("url, body [, contentType [, options ]]");
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
			Parameter.mandatory("url", "URL to connect to"),
			Parameter.mandatory("body", "request body"),
			Parameter.optional("contentType", "content type of the request body, sent as the Content-Type header, charset included (`application/json; charset=UTF-8`)"),
			Parameter.optional("options", "object with optional settings: `username` and `password` for basic auth, `preemptive` to send them on the first request instead of waiting for a 401 challenge, `headers` merged over add_header(), `timeout` in seconds, `redirects` to follow redirects, `validateCertificates`, `parseResponse` to parse the response body as JSON, and `binaryBody` to return the RESPONSE body as a byte array (despite its name, it does not change how the request body is sent)")
		);
	}

	@Override
	public List<Usage> getUsages() {

		return List.of(
			Usage.structrScript("Usage: ${POST(url, body [, contentType [, options ]])}. Example: ${POST('http://localhost:8082/structr/rest/folders', '{name:\"Test\"}', 'application/json; charset=UTF-8')}"),
			Usage.javaScript("Usage: ${{ $.POST(url, body [, contentType [, options ]]) }}. Example: ${{ $.POST('http://localhost:8082/structr/rest/folders', '{name:\"Test\"}', 'application/json; charset=UTF-8') }}")
		);
	}

	@Override
	public String getShortDescription() {

		return "Sends an HTTP POST request to the given URL and returns the response body.";
	}

	@Override
	public String getLongDescription() {

		return """
			This function can be used in a script to make an HTTP POST request **from within the Structr Server**, triggered by a frontend control like a button etc.

			The `POST()` function will return a response object containing the response headers, body and status code. The object has the following structure:

			| Field | Description | Type |
			| --- | --- | --- |
			status | HTTP status of the request | Integer |
			headers | Response headers | Map |
			body | Response body | Map or String |

			The options object configures everything else, for example `{ timeout: 60, redirects: true }`. The timeout is given in seconds; by default there is no timeout and redirects are not followed.
			""";
	}

	@Override
	public List<String> getNotes() {

		return List.of(
			"7.0+: In JavaScript, `$.POST.async(...)` takes the same arguments but starts the request and returns immediately, so several requests can be in flight at once and `await Promise.all([...])` costs the slowest of them rather than their sum. It is awaitable, not a full promise: use `Promise.resolve($.POST.async(url)).catch(...)` to chain, and note that `Promise.race()` does not report the fastest. Only JavaScript has it - StructrScript always calls `POST()` synchronously.",
			"The `POST()` function will **not** be executed in the security context of the current user. The request will be made **by the Structr server**, without any user authentication or additional information. If you want to access external protected resources, you will need to authenticate the request using `addHeader()` (see the related articles for more information).",
			"As of Structr 6.0, it is possible to restrict HTTP calls based on a whitelist setting in structr.conf, `application.httphelper.urlwhitelist`. However the default behaviour in Structr is to allow all outgoing calls.",
			"7.0+: `contentType` is the content type of the REQUEST and is sent as the `Content-Type` header. Before 7.0 it never reached the request and `addHeader('Content-Type', ...)` was needed instead.",
			"If the `contentType` is `application/json`, the response body is automatically parsed and the `body` key of the returned object is a map"
		);
	}

	@Override
	public FunctionCategory getCategory() {

		return FunctionCategory.Http;
	}
}
