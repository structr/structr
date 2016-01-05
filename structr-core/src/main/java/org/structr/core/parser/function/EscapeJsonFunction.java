package org.structr.core.parser.function;

import org.apache.commons.lang3.StringEscapeUtils;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class EscapeJsonFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_ESCAPE_JSON = "Usage: ${escape_json(string)}. Example: ${escape_json(this.name)}";

	@Override
	public String getName() {
		return "escape_json()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		return (arrayHasMinLengthAndAllElementsNotNull(sources, 1))
			? StringEscapeUtils.escapeJson(sources[0].toString())
			: "";
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_ESCAPE_JSON;
	}

	@Override
	public String shortDescription() {
		return "Escapes the given string for use within JSON";
	}

}
