package org.structr.core.parser.function;

import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class CeilFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_CEIL = "Usage: ${ceil(value)}. Example: ${ceil(32.4)}";

	@Override
	public String getName() {
		return "ceil()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		if (arrayHasLengthAndAllElementsNotNull(sources, 1)) {

			return (int)Math.ceil(Double.parseDouble(sources[0].toString()));

		} else {

			return usage(ctx.isJavaScriptContext());

		}

	}


	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_CEIL;
	}

	@Override
	public String shortDescription() {
		return "Returns the smallest integer that is greater than or equal to the argument";
	}

}
