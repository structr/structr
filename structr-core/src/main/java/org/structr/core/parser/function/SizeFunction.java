package org.structr.core.parser.function;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import static org.structr.core.parser.Functions.NULL_STRING;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class SizeFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_SIZE = "Usage: ${size(collection)}. Example: ${size(this.children)}";

	@Override
	public String getName() {
		return "size()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		final List list = new ArrayList();
		for (final Object source : sources) {

			if (source != null) {

				if (source instanceof Collection) {

					// filter null objects
					for (Object obj : (Collection)source) {
						if (obj != null && !NULL_STRING.equals(obj)) {

							list.add(obj);
						}
					}

				} else if (source.getClass().isArray()) {

					list.addAll(Arrays.asList((Object[])source));

				} else if (source != null && !NULL_STRING.equals(source)) {

					list.add(source);
				}

				return list.size();
			}
		}

		return 0;
	}


	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_SIZE;
	}

	@Override
	public String shortDescription() {
		return "Returns the size of the given collection";
	}

}
