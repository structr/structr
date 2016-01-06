package org.structr.core.parser.function;

import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class LtFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_LT = "Usage: ${lt(value1, value2)}. Example: ${if(lt(this.children, 2), \"Less than two\", \"Equal to or more than two\")}";

	@Override
	public String getName() {
		return "lt()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		return lt(sources[0], sources[1]);
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_LT;
	}

	@Override
	public String shortDescription() {
		return "Returns true if the first argument is less than the second argument";
	}

}
