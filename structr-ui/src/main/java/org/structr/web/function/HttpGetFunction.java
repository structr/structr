/**
 * Copyright (C) 2010-2016 Structr GmbH
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

import java.io.InputStream;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;
import static org.structr.web.Importer.getHttpClient;

/**
 *
 */
public class HttpGetFunction extends UiFunction {

	public static final String ERROR_MESSAGE_GET    = "Usage: ${GET(URL[, contentType[, selector]])}. Example: ${GET('http://structr.org', 'text/html')}";
	public static final String ERROR_MESSAGE_GET_JS = "Usage: ${{Structr.GET(URL[, contentType[, selector]])}}. Example: ${{Structr.HEAD('http://structr.org', 'text/html')}}";

	@Override
	public String getName() {
		return "GET()";
	}

	@Override
	public Object apply(ActionContext ctx, final GraphObject entity, final Object[] sources) {

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

					HttpClient client = getHttpClient();

					GetMethod get = new GetMethod(address);
					get.addRequestHeader("User-Agent", "curl/7.35.0");
					get.addRequestHeader("Connection", "close");
					get.getParams().setParameter("http.protocol.single-cookie-header", true);
					get.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);

					get.setFollowRedirects(true);

					client.executeMethod(get);

					final InputStream response = get.getResponseBodyAsStream();

					// Skip BOM to workaround this Jsoup bug: https://github.com/jhy/jsoup/issues/348
					String code = IOUtils.toString(response, "UTF-8");

					System.out.println(code);

					if (code.charAt(0) == 65279) {
						code = code.substring(1);
					}

					final Document doc = Jsoup.parse(code);

					if (sources.length > 2) {

						return doc.select(sources[2].toString()).html();

					} else {

						return doc.html();
					}

				} else {

					return getFromUrl(ctx, address, username, password);
				}

			} catch (Throwable t) {

				logException(entity, t, sources);

			}

			return "";

		} else {

			logParameterError(entity, sources, ctx.isJavaScriptContext());

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
