package org.structr.core.parser.function;

import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class ModFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_MODULO = "Usage: ${mod(value1, value2)}. Example: ${mod(17, 5)}";

	@Override
	public String getName() {
		return "mod()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		if (arrayHasLengthAndAllElementsNotNull(sources, 2)) {

			try {

				return ((int)Double.parseDouble(sources[0].toString())) % ((int)Double.parseDouble(sources[1].toString()));

			} catch (Throwable t) {

				return t.getMessage();

			}

		} else {

			return usage(ctx.isJavaScriptContext());

		}

	}


	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_MODULO;
	}

	@Override
	public String shortDescription() {
		return "Returns the remainder of the division";
	}

}
