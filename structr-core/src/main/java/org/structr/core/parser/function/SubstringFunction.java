package org.structr.core.parser.function;

import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class SubstringFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_SUBSTRING = "Usage: ${substring(string, start, length)}. Example: ${substring(this.name, 19, 3)}";

	@Override
	public String getName() {
		return "substring()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		if (arrayHasMinLengthAndAllElementsNotNull(sources, 2)) {

			final String source = sources[0].toString();
			final int sourceLength = source.length();
			final int start = parseInt(sources[1]);
			final int length = sources.length >= 3 ? parseInt(sources[2]) : sourceLength - start;
			final int end = start + length;

			if (start >= 0 && start < sourceLength && end >= 0 && end <= sourceLength && start <= end) {

				return source.substring(start, end);
			}
		}

		return "";
	}


	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_SUBSTRING;
	}

	@Override
	public String shortDescription() {
		return "Returns the substring of the given string";
	}

}
