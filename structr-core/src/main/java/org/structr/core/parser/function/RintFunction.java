package org.structr.core.parser.function;

import java.util.Random;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class RintFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_RINT = "Usage: ${rint(range)}. Example: ${rint(1000)}";

	@Override
	public String getName() {
		return "rint()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		if (arrayHasLengthAndAllElementsNotNull(sources, 1) && sources[0] instanceof Number) {

			try {
				return new Random(System.currentTimeMillis()).nextInt(((Number)sources[0]).intValue());

			} catch (Throwable t) {
				// ignore
			}
		}

		return "";
	}


	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_RINT;
	}

	@Override
	public String shortDescription() {
		return "Returns a random integer in the given range";
	}

}
