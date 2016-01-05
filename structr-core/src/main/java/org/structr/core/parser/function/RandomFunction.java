package org.structr.core.parser.function;

import org.apache.commons.lang3.RandomStringUtils;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class RandomFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_RANDOM = "Usage: ${random(num)}. Example: ${set(this, \"password\", random(8))}";

	@Override
	public String getName() {
		return "random()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		if (arrayHasLengthAndAllElementsNotNull(sources, 1) && sources[0] instanceof Number) {

			try {
				return RandomStringUtils.randomAlphanumeric(((Number)sources[0]).intValue());

			} catch (Throwable t) {
				// ignore
			}
		}

		return "";
	}


	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_RANDOM;
	}

	@Override
	public String shortDescription() {
		return "Returns a random alphanumeric string of the given length";
	}

}
