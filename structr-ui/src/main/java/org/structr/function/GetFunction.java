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
package org.structr.function;

import java.util.Map;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;

/**
 *
 */
public class GetFunction extends UiFunction {

	public static final String ERROR_MESSAGE_GET    = "Usage: ${GET(URL[, contentType[, selector]])}. Example: ${GET('http://structr.org', 'text/html')}";
	public static final String ERROR_MESSAGE_GET_JS = "Usage: ${{Structr.GET(URL[, contentType[, selector]])}}. Example: ${{Structr.HEAD('http://structr.org', 'text/html')}}";

	@Override
	public String getName() {
		return "GET()";
	}

	@Override
	public Object apply(ActionContext ctx, final GraphObject entity, final Object[] sources) {

		if (sources != null && sources.length > 0) {

			try {

				String address = sources[0].toString();
				String contentType = null;
				String username = null;
				String password = null;

				if (sources.length > 1) {
					contentType = sources[1].toString();
				}

				if (sources.length > 2) {
					username = sources[2].toString();
				}

				if (sources.length > 3) {
					password = sources[3].toString();
				}

				//long t0 = System.currentTimeMillis();
				if ("text/html".equals(contentType)) {

					final Connection connection = Jsoup.connect(address);

					// add request headers from context
					for (final Map.Entry<String, String> header : ctx.getHeaders().entrySet()) {
						connection.header(header.getKey(), header.getValue());
					}

					if (sources.length > 2) {

						return connection.get().select(sources[2].toString()).html();

					} else {

						return connection.get().html();
					}

				} else {

					return getFromUrl(ctx, address, username, password);
				}

			} catch (Throwable t) {
				t.printStackTrace();
			}

			return "";
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
