package org.structr.function;

import javax.servlet.http.HttpServletResponse;
import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;

/**
 *
 */
public class SetResponseHeaderFunction extends UiFunction {

	public static final String ERROR_MESSAGE_SET_RESPONSE_HEADER    = "Usage: ${set_response_header(field, value)}. Example: ${set_response_header('X-User', 'johndoe')}";
	public static final String ERROR_MESSAGE_SET_RESPONSE_HEADER_JS = "Usage: ${{Structr.setResponseHeader(field, value)}}. Example: ${{Structr.setResponseHeader('X-User', 'johndoe')}}";

	@Override
	public String getName() {
		return "set_response_header()";
	}

	@Override
	public Object apply(ActionContext ctx, final GraphObject entity, final Object[] sources) {

		if (sources != null && sources.length == 2) {

			final String name = sources[0].toString();
			final String value = sources[1].toString();

			final SecurityContext securityContext = ctx.getSecurityContext();
			if (securityContext != null) {

				final HttpServletResponse response = securityContext.getResponse();
				if (response != null) {

					response.addHeader(name, value);
				}
			}

			return "";
		}

		return usage(ctx.isJavaScriptContext());
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_SET_RESPONSE_HEADER_JS : ERROR_MESSAGE_SET_RESPONSE_HEADER);
	}

	@Override
	public String shortDescription() {
		return "Adds the given header field and value to the response of the current rendering run";
	}

}
