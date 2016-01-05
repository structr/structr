package org.structr.core.parser.function;

import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class IndexOfFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_INDEX_OF = "Usage: ${index_of(string, word)}. Example: ${index_of(this.name, \"the\")}";

	@Override
	public String getName() {
		return "index_of()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		if (arrayHasLengthAndAllElementsNotNull(sources, 2)) {

			final String source = sources[0].toString();
			final String part = sources[1].toString();

			return source.indexOf(part);
		}

		return "";
	}


	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_INDEX_OF;
	}

	@Override
	public String shortDescription() {
		return "Returns the position of string in a string, or -1";
	}

}
