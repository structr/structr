package org.structr.core.parser.function;

import java.util.List;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class LastFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_LAST = "Usage: ${last(collection)}. Example: ${last(this.children)}";

	@Override
	public String getName() {
		return "last()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		if (arrayHasLengthAndAllElementsNotNull(sources, 1)) {

			if (sources[0] instanceof List && !((List)sources[0]).isEmpty()) {

				final List list = (List)sources[0];
				return list.get(list.size() - 1);
			}

			if (sources[0].getClass().isArray()) {

				final Object[] arr = (Object[])sources[0];
				if (arr.length > 0) {

					return arr[arr.length - 1];
				}
			}

		}

		return null;
	}


	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_LAST;
	}

	@Override
	public String shortDescription() {
		return "Returns the last element of the given collection";
	}

}
