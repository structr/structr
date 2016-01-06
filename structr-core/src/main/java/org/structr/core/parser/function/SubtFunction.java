package org.structr.core.parser.function;

import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class SubtFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_SUBT = "Usage: ${subt(value1, value2)}. Example: ${subt(5, 2)}";

	@Override
	public String getName() {
		return "subt()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		if (arrayHasMinLengthAndAllElementsNotNull(sources, 2)) {

			try {

				Double result = Double.parseDouble(sources[0].toString());

				for (int i = 1; i < sources.length; i++) {

					result -= Double.parseDouble(sources[i].toString());

				}

				return result;

			} catch (Throwable t) {

				return t.getMessage();

			}
		}

		return "";

	}


	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_SUBT;
	}

	@Override
	public String shortDescription() {
		return "Substracts the second argument from the first argument";
	}

}
