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
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
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
			final HttpOptions options = optionsOf(sources).accepting(ctx, "PUT", HttpOptions.ASYNC, HttpOptions.PARSE_RESPONSE);

			HttpBody.checkRepeatable("PUT", body, options);

			final String charset               = HttpOptions.charsetOf(contentType, "utf-8");
			final Map<String, String> headers  = options.mergeHeaders(ctx.getHeaders());
			final boolean validateCertificates = options.getBoolean(HttpOptions.VALIDATE_CERTIFICATES, ctx.isValidateCertificates());
			final Map<String, Object> responseData = HttpHelper.put(uri, body, options.getString(HttpOptions.USERNAME), options.getString(HttpOptions.PASSWORD),
				null, null, null, null, headers, charset, validateCertificates, contentType, options.asRequestConfig());

			return buildResponse(ctx, caller, responseData, options.getBoolean(HttpOptions.PARSE_RESPONSE, false));

		} catch (IllegalArgumentException e) {

			logParameterError(caller, sources, e.getMessage());

			return null;
		}
	}

	/**
	 * The options object of this call, wherever the caller put it.
	 *
	 * One accessor rather than one per caller, so the positions the object may occupy are stated once:
	 * apply() reads the settings from it, and the async opt-in below is read before the call is made.
	 */
	private static HttpOptions optionsOf(final Object[] sources) {

		return HttpOptions.fromAnyOf("PUT", sources, 3, 2);
	}

	@Override
	public boolean isAsyncRequested(final Object[] sources) {

		try {

			return optionsOf(sources).getBoolean(HttpOptions.ASYNC, false);

		} catch (final IllegalArgumentException e) {

			// a malformed options argument is not this method's to report: apply() runs either way, and
			// turns it into the usage error that names what is wrong with it

			return false;
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
			Parameter.mandatory("body", "request body: a string, or a `File` or a stream (e.g. the body of a `binaryResponse`) whose content is sent"),
			Parameter.optional("contentType", "content type of the request body, sent as the Content-Type header, charset included (`application/json; charset=UTF-8`)"),
			Parameter.optional("options",
				"""
					object with optional settings\
					\
					<ul>\
						<li> `username`: username for basic authentication</li>\
						<li> `password`: password for basic authentication</li>\
						<li> `preemptive`: sends the credentials with the first request instead of waiting for a 401 challenge. Required when the body is a `File` or a stream and credentials are given (default: `false`)</li>\
						<li> `headers`: map of request headers. Merged with the headers set with `addHeader()`, the headers given here win on conflicts</li>\
						<li> `timeout`: timeout in seconds (default: no timeout)</li>\
						<li> `redirects`: indicates if redirects should be followed (default: `false`)</li>\
						<li> `validateCertificates`: indicates if the server certificate should be validated (default: the setting of `validateCertificates()`, which is `true` unless changed)</li>\
						<li> `async`: starts the request on a worker thread and returns an awaitable result instead of the response. JavaScript only (default: `false`)</li>\
						<li> `parseResponse`: indicates if the response body should be parsed as JSON (default: `false`)</li>\
					</ul>\
					"""
			)
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

			`contentType` can be left out entirely, so the options object can follow right after the body, e.g. `PUT(url, body, { timeout: 10 })`. There is no need to pass `null` for it.
			""";
	}

	@Override
	public List<String> getNotes() {

		return List.of(
			"7.0+: In JavaScript, the `async` option starts the request and returns immediately, so several requests can be in flight at once and `await Promise.all([...])` costs the slowest of them rather than their sum. The result is awaitable, not a full promise: use `Promise.resolve($.PUT(url, body, { async: true })).catch(...)` to chain. `Promise.race()` answers as soon as its winner arrives, and the calls it beat are discarded. StructrScript has no way to await a result, so it rejects the option rather than calling `PUT()` synchronously without saying so.",
			"The `PUT()` function will **not** be executed in the security context of the current user. The request will be made **by the Structr server**, without any user authentication or additional information. To access protected resources, use the `username` and `password` options for HTTP Basic Auth, or the `headers` option or `addHeader()` for header-based authentication, e.g. a bearer token.",
			"As of Structr 6.0, it is possible to restrict HTTP calls based on a whitelist setting in structr.conf, `application.httphelper.urlwhitelist`. However the default behaviour in Structr is to allow all outgoing calls.",
			"The `body` field contains the response as a string. With the `parseResponse` option it contains the parsed JSON instead, which can be accessed using the dot notation (e.g. `result.body.resultCount`)."

		);
	}

	@Override
	public FunctionCategory getCategory() {

		return FunctionCategory.Http;
	}

	/**
	 * The password option, sensitive headers and a password in the URL are credentials, so they are masked when a wrong call is logged.
	 */
	@Override
	protected String parametersForLog(final Object[] parameters) {

		return HttpOptions.parametersForLog(parameters);
	}
}
