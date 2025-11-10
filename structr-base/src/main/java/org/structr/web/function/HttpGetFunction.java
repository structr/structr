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
import org.structr.docs.Signature;
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

	public static final String ERROR_MESSAGE_GET    = "Usage: ${GET(URL[, contentType[, username, password]])}. Example: ${GET('http://structr.org', 'text/html')}";
	public static final String ERROR_MESSAGE_GET_JS = "Usage: ${{Structr.GET(URL[, contentType[, username, password]])}}. Example: ${{Structr.GET('http://structr.org', 'text/html')}}";

	@Override
	public String getName() {
		return "GET";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllLanguages("url [, contentType [, username, password] ]");
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
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_GET_JS : ERROR_MESSAGE_GET);
	}

	@Override
	public String getShortDescription() {
		return "Sends an HTTP GET request to the given URL and returns the response headers and body";
	}

}