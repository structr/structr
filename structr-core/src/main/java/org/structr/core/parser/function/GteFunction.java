package org.structr.core.parser.function;

import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class GteFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_GTE = "Usage: ${gte(value1, value2)}. Example: ${if(gte(this.children, 2), \"Equal to or more than two\", \"Less than two\")}";

	@Override
	public String getName() {
		return "gte()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		return gte(sources[0], sources[1]);
	}


	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_GTE;
	}

	@Override
	public String shortDescription() {
		return "Returns true if the first argument is greater or equal to the second argument";
	}

}
