package org.structr.core.parser.function;

import java.util.List;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class NthFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_NTH = "Usage: ${nth(collection)}. Example: ${nth(this.children, 2)}";

	@Override
	public String getName() {
		return "nth()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		if (arrayHasLengthAndAllElementsNotNull(sources, 2)) {

			final int pos = Double.valueOf(sources[1].toString()).intValue();

			if (sources[0] instanceof List && !((List)sources[0]).isEmpty()) {

				final List list = (List)sources[0];
				final int size = list.size();

				if (pos >= size) {

					return null;

				}

				return list.get(Math.min(Math.max(0, pos), size - 1));
			}

			if (sources[0].getClass().isArray()) {

				final Object[] arr = (Object[])sources[0];
				if (pos <= arr.length) {

					return arr[pos];
				}
			}
		}

		return null;
	}


	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_NTH;
	}

	@Override
	public String shortDescription() {
		return "Returns the element with the given index of the given collection";
	}
}
