package org.structr.function;

import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;

/**
 *
 */
public class HeadFunction extends UiFunction {

	public static final String ERROR_MESSAGE_HEAD    = "Usage: ${HEAD(URL[, username, password])}. Example: ${HEAD('http://structr.org', 'foo', 'bar')}";
	public static final String ERROR_MESSAGE_HEAD_JS = "Usage: ${{Structr.HEAD(URL[, username, password]])}}. Example: ${{Structr.HEAD('http://structr.org', 'foo', 'bar')}}";

	@Override
	public String getName() {
		return "HEAD()";
	}

	@Override
	public Object apply(ActionContext ctx, final GraphObject entity, final Object[] sources) {

		if (sources != null && sources.length > 0) {

			try {

				String address = sources[0].toString();
				String username = null;
				String password = null;

				if (sources.length > 1) {
					username = sources[1].toString();
				}

				if (sources.length > 2) {
					password = sources[2].toString();
				}

				return headFromUrl(ctx, address, username, password);

			} catch (Throwable t) {
				t.printStackTrace();
			}

			return "";
		}

		return usage(ctx.isJavaScriptContext());
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_HEAD_JS : ERROR_MESSAGE_HEAD);
	}

	@Override
	public String shortDescription() {
		return "Sends an HTTP HEAD request to the given URL and returns the response headers";
	}

}
