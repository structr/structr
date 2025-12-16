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

public class HttpPutFunction extends UiAdvancedFunction {

	@Override
	public String getName() {
		return "PUT";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndAllElementsNotNull(sources, 2);

			final String uri = sources[0].toString();
			final String body = sources[1].toString();
			String contentType = "application/json";
			String charset = "utf-8";

			// override default content type
			if (sources.length >= 3 && sources[2] != null) {
				contentType = sources[2].toString();
			}

			// override default content type
			if (sources.length >= 4 && sources[3] != null) {
				charset = sources[3].toString();
			}

			final Map<String, Object> responseData = HttpHelper.put(uri, body, null, null, ctx.getHeaders(), charset, ctx.isValidateCertificates());

			final String responseBody = responseData.get(HttpHelper.FIELD_BODY) != null ? (String) responseData.get(HttpHelper.FIELD_BODY) : null;

			final GraphObjectMap response = new GraphObjectMap();

			if ("application/json".equals(contentType)) {

				final FromJsonFunction fromJsonFunction = new FromJsonFunction();
				response.setProperty(new StringProperty(HttpHelper.FIELD_BODY), fromJsonFunction.apply(ctx, caller, new Object[]{responseBody}));

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

		} catch (IllegalArgumentException e) {

			logParameterError(caller, sources, e.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("url, body [, contentType, charset ]");
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
			Parameter.mandatory("url", "URL to connect to"),
			Parameter.optional("body", "request body (JSON data)"),
			Parameter.optional("contentType", "content type of the request body"),
			Parameter.optional("charset", "charset of the request body"),
			Parameter.optional("username", "username for the connection"),
			Parameter.optional("password", "password for the connection"),
			Parameter.optional("configMap", "JSON object for request configuration, supports `timeout` in seconds, `redirects` with true or false to follow redirects")
		);
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${PUT(URL, body [, contentType, charset])}. Example: ${PUT('http://localhost:8082/structr/rest/folders/6aa10d68569d45beb384b42a1fc78c50', '{name:\"Test\"}', 'application/json', 'utf-8')}"),
			Usage.javaScript("Usage: ${{Structr.PUT(URL, body [, contentType, charset])}}. Example: ${{Structr.PUT('http://localhost:8082/structr/rest/folders/6aa10d68569d45beb384b42a1fc78c50', '{name:\"Test\"}', 'application/json', 'utf-8')}}")
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
