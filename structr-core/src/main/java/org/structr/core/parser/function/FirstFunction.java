package org.structr.core.parser.function;

import java.util.List;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class FirstFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_FIRST = "Usage: ${first(collection)}. Example: ${first(this.children)}";

	@Override
	public String getName() {
		return "first()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		if (arrayHasLengthAndAllElementsNotNull(sources, 1)) {

			if (sources[0] instanceof List && !((List)sources[0]).isEmpty()) {
				return ((List)sources[0]).get(0);
			}

			if (sources[0].getClass().isArray()) {

				final Object[] arr = (Object[])sources[0];
				if (arr.length > 0) {

					return arr[0];
				}
			}
		}

		return null;
	}


	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_FIRST;
	}

	@Override
	public String shortDescription() {
		return "Returns the first element of the given collection";
	}
}
