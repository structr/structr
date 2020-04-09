/**
 * Copyright (C) 2010-2020 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.web.function;

import java.util.ArrayList;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.structr.rest.common.HttpHelper;
import org.structr.schema.action.ActionContext;

/**
 *
 */
public class HttpGetFunction extends UiAdvancedFunction {

	public static final String ERROR_MESSAGE_GET    = "Usage: ${GET(URL[, contentType[, username, password]])}. Example: ${GET('http://structr.org', 'text/html')}";
	public static final String ERROR_MESSAGE_GET_JS = "Usage: ${{Structr.GET(URL[, contentType[, username, password]])}}. Example: ${{Structr.HEAD('http://structr.org', 'text/html')}}";

	@Override
	public String getName() {
		return "GET";
	}

	@Override
	public String getSignature() {
		return "url [, contentType [, username, password] ]";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) {

		if (sources != null && sources.length >= 1 && sources.length <= 4 && sources[0] != null) {

			try {

				String address = sources[0].toString();
				String contentType = null;
				String username = null;
				String password = null;

				switch (sources.length) {

					case 4: password = sources[3].toString();
					case 3: username = sources[2].toString();
					case 2: contentType = sources[1].toString();
						break;
				}

				//long t0 = System.currentTimeMillis();
				if ("text/html".equals(contentType)) {

					final Document doc = Jsoup.parse(HttpHelper.get(address, ctx.getHeaders()));

					if (sources.length > 2) {

						Elements elements = doc.select(sources[2].toString());

						if (elements.size() > 1) {

							final List<String> parts = new ArrayList<>();

							for (final Element el : elements) {

								parts.add(el.html());

							}

							return parts;

						} else {
							return elements.html();
						}

					} else {

						return doc.html();
					}
				} else if ("application/octet-stream".equals(contentType)) {
					
					return getBinaryFromUrl(ctx, address, username, password);
					
				} else {

					return getFromUrl(ctx, address, username, password);
				}

			} catch (Throwable t) {

				logException(caller, t, sources);

			}

			return "";

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
	public String shortDescription() {
		return "Sends an HTTP GET request to the given URL and returns the response headers and body";
	}

}