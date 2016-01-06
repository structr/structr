package org.structr.core.parser.function;

import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class OrFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_OR = "Usage: ${or(bool1, bool2)}. Example: ${or(\"true\", \"true\")}";

	@Override
	public String getName() {
		return "or()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		boolean result = false;

		if (sources != null) {

			if (sources.length < 2) {
				return usage(ctx.isJavaScriptContext());
			}

			for (Object i : sources) {

				if (i != null) {

					try {

						result |= "true".equals(i.toString()) || Boolean.TRUE.equals(i);

					} catch (Throwable t) {

						return t.getMessage();

					}

				} else {

					// null is false
					result |= false;
				}
			}

		}

		return result;
	}


	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_OR;
	}

	@Override
	public String shortDescription() {
		return "Returns the disjunction of the given arguments";
	}

}
