package org.structr.core.parser.function;

import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class UpperFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_UPPER = "Usage: ${upper(string)}. Example: ${upper(this.nickName)}";

	@Override
	public String getName() {
		return "upper()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		return (arrayHasMinLengthAndAllElementsNotNull(sources, 1))
			? sources[0].toString().toUpperCase()
			: "";
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_UPPER;
	}

	@Override
	public String shortDescription() {
		return "Returns the uppercase value of its parameter";
	}
}
