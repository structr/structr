package org.structr.core.parser.function;

import java.util.Collection;
import org.apache.commons.lang3.StringUtils;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class JoinFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_JOIN = "Usage: ${join(collection, separator)}. Example: ${join(this.names, \",\")}";

	@Override
	public String getName() {
		return "join()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		if (arrayHasLengthAndAllElementsNotNull(sources, 2)) {

			if (sources[0] instanceof Collection) {

				return StringUtils.join((Collection)sources[0], sources[1].toString());
			}

			if (sources[0].getClass().isArray()) {

				return StringUtils.join((Object[])sources[0], sources[1].toString());
			}
		}

		return "";
	}


	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_JOIN;
	}

	@Override
	public String shortDescription() {
		return "Joins all its parameters to a single string";
	}

}
