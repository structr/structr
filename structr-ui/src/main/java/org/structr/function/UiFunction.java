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
import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObjectMap;
import org.structr.core.property.IntProperty;
import org.structr.core.property.StringProperty;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;
import static org.structr.web.entity.dom.DOMNode.extractHeaders;

/**
 *
 */
public abstract class UiFunction extends Function<Object, Object> {

	protected String getFromUrl(final ActionContext ctx, final String requestUrl, final String username, final String password) throws IOException {

		final HttpClientParams params = new HttpClientParams(HttpClientParams.getDefaultParams());
		final HttpClient client = new HttpClient(params);
		final GetMethod getMethod = new GetMethod(requestUrl);

		if (username != null && password != null) {

			Credentials defaultcreds = new UsernamePasswordCredentials(username, password);
			client.getState().setCredentials(AuthScope.ANY, defaultcreds);
			client.getParams().setAuthenticationPreemptive(true);

			getMethod.setDoAuthentication(true);
		}

		getMethod.addRequestHeader("Connection", "close");

		// add request headers from context
		for (final Map.Entry<String, String> header : ctx.getHeaders().entrySet()) {
			getMethod.addRequestHeader(header.getKey(), header.getValue());
		}

		client.executeMethod(getMethod);

		return getMethod.getResponseBodyAsString();

	}

	protected GraphObjectMap headFromUrl(final ActionContext ctx, final String requestUrl, final String username, final String password) throws IOException, FrameworkException {

		final HttpClientParams params = new HttpClientParams(HttpClientParams.getDefaultParams());
		final HttpClient client = new HttpClient(params);
		final HeadMethod headMethod = new HeadMethod(requestUrl);

		if (username != null && password != null) {

			Credentials defaultcreds = new UsernamePasswordCredentials(username, password);
			client.getState().setCredentials(AuthScope.ANY, defaultcreds);
			client.getParams().setAuthenticationPreemptive(true);

			headMethod.setDoAuthentication(true);
		}

		headMethod.addRequestHeader("Connection", "close");
		// Don't follow redirects automatically, return status code 302 etc. instead
		headMethod.setFollowRedirects(false);

		// add request headers from context
		for (final Map.Entry<String, String> header : ctx.getHeaders().entrySet()) {
			headMethod.addRequestHeader(header.getKey(), header.getValue());
		}

		client.executeMethod(headMethod);

		final GraphObjectMap response = new GraphObjectMap();
		response.setProperty(new IntProperty("status"), headMethod.getStatusCode());
		response.setProperty(new StringProperty("headers"), extractHeaders(headMethod.getResponseHeaders()));

		return response;

	}
}
