package org.structr.core.parser.function;

import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class IncCounterFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_INC_COUNTER = "Usage: ${inc_counter(level, [resetLowerLevels])}. Example: ${inc_counter(1, true)}";

	@Override
	public String getName() {
		return "inc_counter()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		if (arrayHasMinLengthAndAllElementsNotNull(sources, 1)) {

			final int level = parseInt(sources[0]);

			ctx.incrementCounter(level);

			// reset lower levels?
			if (sources.length == 2 && "true".equals(sources[1].toString())) {

				// reset lower levels
				for (int i = level + 1; i < 10; i++) {
					ctx.resetCounter(i);
				}
			}
		}

		return "";
	}


	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_INC_COUNTER;
	}

	@Override
	public String shortDescription() {
		return "Increases the value of the counter with the given index";
	}

}
