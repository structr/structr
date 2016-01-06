package org.structr.core.parser.function;

import org.apache.commons.lang3.StringUtils;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class RoundFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_ROUND = "Usage: ${round(value1 [, decimalPlaces])}. Example: ${round(2.345678, 2)}";

	@Override
	public String getName() {
		return "round()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		if (arrayHasLengthAndAllElementsNotNull(sources, 2)) {

			if (StringUtils.isBlank(sources[0].toString())) {
				return "";
			}

			try {

				Double f1 = Double.parseDouble(sources[0].toString());
				double f2 = Math.pow(10, (Double.parseDouble(sources[1].toString())));
				long r = Math.round(f1 * f2);

				return (double)r / f2;

			} catch (Throwable t) {

				return t.getMessage();

			}

		} else {

			return "";
		}
	}


	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_ROUND;
	}

	@Override
	public String shortDescription() {
		return "Rounds the given argument to an integer";
	}

}
