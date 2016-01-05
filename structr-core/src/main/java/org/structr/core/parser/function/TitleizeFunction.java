package org.structr.core.parser.function;

import org.apache.commons.lang3.StringUtils;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class TitleizeFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_TITLEIZE = "Usage: ${titleize(string, separator}. (Default separator is \" \") Example: ${titleize(this.lowerCamelCaseString, \"_\")}";

	@Override
	public String getName() {
		return "titleize()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		if (sources == null || sources[0] == null) {
			return null;
		}

		if (StringUtils.isBlank(sources[0].toString())) {
			return "";
		}

		final String separator;
		if (sources.length < 2) {
			separator = " ";
		} else {
			separator = sources[1].toString();
		}

		String[] in = StringUtils.split(sources[0].toString(), separator);
		String[] out = new String[in.length];
		for (int i = 0; i < in.length; i++) {
			out[i] = StringUtils.capitalize(in[i]);
		}
		return StringUtils.join(out, " ");

	}


	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_TITLEIZE;
	}

	@Override
	public String shortDescription() {
		return "Titleizes the given string";
	}

}
