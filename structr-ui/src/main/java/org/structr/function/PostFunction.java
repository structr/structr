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

import java.io.IOException;
import java.util.Map;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.GraphObjectMap;
import org.structr.core.property.IntProperty;
import org.structr.core.property.StringProperty;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;
import static org.structr.web.entity.dom.DOMNode.extractHeaders;

/**
 *
 */
public class PostFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_POST    = "Usage: ${POST(URL, body [, contentType, charset])}. Example: ${POST('http://localhost:8082/structr/rest/folders', '{name:Test}', 'application/json', 'utf-8')}";
	public static final String ERROR_MESSAGE_POST_JS = "Usage: ${{Structr.POST(URL, body [, contentType, charset])}}. Example: ${{Structr.POST('http://localhost:8082/structr/rest/folders', '{name:\"Test\"}', 'application/json', 'utf-8')}}";

	@Override
	public String getName() {
		return "POST()";
	}

	@Override
	public Object apply(ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		if (arrayHasMinLengthAndAllElementsNotNull(sources, 2)) {

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

			final HttpClientParams params = new HttpClientParams(HttpClientParams.getDefaultParams());
			final HttpClient client = new HttpClient(params);
			final PostMethod postMethod = new PostMethod(uri);

			// add request headers from context
			for (final Map.Entry<String, String> header : ctx.getHeaders().entrySet()) {
				postMethod.addRequestHeader(header.getKey(), header.getValue());
			}

			try {

				postMethod.setRequestEntity(new StringRequestEntity(body, contentType, charset));

				final int statusCode = client.executeMethod(postMethod);
				final String responseBody = postMethod.getResponseBodyAsString();

				final GraphObjectMap response = new GraphObjectMap();

				if ("application/json".equals(contentType)) {

					final FromJsonFunction fromJsonFunction = new FromJsonFunction();
					response.setProperty(new StringProperty("body"), fromJsonFunction.apply(ctx, entity, new Object[]{responseBody}));

				} else {

					response.setProperty(new StringProperty("body"), responseBody);

				}

				response.setProperty(new IntProperty("status"), statusCode);
				response.setProperty(new StringProperty("headers"), extractHeaders(postMethod.getResponseHeaders()));

				return response;

			} catch (IOException ioex) {
				ioex.printStackTrace();
			}

		} else {

			return usage(ctx.isJavaScriptContext());
		}

		return null;
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_POST_JS : ERROR_MESSAGE_POST);
	}

	@Override
	public String shortDescription() {
		return "Sends an HTTP POST request to the given URL and returns the response body";
	}

}
