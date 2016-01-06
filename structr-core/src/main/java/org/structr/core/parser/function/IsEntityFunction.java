package org.structr.core.parser.function;

import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class IsEntityFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_IS_ENTITY = "Usage: ${is_entity(value)}. Example: ${is_entity(this)}";

	@Override
	public String getName() {
		return "is_entity()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		if (arrayHasLengthAndAllElementsNotNull(sources, 1)) {
			return (sources[0] instanceof GraphObject);
		} else {
			return false;
		}

	}


	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_IS_ENTITY;
	}

	@Override
	public String shortDescription() {
		return "Returns true if the given argument is a Structr entity";
	}

}
