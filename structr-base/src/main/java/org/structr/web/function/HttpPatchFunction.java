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
import org.structr.schema.action.ActionContext;

import java.util.List;
import java.util.Map;

public class HttpPatchFunction extends UiAdvancedFunction {

	@Override
	public String getName() {

		return "PATCH";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndAllElementsNotNull(sources, 2);

			final String uri          = sources[0].toString();
			final String body         = sources[1].toString();
			final String contentType  = (sources.length >= 3 && sources[2] != null) ? sources[2].toString() : "application/json";
			final HttpOptions options = HttpOptions.from("PATCH", sources, 3);

			final String charset               = HttpOptions.charsetOf(contentType, "utf-8");
			final Map<String, String> headers  = options.mergeHeaders(ctx.getHeaders());
			final boolean validateCertificates = options.getBoolean("validateCertificates", ctx.isValidateCertificates());

			final Map<String, Object> responseData = HttpHelper.patch(uri, body, options.getString("username"), options.getString("password"),
				null, null, null, null, headers, charset, validateCertificates, contentType, options.asRequestConfig());

			final String responseBody     = responseData.get(HttpHelper.FIELD_BODY) != null ? responseData.get(HttpHelper.FIELD_BODY).toString() : null;
			final GraphObjectMap response = new GraphObjectMap();

			if (options.getBoolean("parseResponse", false)) {

				// explicit opt-in only: the request content type says nothing about the response
				response.setProperty(new GenericProperty(HttpHelper.FIELD_BODY), new FromJsonFunction().apply(ctx, caller, new Object[] { responseBody }));

			} else {

				response.setProperty(new StringProperty(HttpHelper.FIELD_BODY), responseBody);
			}

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
	}

	@Override
	public List<Signature> getSignatures() {

		return Signature.forAllScriptingLanguages("url, body [, contentType, options ]");
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
			Parameter.mandatory("url", "URL to connect to"),
			Parameter.mandatory("body", "request body"),
			Parameter.optional("contentType", "content type of the request body, sent as the Content-Type header, charset included (`application/json; charset=UTF-8`)"),
			Parameter.optional("options", "object with optional settings: `username` and `password` for basic auth, `headers` merged over add_header(), `timeout` in seconds, `redirects` to follow redirects, `validateCertificates`, `parseResponse` to parse the response body as JSON")
		);
	}

	@Override
	public List<Usage> getUsages() {

		return List.of(
			Usage.structrScript("Usage: ${PATCH(url, body [, contentType, options])}. Example: ${PATCH('http://localhost:8082/structr/rest/folders/6aa10d68569d45beb384b42a1fc78c50', '{name:\"Test\"}', 'application/json')}"),
			Usage.javaScript("Usage: ${{ $.PATCH(url, body [, contentType, options]) }}. Example: ${{ $.PATCH('http://localhost:8082/structr/rest/folders/6aa10d68569d45beb384b42a1fc78c50', '{name:\"Test\"}', 'application/json') }}")
		);
	}

	@Override
	public String getShortDescription() {

		return "Sends an HTTP PATCH request to the given URL and returns the response headers and body.";
	}

	@Override
	public String getLongDescription() {

		return """
			This function can be used in a script to make an HTTP PATCH request **from within the Structr Server**, triggered by a frontend control like a button etc.

			The `PATCH()` function will return a response object containing the response headers, body and status code. The object has the following structure:

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
			"The `PATCH()` function will **not** be executed in the security context of the current user. The request will be made **by the Structr server**, without any user authentication or additional information. If you want to access external protected resources, you will need to authenticate the request using `addHeader()` (see the related articles for more information).",
			"As of Structr 6.0, it is possible to restrict HTTP calls based on a whitelist setting in structr.conf, `application.httphelper.urlwhitelist`. However the default behaviour in Structr is to allow all outgoing calls."
		);
	}

	@Override
	public FunctionCategory getCategory() {

		return FunctionCategory.Http;
	}
}
