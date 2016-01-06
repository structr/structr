package org.structr.core.parser.function;

import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class MaxFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_MAX = "Usage: ${max(value1, value2)}. Example: ${max(this.children, 10)}";

	@Override
	public String getName() {
		return "max()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		Object result = "";
		String errorMsg = "ERROR! Usage: ${max(val1, val2)}. Example: ${max(5,10)}";

		if (arrayHasLengthAndAllElementsNotNull(sources, 2)) {

			try {
				result = Math.max(Double.parseDouble(sources[0].toString()), Double.parseDouble(sources[1].toString()));

			} catch (Throwable t) {
				result = errorMsg;
			}

		} else {

			result = "";
		}

		return result;

	}


	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_MAX;
	}

	@Override
	public String shortDescription() {
		return "Returns the larger value of the given arguments";
	}

}
