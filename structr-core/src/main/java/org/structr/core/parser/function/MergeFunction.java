package org.structr.core.parser.function;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class MergeFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_MERGE = "Usage: ${merge(list1, list2, list3, ...)}. Example: ${merge(this.children, this.siblings)}";

	@Override
	public String getName() {
		return "merge()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		final List list = new ArrayList();
		for (final Object source : sources) {

			if (source instanceof Collection) {

				// filter null objects
				for (Object obj : (Collection)source) {

					if (obj != null) {

						list.add(obj);
					}
				}

			} else if (source != null) {

				list.add(source);
			}
		}

		return list;
	}


	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_MERGE;
	}

	@Override
	public String shortDescription() {
		return "Merges the given collections / objects into a single collection";
	}


}
