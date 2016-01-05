package org.structr.core.parser.function;

import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class AndFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_AND = "Usage: ${and(bool1, bool2)}. Example: ${and(\"true\", \"true\")}";

	@Override
	public String getName() {
		return "and()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		boolean result = true;

		if (sources != null) {

			if (sources.length < 2) {
				return usage(ctx.isJavaScriptContext());
			}

			for (Object i : sources) {

				if (i != null) {

					try {

						result &= "true".equals(i.toString()) || Boolean.TRUE.equals(i);

					} catch (Throwable t) {

						return t.getMessage();

					}

				} else {

					// null is false
					return false;
				}
			}

		}

		return result;
	}


	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_AND;
	}

	@Override
	public String shortDescription() {
		return "Returns the conjunction of the given arguments";
	}

}
