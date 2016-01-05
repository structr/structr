package org.structr.core.parser.function;

import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class NotFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_NOT = "Usage: ${not(bool1, bool2)}. Example: ${not(\"true\", \"true\")}";

	@Override
	public String getName() {
		return "not()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		if (arrayHasMinLengthAndAllElementsNotNull(sources, 1)) {

			return !("true".equals(sources[0].toString()) || Boolean.TRUE.equals(sources[0]));

		}

		return true;
	}


	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_NOT;
	}

	@Override
	public String shortDescription() {
		return "Negates the given arguments";
	}

}
