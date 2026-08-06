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
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.docs.ontology.FunctionCategory;
import org.structr.rest.common.HttpHelper;
import org.structr.schema.action.ActionContext;

import java.util.List;
import java.util.Map;

/**
 * Sends an HTTP request with an arbitrary method to a given URL.
 * Unlike the dedicated GET/POST/PUT/DELETE/PATCH functions, this
 * supports any HTTP verb (e.g. PROPFIND, MKCOL, MOVE, COPY) as
 * required by protocols like WebDAV.
 */
public class HttpFetchFunction extends UiAdvancedFunction {

	@Override
	public String getName() {

		return "FETCH";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndAllElementsNotNull(sources, 2);

			final String url     = sources[0].toString();
			final String method  = sources[1].toString();
			final String body    = (sources.length >= 3 && sources[2] != null) ? sources[2].toString() : null;
			final String charset = (sources.length >= 4 && sources[3] != null) ? sources[3].toString() : "UTF-8";
			boolean followRedirects = false;
			Integer timeout         = null;

			if (sources.length >= 5 && sources[4] != null && sources[4] instanceof Map) {

				final Map<String, Object> config = (Map<String, Object>) sources[4];
				if (Boolean.TRUE.equals(config.get("redirects"))) {

					followRedirects = true;
				}

				if (config.containsKey("timeout") && config.get("timeout") instanceof Number) {

					timeout = ((Number) config.get("timeout")).intValue() * 1000;
				}
			}

			final Map<String, Object> responseData = HttpHelper.fetch(url, method, body, null, null, ctx.getHeaders(), charset, ctx.isValidateCertificates(), followRedirects, timeout);
			final GraphObjectMap response = new GraphObjectMap();

			response.setProperty(new StringProperty(HttpHelper.FIELD_BODY), responseData.get(HttpHelper.FIELD_BODY));

			final int statusCode = Integer.parseInt(responseData.get(HttpHelper.FIELD_STATUS) != null
					? responseData.get(HttpHelper.FIELD_STATUS).toString() : "0");
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

		return Signature.forAllScriptingLanguages("url, method [, body, charset, configMap ]");
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
			Parameter.mandatory("url", "URL to connect to"),
			Parameter.mandatory("method", "HTTP method (GET, POST, PUT, DELETE, PATCH, PROPFIND, MKCOL, MOVE, COPY, etc.)"),
			Parameter.optional("body", "request body"),
			Parameter.optional("charset", "charset of the request body (default: UTF-8)"),
			Parameter.optional("configMap", "JSON object for request configuration, supports `timeout` in seconds, `redirects` with true or false")
		);
	}

	@Override
	public List<Usage> getUsages() {

		return List.of(Usage.structrScript("Usage: ${FETCH(url, method [, body, charset, configMap])}"), Usage.javaScript("Usage: $.FETCH(url, method [, body, charset, configMap])"));
	}

	@Override
	public List<Example> getExamples() {

		return List.of(
			Example.javaScript("""
				$.addHeader('Authorization', 'Basic ' + $.base64encode('user:pass', 'basic', 'UTF-8'));
				$.addHeader('Depth', '1');
				$.addHeader('Content-Type', 'application/xml; charset=utf-8');

				const response = $.FETCH(
				    'https://cloud.example.com/remote.php/dav/files/user/',
				    'PROPFIND',
				    '<?xml version="1.0"?><d:propfind xmlns:d="DAV:"><d:prop><d:resourcetype/><d:displayname/></d:prop></d:propfind>'
				);
				""", "Send a WebDAV PROPFIND request to list a Nextcloud directory."),
			Example.javaScript("""
				$.addHeader('Authorization', 'Basic ' + $.base64encode('user:pass', 'basic', 'UTF-8'));

				const response = $.FETCH(
				    'https://cloud.example.com/remote.php/dav/files/user/NewFolder/',
				    'MKCOL'
				);
				""", "Create a folder on a WebDAV server using MKCOL."),
			Example.javaScript("""
				$.addHeader('Authorization', 'Basic ' + $.base64encode('user:pass', 'basic', 'UTF-8'));
				$.addHeader('Destination', 'https://cloud.example.com/remote.php/dav/files/user/new-name.txt');
				$.addHeader('Overwrite', 'F');

				const response = $.FETCH(
				    'https://cloud.example.com/remote.php/dav/files/user/old-name.txt',
				    'MOVE'
				);
				""", "Rename/move a file on a WebDAV server using MOVE.")
		);
	}

	@Override
	public String getShortDescription() {

		return "Sends an HTTP request with an arbitrary method to the given URL and returns the response headers, body, and status code.";
	}

	@Override
	public String getLongDescription() {

		return """
			This function sends an HTTP request with any HTTP method to the given URL. Unlike `GET()`, `POST()`, `PUT()` etc., the `FETCH()` function supports arbitrary HTTP methods such as PROPFIND, MKCOL, MOVE, COPY, REPORT, SEARCH and others required by protocols like WebDAV.

			The `FETCH()` function returns a response object with the following structure:

			| Field | Description | Type |
			| --- | --- | --- |
			status | HTTP status of the request | Integer |
			headers | Response headers | Map |
			body | Response body | String |

			Authentication is handled via `addHeader()`, just like the other HTTP functions.

			The configMap parameter can be used to configure the timeout and redirect behaviour (e.g. `{ timeout: 60, redirects: true }`). By default there is no timeout and redirects are not followed.
			""";
	}

	@Override
	public List<String> getNotes() {

		return List.of(
			"The `FETCH()` function will **not** be executed in the security context of the current user. The request will be made **by the Structr server**, without any user authentication or additional information. Use `addHeader()` for authentication.",
			"As of Structr 6.0, it is possible to restrict HTTP calls based on a whitelist setting in structr.conf, `application.httphelper.urlwhitelist`. However the default behaviour in Structr is to allow all outgoing calls.",
			"The response body is always returned as a string. For binary content, consider using `GET()` with `application/octet-stream`.",
			"While `FETCH()` can also be used for standard methods like GET and POST, it is recommended to use the dedicated functions for those, as they offer additional features like automatic JSON parsing and binary content handling."
		);
	}

	@Override
	public FunctionCategory getCategory() {

		return FunctionCategory.Http;
	}
}
