package org.structr.core.parser.function;

import org.apache.commons.lang3.StringUtils;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class AbbrFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_ABBR = "Usage: ${abbr(longString, maxLength)}. Example: ${abbr(this.title, 20)}";

	@Override
	public String getName() {
		return "abbr()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		if (arrayHasMinLengthAndAllElementsNotNull(sources, 2)) {

			try {
				int maxLength = Double.valueOf(sources[1].toString()).intValue();

				if (sources[0].toString().length() > maxLength) {

					return StringUtils.substringBeforeLast(StringUtils.substring(sources[0].toString(), 0, maxLength), " ").concat("…");

				} else {

					return sources[0];
				}

			} catch (NumberFormatException nfe) {

				return nfe.getMessage();

			}

		}

		return "";

	}


	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_ABBR;
	}

	@Override
	public String shortDescription() {
		return "Abbreviates the given string";
	}

}
