/*
 * Copyright (C) 2010-2025 Structr GmbH
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
import org.structr.docs.Signature;
import org.structr.rest.common.HttpHelper;
import org.structr.schema.action.ActionContext;

import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.List;
import java.util.Map;

public class HttpPostFunction extends UiAdvancedFunction {

	public static final String ERROR_MESSAGE_POST    = "Usage: ${POST(URL, body [, contentType, charset, username, password, configMap])}. Example: ${POST('http://localhost:8082/structr/rest/folders', '{name:\"Test\"}', 'application/json', 'UTF-8')}";
	public static final String ERROR_MESSAGE_POST_JS = "Usage: ${{Structr.POST(URL, body [, contentType, charset, username, password, configMap])}}. Example: ${{Structr.POST('http://localhost:8082/structr/rest/folders', '{name:\"Test\"}', 'application/json', 'UTF-8')}}";

	protected final String DEFAULT_CONTENT_TYPE = "application/json";
	protected final String DEFAULT_CHARSET      = "UTF-8";

	@Override
	public String getName() {
		return "POST";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllLanguages("url, body [, contentType, charset, username, password, configMap ]");
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

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_POST_JS : ERROR_MESSAGE_POST);
	}

	@Override
	public String getShortDescription() {
		return "Sends an HTTP POST request to the given URL and returns the response body. The configMap parameter can be used to configure the timeout and redirect behaviour (e.g. config = { timeout: 60, redirects: true } ). By default there is not timeout and redirects are not followed.";
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
}
