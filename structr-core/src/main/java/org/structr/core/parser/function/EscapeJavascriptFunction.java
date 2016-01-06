package org.structr.core.parser.function;

import org.apache.commons.lang3.StringEscapeUtils;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class EscapeJavascriptFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_ESCAPE_JS = "Usage: ${escape_javascript(string)}. Example: ${escape_javascript(this.name)}";

	@Override
	public String getName() {
		return "escape_javascript()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		return (arrayHasMinLengthAndAllElementsNotNull(sources, 1))
			? StringEscapeUtils.escapeEcmaScript(sources[0].toString())
			: "";
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_ESCAPE_JS;
	}

	@Override
	public String shortDescription() {
		return "Escapes the given string for use with Javascript";
	}


}
