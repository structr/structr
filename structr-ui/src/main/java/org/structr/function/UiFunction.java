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
