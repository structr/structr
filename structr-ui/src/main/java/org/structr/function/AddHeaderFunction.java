package org.structr.function;

import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;

/**
 *
 */
public class AddHeaderFunction extends UiFunction {

	public static final String ERROR_MESSAGE_ADD_HEADER    = "Usage: ${add_header(field, value)}. Example: ${add_header('X-User', 'johndoe')}";
	public static final String ERROR_MESSAGE_ADD_HEADER_JS = "Usage: ${{Structr.add_header(field, value)}}. Example: ${{Structr.add_header('X-User', 'johndoe')}}";

	@Override
	public String getName() {
		return "add_header()";
	}

	@Override
	public Object apply(ActionContext ctx, final GraphObject entity, final Object[] sources) {

		if (sources != null && sources.length == 2) {

			final String name = sources[0].toString();
			final String value = sources[1].toString();

			ctx.addHeader(name, value);

			return "";
		}

		return usage(ctx.isJavaScriptContext());
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_ADD_HEADER_JS : ERROR_MESSAGE_ADD_HEADER);
	}

	@Override
	public String shortDescription() {
		return "Adds the given header field and value to the next request";
	}

}
