package org.structr.core.parser.function;

import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class EqualFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_EQUAL = "Usage: ${equal(value1, value2)}. Example: ${equal(this.children.size, 0)}";

	@Override
	public String getName() {
		return "equal()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		if (sources.length < 2) {

			return true;
		}

		if (sources[0] == null && sources[1] == null) {
			return true;
		}

		if (sources[0] == null || sources[1] == null) {
			return false;
		}

		return valueEquals(sources[0], sources[1]);
	}


	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_EQUAL;
	}

	@Override
	public String shortDescription() {
		return "Returns true if the given arguments are equal";
	}

}
