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
import org.structr.core.property.ByteArrayProperty;
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

		if (sources != null && sources.length >= 1 && sources.length <= 4 && sources[0] != null) {

			try {

				String address = sources[0].toString();
				String contentType = null;
				String charset     = null;
				String username = null;
				String password = null;

				switch (sources.length) {

					case 4: password    = sources[3].toString();
					case 3: username    = sources[2].toString();
					case 2: contentType = sources[1].toString();
						break;
				}

				final GraphObjectMap response = new GraphObjectMap();

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

				final Map<String, Object> responseData;
				if ("text/html".equals(contentType)) {

					responseData = HttpHelper.get(address, charset, ctx.getHeaders(), ctx.isValidateCertificates());

					String body = responseData.get(HttpHelper.FIELD_BODY) != null ? (String) responseData.get(HttpHelper.FIELD_BODY) : "";

					final Document doc = Jsoup.parse(body);

					if (sources.length > 2) {

						Elements elements = doc.select(sources[2].toString());

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
				} else if ("application/octet-stream".equals(contentType)) {

					responseData = getBinaryFromUrl(ctx, address, charset, username, password);

					response.setProperty(new ByteArrayProperty(HttpHelper.FIELD_BODY), responseData.get(HttpHelper.FIELD_BODY));
				} else {

					responseData = getFromUrl(ctx, address, charset, username, password);

					response.setProperty(new StringProperty(HttpHelper.FIELD_BODY), responseData.get(HttpHelper.FIELD_BODY));
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
			
		} else {

			logParameterError(caller, sources, ctx.isJavaScriptContext());

		}

		return usage(ctx.isJavaScriptContext());
	}

	@Override
	public List<Signature> getSignatures() {

		return List.of(
			Signature.javaScript("url [, contentType [, username, password]]"),
			Signature.structrScript("url [, contentType [, username, password]]"),
			Signature.structrScript("url, 'text/html', selector"),
			Signature.structrScript("url, 'application/octet-stream' [, username, password]]")
		);
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
			Parameter.mandatory("url", "URL to connect to"),
			Parameter.optional("contentType", "expected content type (see notes)"),
			Parameter.optional("username", "username for the connection"),
			Parameter.optional("password", "password for the connection")
		);
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${GET(URL[, contentType[, username, password]])}. Example: ${GET('http://structr.org', 'text/html')}"),
			Usage.javaScript("Usage: ${{Structr.GET(URL[, contentType[, username, password]])}}. Example: ${{Structr.GET('http://structr.org', 'text/html')}}")
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

			The `GET()` function will return a response object with the following structure:

			| Field | Description | Type |
			| --- | --- | --- |
			status | HTTP status of the request | Integer |
			headers | Response headers | Map |
			body | Response body | Map or String |
			""";
	}

	@Override
	public List<Example> getExamples() {

		return List.of(
			Example.structrScript("${GET('http://localhost:8082/structr/rest/User')}", "Return an 'Access denied' error message with code 401 from the local Structr instance (depending on the configuration of that instance), because you cannot access the User collection from the outside without authentication."),
			Example.structrScript("""
			${
				(
				  addHeader('X-User', 'admin'),
				  addHeader('X-Password', 'admin'),
				  GET('http://localhost:8082/structr/rest/User')
				)
			}
			""", "Return the list of users from the local Structr instance (depending on the configuration of that instance)."),
			Example.structrScript("${GET('http://www.google.com', 'text/html')}", "Return the HTML source code of the front page of google.com."),
			Example.structrScript("${GET('http://www.google.com', 'text/html; charset=UTF-8')}", "Return the HTML source code of the front page of google.com (since the server sends a charset in the response, the given charset parameter is overridden)."),
			Example.structrScript("${GET('http://www.google.com', 'text/html; charset=ISO-8859-1')}", "Return the HTML source code of the front page of google.com (since the server sends a charset in the response, the given charset parameter is overridden)."),
			Example.structrScript("${GET('http://www.google.com', 'text/html', '#footer')}", "Return the HTML content of the element with the ID 'footer' from google.com."),
			Example.structrScript("${setContent(create('File', 'name', 'googleLogo.png'), GET('https://www.google.com/images/branding/googlelogo/1x/googlelogoLightColor_272x92dp.png', 'application/octet-stream'))}", "Create a new file with the google logo in the local Structr instance.")
		);
	}

	@Override
	public List<String> getNotes() {
		return List.of(
			"From version 3.5 onwards, GET() supports binary content by setting the `contentType` parameter to `application/octet-stream`. (This is helpful when creating files - see example.)",
			"v4.0+: `contentType` can be used like the `Content-Type` header - to set the **expected** response mime type and to set the `charset` with which the response will be interpreted (**unless** the server sends provides a charset, then this charset will be used).",
			"Prior to v4.0: `contentType` is the **expected** response content type (it does not influence the charset of the response - the charset from the **sending server** will be used).",
			"The parameters `username` and `password` are intended for HTTP Basic Auth. For header authentication use `addHeader()`.",
			"The `GET()` function will **not** be executed in the security context of the current user. The request will be made **by the Structr server**, without any user authentication or additional information. If you want to access external protected resources, you will need to authenticate the request using `addHeader()` (see the related articles for more information).",
			"As of Structr 6.0, it is possible to restrict HTTP calls based on a whitelist setting in structr.conf, `application.httphelper.urlwhitelist`. However the default behaviour in Structr is to allow all outgoing calls."
		);
	}

	@Override
	public FunctionCategory getCategory() {
		return FunctionCategory.Http;
	}
}