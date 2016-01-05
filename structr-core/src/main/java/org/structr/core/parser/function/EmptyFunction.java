package org.structr.core.parser.function;

import org.apache.commons.lang3.StringUtils;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class EmptyFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_EMPTY = "Usage: ${empty(string)}. Example: ${if(empty(possibleEmptyString), \"empty\", \"non-empty\")}";

	@Override
	public String getName() {
		return "empty()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		if (sources.length == 0 || sources[0] == null || StringUtils.isEmpty(sources[0].toString())) {

			return true;

		} else {
			return false;
		}

	}


	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_EMPTY;
	}

	@Override
	public String shortDescription() {
		return "Returns true if the given string or collection is null or empty";
	}

}
