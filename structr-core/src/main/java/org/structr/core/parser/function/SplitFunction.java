package org.structr.core.parser.function;

import java.util.Arrays;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 * @author Christian Morgner
 */
public class SplitFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_SPLIT = "Usage: ${split(value)}. Example: ${split(this.commaSeparatedItems)}";

	@Override
	public String getName() {
		return "split()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		if (arrayHasMinLengthAndAllElementsNotNull(sources, 1)) {

			final String toSplit = sources[0].toString();
			String splitExpr = "[,;]+";

			if (sources.length >= 2) {
				splitExpr = sources[1].toString();
			}

			return Arrays.asList(toSplit.split(splitExpr));
		}

		return "";
	}


	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_SPLIT;
	}

	@Override
	public String shortDescription() {
		return "Splits the given string";
	}


}
