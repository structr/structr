package org.structr.core.parser.function;

import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class LengthFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_LENGTH = "Usage: ${length(string)}. Example: ${length(this.name)}";

	@Override
	public String getName() {
		return "length()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		if (arrayHasLengthAndAllElementsNotNull(sources, 1)) {

			return sources[0].toString().length();
		}

		return "";
	}


	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_LENGTH;
	}

	@Override
	public String shortDescription() {
		return "Returns the length of the given string";
	}

}
