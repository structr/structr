package org.structr.core.parser.function;

import java.util.Collection;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class IntSumFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_INT_SUM = "Usage: ${int_sum(list)}. Example: ${int_sum(extract(this.children, \"number\"))}";

	@Override
	public String getName() {
		return "int_sum()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		int result = 0;

		if (arrayHasLengthAndAllElementsNotNull(sources, 1)) {

			if (sources[0] instanceof Collection) {

				for (final Number num : (Collection<Number>)sources[0]) {

					result += num.intValue();
				}
			}
		}

		return result;
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_INT_SUM;
	}

	@Override
	public String shortDescription() {
		return "Returns the sum of the given arguments as an integer";
	}

}
