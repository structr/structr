package org.structr.core.parser.function;

import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class ResetCounterFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_RESET_COUNTER = "Usage: ${reset_counter(level)}. Example: ${reset_counter(1)}";

	@Override
	public String getName() {
		return "reset_counter()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		if (arrayHasLengthAndAllElementsNotNull(sources, 1)) {

			ctx.resetCounter(parseInt(sources[0]));
		}

		return "";
	}


	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_RESET_COUNTER;
	}

	@Override
	public String shortDescription() {
		return "Resets the value of the counter with the given index";
	}

}
