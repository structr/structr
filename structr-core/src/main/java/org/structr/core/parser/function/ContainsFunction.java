package org.structr.core.parser.function;

import java.util.Collection;
import org.apache.commons.lang3.ArrayUtils;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class ContainsFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_CONTAINS = "Usage: ${contains(string, word)}. Example: ${contains(this.name, \"the\")}";

	@Override
	public String getName() {
		return "contains()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		if (arrayHasLengthAndAllElementsNotNull(sources, 2)) {

			if (sources[0] instanceof String && sources[1] instanceof String) {

				final String source = sources[0].toString();
				final String part = sources[1].toString();

				return source.contains(part);

			} else if (sources[0] instanceof Collection) {

				final Collection collection = (Collection)sources[0];
				return collection.contains(sources[1]);

			} else if (sources[0].getClass().isArray()) {

				return ArrayUtils.contains((Object[])sources[0], sources[1]);
			}
		}

		return false;
	}


	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_CONTAINS;
	}

	@Override
	public String shortDescription() {
		return "Returns true if the given string or collection contains an element";
	}

}
