package org.structr.core.parser.function;

import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class MultFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_MULT = "Usage: ${mult(value1, value2)}. Example: ${mult(5, 2)}";

	@Override
	public String getName() {
		return "mult()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		Double result = 1.0d;

		if (sources != null) {

			for (Object i : sources) {

				try {

					result *= Double.parseDouble(i.toString());

				} catch (Throwable t) {

					return t.getMessage();

				}
			}

		}

		return result;

	}


	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_MULT;
	}

	@Override
	public String shortDescription() {
		return "Multiplies the first argument by the second argument";
	}

}
