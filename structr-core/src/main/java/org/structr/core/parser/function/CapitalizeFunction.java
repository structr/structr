package org.structr.core.parser.function;

import org.apache.commons.lang3.StringUtils;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class CapitalizeFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_CAPITALIZE = "Usage: ${capitalize(string)}. Example: ${capitalize(this.nickName)}";

	@Override
	public String getName() {
		return "capitalize()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		return (arrayHasMinLengthAndAllElementsNotNull(sources, 1))
			? StringUtils.capitalize(sources[0].toString())
			: "";

	}


	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_CAPITALIZE;
	}

	@Override
	public String shortDescription() {
		return "Capitalizes the given string";
	}
}
