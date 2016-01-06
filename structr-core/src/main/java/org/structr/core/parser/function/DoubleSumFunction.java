package org.structr.core.parser.function;

import java.util.Collection;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class DoubleSumFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_DOUBLE_SUM = "Usage: ${double_sum(list)}. Example: ${double_sum(extract(this.children, \"amount\"))}";

	@Override
	public String getName() {
		return "double_sum()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		double result = 0.0;

		if (arrayHasLengthAndAllElementsNotNull(sources, 1)) {

			if (sources[0] instanceof Collection) {

				for (final Number num : (Collection<Number>)sources[0]) {

					result += num.doubleValue();
				}
			}
		}

		return result;

	}


	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_DOUBLE_SUM;
	}

	@Override
	public String shortDescription() {
		return "Returns the sum of the given arguments as a floating-point number";
	}


}
