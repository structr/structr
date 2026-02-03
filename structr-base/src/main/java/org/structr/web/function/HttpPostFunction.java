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

			final String address  = sources[0].toString();
			final String body     = sources[1].toString();
			String contentType    = (sources.length >= 3 && sources[2] != null) ? sources[2].toString() : DEFAULT_CONTENT_TYPE;
			String charset        = (sources.length >= 4 && sources[3] != null) ? sources[3].toString() : DEFAULT_CHARSET;
			final String username = (sources.length >= 5 && sources[4] != null) ? sources[4].toString() : null;
			final String password = (sources.length >= 6 && sources[5] != null) ? sources[5].toString() : null;
			Map<String, Object> config = null;

			if (sources.length >= 7 && sources[6] != null && sources[6] instanceof Map) {
				config = (Map) sources[6];
			}

			// Extract character set from contentType if given
			if (StringUtils.isNotBlank(contentType)) {

				try {

					final ContentType ct = ContentType.parse(contentType);

					contentType = ct.getMimeType();

					final Charset cs = ct.getCharset();

					if (cs != null) {
						charset = cs.toString();
					}

				} catch(ParseException pe) {

					logger.warn("Unable to parse contentType parameter '{}' - using as is.", contentType);

				} catch (UnsupportedCharsetException uce) {

					logger.warn("Unsupported charset in contentType parameter '{}'", contentType);
				}
			}

			Map<String, Object> responseData = null;
			GraphObjectMap      response     = new GraphObjectMap();

			if ("application/octet-stream".equals(contentType)) {

				responseData = HttpHelper.postBinary(address, body, charset, username, password, ctx.getHeaders(), ctx.isValidateCertificates());
				response.setProperty(new ByteArrayProperty(HttpHelper.FIELD_BODY), responseData.get(HttpHelper.FIELD_BODY));

			} else {

				responseData = HttpHelper.post(address, body, username, password, ctx.getHeaders(),charset, ctx.isValidateCertificates(), config);
				response     = processResponseData(ctx, caller, responseData, contentType);
			}

			return response;

		} catch (IllegalArgumentException e) {

			logParameterError(caller, sources, e.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}
	}

	protected GraphObjectMap processResponseData(final ActionContext ctx, final Object caller, final Map<String, Object> responseData, final String contentType) throws FrameworkException {

		final String responseBody = responseData.get(HttpHelper.FIELD_BODY) != null ? (String) responseData.get(HttpHelper.FIELD_BODY) : "";

		final GraphObjectMap response = new GraphObjectMap();

		response.setProperty(new StringProperty(HttpHelper.FIELD_BODY), responseBody);

		// Set status and headers
		final int statusCode = Integer.parseInt(responseData.get(HttpHelper.FIELD_STATUS) != null ? responseData.get(HttpHelper.FIELD_STATUS).toString() : "0");
		response.setProperty(new IntProperty(HttpHelper.FIELD_STATUS), statusCode);

		if (responseData.containsKey(HttpHelper.FIELD_HEADERS) && responseData.get(HttpHelper.FIELD_HEADERS) instanceof Map map) {

			response.setProperty(new GenericProperty<Map<String, String>>(HttpHelper.FIELD_HEADERS), GraphObjectMap.fromMap(map));
		}

		return response;
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("url, body [, contentType, charset, username, password, configMap ]");
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
			Usage.structrScript("Usage: ${POST(URL, body [, contentType, charset, username, password, configMap])}. Example: ${POST('http://localhost:8082/structr/rest/folders', '{name:\"Test\"}', 'application/json', 'UTF-8')}"),
			Usage.javaScript("Usage: ${{Structr.POST(URL, body [, contentType, charset, username, password, configMap])}}. Example: ${{Structr.POST('http://localhost:8082/structr/rest/folders', '{name:\"Test\"}', 'application/json', 'UTF-8')}}")
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

			The configMap parameter can be used to configure the timeout and redirect behaviour (e.g. config = { timeout: 60, redirects: true } ). By default there is not timeout and redirects are not followed.
			""";
	}

	@Override
	public List<String> getNotes() {
		return List.of(
			"The `POST()` function will **not** be executed in the security context of the current user. The request will be made **by the Structr server**, without any user authentication or additional information. If you want to access external protected resources, you will need to authenticate the request using `addHeader()` (see the related articles for more information).",
			"As of Structr 6.0, it is possible to restrict HTTP calls based on a whitelist setting in structr.conf, `application.httphelper.urlwhitelist`. However the default behaviour in Structr is to allow all outgoing calls.",
			"`contentType` is the expected response content type. If you need to define the request content type, use `addHeader('Content-Type', 'your-content-type-here')`",
			"If the `contentType` is `application/json`, the response body is automatically parsed and the `body` key of the returned object is a map"
		);
	}

	@Override
	public FunctionCategory getCategory() {
		return FunctionCategory.Http;
	}
}
