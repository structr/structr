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

import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObjectMap;
import org.structr.core.property.GenericProperty;
import org.structr.core.property.IntProperty;
import org.structr.core.property.StringProperty;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.docs.ontology.FunctionCategory;
import org.structr.rest.common.HttpHelper;
import org.structr.common.error.ArgumentTypeException;
import org.structr.schema.action.ActionContext;

import java.util.List;
import java.util.Map;

public class HttpPutFunction extends UiAdvancedFunction {

	@Override
	public String getName() {

		return "PUT";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			// max length as well as min, and only the mandatory arguments checked for null: the optional
			// ones may legitimately be passed as null to reach the options object behind them, which the
			// code below is written to handle. Asserting no nulls anywhere contradicted that.
			assertArrayHasMinLengthAndMaxLength(sources, 2, 4);

			for (int i = 0; i < 2; i++) {

				if (sources[i] == null) {

					throw new ArgumentTypeException("PUT(): the url and the body must not be null.");
				}
			}

			final String uri          = sources[0].toString();
			final Object body         = HttpBody.of(sources[1]);
			final String contentType  = (sources.length >= 3 && sources[2] != null) && !HttpOptions.isOptionsAt(sources, 2) ? sources[2].toString() : "application/json";
			final HttpOptions options = HttpOptions.fromAnyOf("PUT", sources, 3, 2).accepting("PUT", HttpOptions.PARSE_RESPONSE);

			HttpBody.checkRepeatable("PUT", body, options);

			final String charset               = HttpOptions.charsetOf(contentType, "utf-8");
			final Map<String, String> headers  = options.mergeHeaders(ctx.getHeaders());
			final boolean validateCertificates = options.getBoolean(HttpOptions.VALIDATE_CERTIFICATES, ctx.isValidateCertificates());

			final Map<String, Object> responseData = HttpHelper.put(uri, body, options.getString(HttpOptions.USERNAME), options.getString(HttpOptions.PASSWORD),
				null, null, null, null, headers, charset, validateCertificates, contentType, options.asRequestConfig());

			return buildResponse(ctx, caller, responseData, options.getBoolean(HttpOptions.PARSE_RESPONSE, false));


		} catch (IllegalArgumentException e) {

			logParameterError(caller, sources, e.getMessage(), ctx.isJavaScriptContext());

			return null;
		}
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
			Parameter.optional("options", "object with optional settings: `username` and `password` for basic auth, `preemptive` to send them on the first request instead of waiting for a 401 challenge, `headers` merged over add_header(), `timeout` in seconds, `redirects` to follow redirects, `validateCertificates`, `parseResponse` to parse the response body as JSON")
		);
	}

	@Override
	public List<Usage> getUsages() {

		return List.of(
			Usage.structrScript("Usage: ${PUT(url, body [, contentType [, options ]])}. Example: ${PUT('http://localhost:8082/structr/rest/folders/6aa10d68569d45beb384b42a1fc78c50', '{name:\"Test\"}', 'application/json')}"),
			Usage.javaScript("Usage: ${{ $.PUT(url, body [, contentType [, options ]]) }}. Example: ${{ $.PUT('http://localhost:8082/structr/rest/folders/6aa10d68569d45beb384b42a1fc78c50', '{name:\"Test\"}', 'application/json') }}")
		);
	}

	@Override
	public String getShortDescription() {

		return "Sends an HTTP PUT request with an optional content type to the given URL and returns the response headers and body.";
	}

	@Override
	public String getLongDescription() {

		return """
			This function can be used in a script to make an HTTP PUT request **from within the Structr Server**, triggered by a frontend control like a button etc.

			The `PUT()` function will return a response object with the following structure:

			| Field | Description | Type |
			| --- | --- | --- |
			status | HTTP status of the request | Integer |
			headers | Response headers | Map |
			body | Response body | Map or String |
			""";
	}

	@Override
	public List<String> getNotes() {

		return List.of(
			"7.0+: In JavaScript, `$.PUT.async(...)` takes the same arguments but starts the request and returns immediately, so several requests can be in flight at once and `await Promise.all([...])` costs the slowest of them rather than their sum. It is awaitable, not a full promise: use `Promise.resolve($.PUT.async(url)).catch(...)` to chain, `Promise.race()` answers the call that finished first, but the script still waits for the calls it did not answer with before it ends, so a race costs the slowest rather than the fastest. Only JavaScript has it - StructrScript always calls `PUT()` synchronously.",
			"The `PUT()` function will **not** be executed in the security context of the current user. The request will be made **by the Structr server**, without any user authentication or additional information. If you want to access external protected resources, you will need to authenticate the request using `addHeader()` (see the related articles for more information).",
			"As of Structr 6.0, it is possible to restrict HTTP calls based on a whitelist setting in structr.conf, `application.httphelper.urlwhitelist`. However the default behaviour in Structr is to allow all outgoing calls.",
			"`contentType` is the expected response content type. If you need to define the request content type, use `addHeader('Content-Type', 'your-content-type-here')`",
			"If the MIME type of the response is `application/json`, the `body` field will contain the mapped response as a Structr object that can be accessed using the dot notation (e.g. `result.body.resultCount`). Otherwise, the body field will contain the response as a string.  (see the related articles for more information)"

		);
	}

	@Override
	public FunctionCategory getCategory() {

		return FunctionCategory.Http;
	}
}
