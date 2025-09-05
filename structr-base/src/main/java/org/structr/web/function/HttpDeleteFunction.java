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

import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObjectMap;
import org.structr.core.property.GenericProperty;
import org.structr.core.property.IntProperty;
import org.structr.core.property.StringProperty;
import org.structr.rest.common.HttpHelper;
import org.structr.schema.action.ActionContext;

import java.util.Map;

public class HttpDeleteFunction extends UiAdvancedFunction {

	public static final String ERROR_MESSAGE_DELETE    = "Usage: ${DELETE(URL[, contentType])}. Example: ${DELETE('http://localhost:8082/structr/rest/folders/6aa10d68569d45beb384b42a1fc78c50', 'application/json')}";
	public static final String ERROR_MESSAGE_DELETE_JS = "Usage: ${{Structr.DELETE(URL[, contentType])}}. Example: ${{Structr.DELETE('http://localhost:8082/structr/rest/folders/6aa10d68569d45beb384b42a1fc78c50', 'application/json')}}";

	@Override
	public String getName() {
		return "DELETE";
	}

	@Override
	public String getSignature() {
		return "url [, contentType ]";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndAllElementsNotNull(sources, 1);

			final String uri = sources[0].toString();
			String contentType = "application/json";

			// override default content type
			if (sources.length >= 3 && sources[2] != null) {
				contentType = sources[2].toString();
			}

			final Map<String, Object> responseData = HttpHelper.delete(uri, null, null, ctx.getHeaders(), ctx.isValidateCertificates());

			final String responseBody = responseData.get(HttpHelper.FIELD_BODY) != null ? responseData.get(HttpHelper.FIELD_BODY).toString() : null;

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
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_DELETE_JS : ERROR_MESSAGE_DELETE);
	}

	@Override
	public String shortDescription() {
		return "Sends an HTTP DELETE request to the given URL and returns the response body";
	}
}
