package org.structr.core.parser.function;

import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class LteFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_LTE = "Usage: ${lte(value1, value2)}. Example: ${if(lte(this.children, 2), \"Equal to or less than two\", \"More than two\")}";

	@Override
	public String getName() {
		return "lte()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		return lte(sources[0], sources[1]);
	}


	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_LTE;
	}

	@Override
	public String shortDescription() {
		return "Returns true if the first argument is less or equal to the second argument";
	}

}
