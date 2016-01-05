package org.structr.core.parser.function;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.structr.common.GraphObjectComparator;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.property.PropertyKey;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class SortFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_SORT = "Usage: ${sort(list1, key [, true])}. Example: ${sort(this.children, \"name\")}";

	@Override
	public String getName() {
		return "sort()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		if (arrayHasMinLengthAndAllElementsNotNull(sources, 2)) {

			if (sources[0] instanceof List && sources[1] instanceof String) {

				final List list = (List)sources[0];
				final String sortKey = sources[1].toString();
				final Iterator iterator = list.iterator();

				if (iterator.hasNext()) {

					final Object firstElement = iterator.next();
					if (firstElement instanceof GraphObject) {

						final Class type = firstElement.getClass();
						final PropertyKey key = StructrApp.getConfiguration().getPropertyKeyForJSONName(type, sortKey);
						final boolean descending = sources.length == 3 && sources[2] != null && "true".equals(sources[2].toString());

						if (key != null) {

							List<GraphObject> sortCollection = (List<GraphObject>)list;
							Collections.sort(sortCollection, new GraphObjectComparator(key, descending));

							return sortCollection;
						}
					}

				}
			}
		}

		return sources[0];
	}


	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_SORT;
	}

	@Override
	public String shortDescription() {
		return "Sorts the given collection according to the given property key";
	}

}
