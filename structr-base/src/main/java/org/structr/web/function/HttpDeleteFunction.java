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

import org.structr.common.error.ArgumentTypeException;
import org.structr.common.error.FrameworkException;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.ontology.FunctionCategory;
import org.structr.rest.common.HttpHelper;
import org.structr.schema.action.ActionContext;

import java.util.List;
import java.util.Map;

public class HttpDeleteFunction extends UiAdvancedFunction {

	@Override
	public String getName() {

		return "DELETE";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			// only the url is mandatory: the body and the content type are placeholders a caller passes
			// as null to reach the options, so they must be allowed to be null
			assertArrayHasMinLengthAndMaxLength(sources, 1, 4);

			if (sources[0] == null) {

				throw new ArgumentTypeException("DELETE(): the url must not be null.");
			}

			final String uri = sources[0].toString();

			// The signature gained a body, so an options object in the position it used to occupy would
			// now be sent as one. Refuse it by name rather than making that request.
			if (sources.length >= 2 && sources[1] instanceof Map) {

				throw new ArgumentTypeException("DELETE(): the options object moved. The signature is DELETE(url [, body [, contentType [, options ]]]) now, "
					+ "so pass null as the body when there is none: DELETE(url, null, null, { ... }).");
			}

			final Object body         = (sources.length >= 2 && sources[1] != null) && !HttpOptions.isOptionsAt(sources, 1) ? HttpBody.of(sources[1]) : null;
			final String contentType  = (sources.length >= 3 && sources[2] != null) && !HttpOptions.isOptionsAt(sources, 2) ? sources[2].toString() : null;
			final HttpOptions options = optionsOf(sources).accepting(ctx, "DELETE", HttpOptions.ASYNC, HttpOptions.PARSE_RESPONSE);

			HttpBody.checkRepeatable("DELETE", body, options);

			final Map<String, String> headers  = options.mergeHeaders(ctx.getHeaders());
			final boolean validateCertificates = options.getBoolean(HttpOptions.VALIDATE_CERTIFICATES, ctx.isValidateCertificates());
			final Map<String, Object> responseData = HttpHelper.delete(uri, options.getString(HttpOptions.USERNAME), options.getString(HttpOptions.PASSWORD),
				null, null, null, null, headers, validateCertificates, options.asRequestConfig(),
				body, contentType, HttpOptions.charsetOf(contentType, "utf-8"));

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

		return HttpOptions.fromAnyOf("DELETE", sources, 3, 1, 2);
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

		return Signature.forAllScriptingLanguages("url [, body [, contentType [, options ]]]");
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
			Parameter.mandatory("url", "URL to connect to"),
			Parameter.optional("body", "request body: a string, or a `File` or a stream (e.g. the body of a `binaryResponse`) whose content is sent. HTTP allows a body on DELETE and some APIs require it"),
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

		return "Sends an HTTP DELETE request with an optional content type to the given URL and returns the response headers and body.";
	}

	@Override
	public String getLongDescription() {

		return """
			This function can be used in a script to make an HTTP DELETE request **from within the Structr Server**, triggered by a frontend control like a button etc.

			The `DELETE()` function will return a response object with the following structure:

			| Field | Description | Type |
			| --- | --- | --- |
			status | HTTP status of the request | Integer |
			headers | Response headers | Map |
			body | Response body | Map or String |

			`contentType` can be left out entirely, so the options object can follow right after the body, e.g. `DELETE(url, body, { timeout: 10 })`. Without a body, pass `null` for it: `DELETE(url, null, { username: 'user', password: 'pass' })`. An object right after the url is refused, because that is where the options were before 7.0. For the same reason, a JSON body must be passed as a string, e.g. `JSON.stringify(data)`.
			""";
	}

	@Override
	public List<Example> getExamples() {

		return List.of(Example.structrScript("${DELETE('http://localhost:8082/structr/rest/User/6aa10d68569d45beb384b42a1fc78c50')}"));
	}

	@Override
	public List<String> getNotes() {

		return List.of(
			"7.0+: In JavaScript, the `async` option starts the request and returns immediately, so several requests can be in flight at once and `await Promise.all([...])` costs the slowest of them rather than their sum. The result is awaitable, not a full promise: use `Promise.resolve($.DELETE(url, { async: true })).catch(...)` to chain. `Promise.race()` answers as soon as its winner arrives, and the calls it beat are discarded. StructrScript has no way to await a result, so it rejects the option rather than calling `DELETE()` synchronously without saying so.",
			"The `DELETE()` function will **not** be executed in the security context of the current user. The request will be made **by the Structr server**, without any user authentication or additional information. To access protected resources, use the `username` and `password` options for HTTP Basic Auth, or the `headers` option or `addHeader()` for header-based authentication, e.g. a bearer token.",
			"As of Structr 6.0, it is possible to restrict HTTP calls based on a whitelist setting in structr.conf, `application.httphelper.urlwhitelist`. However the default behaviour in Structr is to allow all outgoing calls."
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
