package org.structr.core.parser.function;

import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class UrlEncodeFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_URLENCODE = "Usage: ${urlencode(string)}. Example: ${urlencode(this.email)}";

	@Override
	public String getName() {
		return "urlencode()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		return (arrayHasMinLengthAndAllElementsNotNull(sources, 1))
			? encodeURL(sources[0].toString())
			: "";

	}


	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_URLENCODE;
	}

	@Override
	public String shortDescription() {
		return "URL-encodes the given string";
	}


}
