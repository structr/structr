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
