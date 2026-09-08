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

import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.docs.ontology.FunctionCategory;
import org.structr.common.error.FrameworkException;
import org.structr.rest.common.HttpHelper;
import org.structr.schema.action.ActionContext;

import java.util.List;
import java.util.Map;

/**
 *
 */
public class HttpHeadFunction extends UiAdvancedFunction {

	@Override
	public String getName() {

		return "HEAD";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		if (sources != null && sources.length >= 1 && sources[0] != null) {

			try {

				final String address      = sources[0].toString();
				final HttpOptions options = optionsOf(sources).accepting(ctx, "HEAD", HttpOptions.ASYNC);

				final Map<String, Object> responseData = HttpHelper.head(address, options.getString(HttpOptions.USERNAME), options.getString(HttpOptions.PASSWORD),
					null, null, null, null, options.mergeHeaders(ctx.getHeaders()),
					options.getBoolean(HttpOptions.VALIDATE_CERTIFICATES, ctx.isValidateCertificates()), options.asRequestConfig());

				// the same shape as every other verb: a HEAD has no body, but status is an int here too
				return buildResponse(ctx, caller, responseData, false);

			} catch (IllegalArgumentException e) {

				// only argument errors are swallowed, as in every other verb. A failed request throws a
				// FrameworkException from HttpHelper and must reach the script rather than becoming null.
				logParameterError(caller, sources, e.getMessage(), ctx.isJavaScriptContext());
			}

			return null;

		} else {

			logParameterError(caller, sources, ctx.isJavaScriptContext());
		}

		return null;
	}

	/**
	 * The options object of this call, wherever the caller put it.
	 *
	 * One accessor rather than one per caller, so the positions the object may occupy are stated once:
	 * apply() reads the settings from it, and the async opt-in below is read before the call is made.
	 */
	private static HttpOptions optionsOf(final Object[] sources) {

		return HttpOptions.from("HEAD", sources, 1);
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

		return Signature.forAllScriptingLanguages("url [, options ]");
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
			Parameter.mandatory("url", "URL to connect to"),
			Parameter.optional("options", "object with optional settings: `username` and `password` for basic auth, `preemptive` to send them on the first request instead of waiting for a 401 challenge, `headers` merged over add_header(), `timeout` in seconds, `redirects` to follow redirects, `validateCertificates`, `async` to start the request on a worker thread and answer an awaitable result (JavaScript only)")
		);
	}

	@Override
	public List<Usage> getUsages() {

		return List.of(
			Usage.structrScript("Usage: ${HEAD(url [, options ])}. Example: ${HEAD('http://structr.org', { username: 'foo', password: 'bar' })}"),
			Usage.javaScript("Usage: ${{ $.HEAD(url [, options ]) }}. Example: ${{ $.HEAD('http://structr.org', { username: 'foo', password: 'bar' }) }}")
		);
	}

	@Override
	public String getShortDescription() {

		return "Sends an HTTP HEAD request with optional username and password to the given URL and returns the response headers.";
	}

	@Override
	public String getLongDescription() {

		return """
			This function can be used in a script to make an HTTP HEAD request **from within the Structr Server**, triggered by a frontend control like a button etc. The optional username and password parameters can be used to authenticate the request.

			The `HEAD()` function will return a response object with the following structure:

			| Field | Description | Type |
			| --- | --- | --- |
			status | HTTP status of the request | Integer |
			headers | Response headers | Map |
			""";
	}

	@Override
	public List<String> getNotes() {

		return List.of(
			"7.0+: In JavaScript, the `async` option starts the request and returns immediately, so several requests can be in flight at once and `await Promise.all([...])` costs the slowest of them rather than their sum. The result is awaitable, not a full promise: use `Promise.resolve($.HEAD(url, { async: true })).catch(...)` to chain. `Promise.race()` answers as soon as its winner arrives, and the calls it beat are discarded. StructrScript has no way to await a result, so it rejects the option rather than calling `HEAD()` synchronously without saying so.",
			"The `HEAD()` function will **not** be executed in the security context of the current user. The request will be made **by the Structr server**, without any user authentication or additional information. If you want to access external protected resources, you will need to authenticate the request using `addHeader()` (see the related articles for more information).",
			"As of Structr 6.0, it is possible to restrict HTTP calls based on a whitelist setting in structr.conf, `application.httphelper.urlwhitelist`. However the default behaviour in Structr is to allow all outgoing calls."
		);
	}

	@Override
	public FunctionCategory getCategory() {

		return FunctionCategory.Http;
	}
}
