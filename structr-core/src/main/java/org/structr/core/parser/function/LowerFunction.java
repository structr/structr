package org.structr.core.parser.function;

import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class LowerFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_LOWER = "Usage: ${lower(string)}. Example: ${lower(this.email)}";

	@Override
	public String getName() {
		return "lower()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		return (arrayHasMinLengthAndAllElementsNotNull(sources, 1))
			? sources[0].toString().toLowerCase()
			: "";
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_LOWER;
	}

	@Override
	public String shortDescription() {
		return "Returns the lowercase value of its parameter";
	}
}
