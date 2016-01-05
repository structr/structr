package org.structr.core.parser.function;

import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class StoreFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_STORE    = "Usage: ${store(key, value)}. Example: ${store('tmpUser', this.owner)}";
	public static final String ERROR_MESSAGE_STORE_JS = "Usage: ${{Structr.store(key, value)}}. Example: ${{Structr.store('tmpUser', Structr.get('this').owner)}}";

	@Override
	public String getName() {
		return "store()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		if (sources.length > 0 && sources[0] != null && sources[0] instanceof String) {

			if (sources[1] != null) {

				ctx.store(sources[0].toString(), sources[1]);
			}

		} else {

			return usage(ctx.isJavaScriptContext());
		}

		return "";
	}


	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_STORE_JS : ERROR_MESSAGE_STORE);
	}

	@Override
	public String shortDescription() {
		return "Stores the given value with the given key in the temporary store";
	}

}
