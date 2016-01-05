package org.structr.core.parser.function;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class MergeUniqueFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_MERGE_UNIQUE = "Usage: ${merge_unique(list1, list2, list3, ...)}. Example: ${merge_unique(this.children, this.siblings)}";

	@Override
	public String getName() {
		return "merge_unique()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		final Set result = new LinkedHashSet<>();
		for (final Object source : sources) {

			if (source instanceof Collection) {

				// filter null objects
				for (Object obj : (Collection)source) {

					if (obj != null) {

						result.add(obj);
					}
				}

			} else if (source != null) {

				result.add(source);
			}
		}

		return new LinkedList<>(result);
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_MERGE_UNIQUE;
	}

	@Override
	public String shortDescription() {
		return "Merges the given collections / objects into a single collection, removing duplicates";
	}

}
