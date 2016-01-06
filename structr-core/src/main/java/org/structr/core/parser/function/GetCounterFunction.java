package org.structr.core.parser.function;

import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class GetCounterFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_GET_COUNTER = "Usage: ${get_counter(level)}. Example: ${get_counter(1)}";

	@Override
	public String getName() {
		return "get_counter()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		if (arrayHasLengthAndAllElementsNotNull(sources, 1)) {

			return ctx.getCounter(parseInt(sources[0]));
		}

		return 0;
	}


	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_GET_COUNTER;
	}

	@Override
	public String shortDescription() {
		return "Returns the value of the counter with the given index";
	}

}
