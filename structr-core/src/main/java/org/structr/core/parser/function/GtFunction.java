package org.structr.core.parser.function;

import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class GtFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_GT = "Usage: ${gt(value1, value2)}. Example: ${if(gt(this.children, 2), \"More than two\", \"Equal to or less than two\")}";

	@Override
	public String getName() {
		return "gt()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		return gt(sources[0], sources[1]);
	}


	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_GT;
	}

	@Override
	public String shortDescription() {
		return "Returns true if the first argument is greater than the second argument";
	}

}
