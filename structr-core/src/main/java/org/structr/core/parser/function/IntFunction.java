package org.structr.core.parser.function;

import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class IntFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_INT = "Usage: ${int(string)}. Example: ${int(this.numericalStringValue)}";

	@Override
	public String getName() {
		return "int()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		if (arrayHasMinLengthAndAllElementsNotNull(sources, 1)) {

			if (sources[0] instanceof Number) {
				return ((Number)sources[0]).intValue();
			}

			try {
				return getDoubleOrNull(sources[0]).intValue();

			} catch (Throwable t) {
				// ignore
			}
		}

		return "";
	}


	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_INT;
	}

	@Override
	public String shortDescription() {
		return "Converts the given string to an integer";
	}

}
