package org.structr.core.parser.function;

import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class AddFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_ADD = "Usage: ${add(values...)}. Example: ${add(1, 2, 3, this.children.size)}";

	@Override
	public String getName() {
		return "add()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		Double result = 0.0;

		if (sources != null) {

			for (Object i : sources) {

				if (i != null) {

					try {

						result += Double.parseDouble(i.toString());

					} catch (Throwable t) {

						return t.getMessage();

					}

				} else {

					result += 0.0;
				}
			}

		}

		return result;

	}


	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_ADD;
	}

	@Override
	public String shortDescription() {
		return "Returns the sum of the given arguments";
	}
}
