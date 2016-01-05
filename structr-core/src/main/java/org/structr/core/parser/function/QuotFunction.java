package org.structr.core.parser.function;

import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class QuotFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_QUOT = "Usage: ${quot(value1, value2)}. Example: ${quot(5, 2)}";

	@Override
	public String getName() {
		return "quot()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		if (arrayHasLengthAndAllElementsNotNull(sources, 2)) {

			try {

				return Double.parseDouble(sources[0].toString()) / Double.parseDouble(sources[1].toString());

			} catch (Throwable t) {

				return t.getMessage();

			}

		} else {

			if (sources != null) {

				if (sources.length > 0 && sources[0] != null) {
					return Double.valueOf(sources[0].toString());
				}

				return "";
			}
		}

		return "";
	}


	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_QUOT;
	}

	@Override
	public String shortDescription() {
		return "Divides the first argument by the second argument";
	}

}
