package org.structr.core.parser.function;

import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class NumFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_NUM = "Usage: ${num(string)}. Example: ${num(this.numericalStringValue)}";

	@Override
	public String getName() {
		return "num()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		if (arrayHasMinLengthAndAllElementsNotNull(sources, 1)) {

			try {
				return getDoubleOrNull(sources[0]);

			} catch (Throwable t) {
				// ignore
			}
		}

		return "";
	}


	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_NUM;
	}

	@Override
	public String shortDescription() {
		return "Converts the given string to a flaoting-point number";
	}

}
