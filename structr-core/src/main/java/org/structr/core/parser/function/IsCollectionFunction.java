package org.structr.core.parser.function;

import java.util.Collection;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class IsCollectionFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_IS_COLLECTION = "Usage: ${is_collection(value)}. Example: ${is_collection(this)}";

	@Override
	public String getName() {
		return "is_collection()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		if (arrayHasLengthAndAllElementsNotNull(sources, 1)) {
			return (sources[0] instanceof Collection);
		} else {
			return false;
		}

	}


	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_IS_COLLECTION;
	}

	@Override
	public String shortDescription() {
		return "Returns true if the given argument is a collection";
	}

}
