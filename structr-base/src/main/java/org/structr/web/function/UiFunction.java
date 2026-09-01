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
import org.structr.rest.common.HttpHelper;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

import java.io.InputStream;
import java.util.Map;

/**
 *
 */
public abstract class UiFunction extends Function<Object, Object> {

	/**
	 * The response every outbound HTTP function returns: body, status and headers, in one shape.
	 *
	 * Built here rather than in each verb so the shape cannot drift apart again - HEAD used to hand back
	 * the raw map from HttpHelper, where the status is a String, so `r.status === 200` was true after a
	 * POST and false after a HEAD.
	 *
	 * The body is parsed as JSON only when the caller asked for it. Inferring that from a content type
	 * would make the return type depend on the server rather than on the call.
	 */
	protected GraphObjectMap buildResponse(final ActionContext ctx, final Object caller, final Map<String, Object> responseData, final boolean parseResponse) throws FrameworkException {

		final GraphObjectMap response = new GraphObjectMap();
		final Object responseBody     = responseData.get(HttpHelper.FIELD_BODY);

		if (parseResponse) {

			response.setProperty(new GenericProperty(HttpHelper.FIELD_BODY), new FromJsonFunction().apply(ctx, caller, new Object[] { responseBody }));

		} else if (responseBody instanceof InputStream stream) {

			response.setProperty(new GenericProperty<InputStream>(HttpHelper.FIELD_BODY), stream);

		} else {

			response.setProperty(new StringProperty(HttpHelper.FIELD_BODY), responseBody != null ? responseBody.toString() : null);
		}

		final Object status = responseData.get(HttpHelper.FIELD_STATUS);

		response.setProperty(new IntProperty(HttpHelper.FIELD_STATUS), status != null ? Integer.parseInt(status.toString()) : 0);

		if (responseData.get(HttpHelper.FIELD_HEADERS) instanceof Map map) {

			response.setProperty(new GenericProperty<Map<String, String>>(HttpHelper.FIELD_HEADERS), GraphObjectMap.fromMap(map));
		}

		return response;
	}

	protected Map<String, Object> getFromUrl(final ActionContext ctx, final String requestUrl, final String charset, final String username, final String password) throws FrameworkException {

		return HttpHelper.get(requestUrl, charset, username, password, ctx.getHeaders(), ctx.isValidateCertificates());
	}

	protected Map<String, Object> getBinaryFromUrl(final ActionContext ctx, final String requestUrl, final String charset, final String username, final String password) throws FrameworkException {

		return HttpHelper.getBinary(requestUrl, charset, username, password, ctx.getHeaders(), ctx.isValidateCertificates());
	}

	protected Map<String, Object> getStreamFromUrl(final ActionContext ctx, final String requestUrl, final String charset, final String username, final String password, final Map<String, String> headers, final boolean validateCertificates, final Map<String, Object> config) throws FrameworkException {

		// the caller's merged headers rather than the ActionContext's alone: preemptive basic auth is added
		// by HttpOptions.mergeHeaders, so passing ctx.getHeaders() here silently disabled it
		return HttpHelper.getAsStream(requestUrl, charset, username, password, null, null, null, null, headers, validateCertificates, config);
	}

}
