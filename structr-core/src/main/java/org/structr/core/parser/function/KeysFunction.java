package org.structr.core.parser.function;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.GraphObjectMap;
import org.structr.core.property.PropertyKey;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class KeysFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_KEYS = "Usage: ${keys(entity, viewName)}. Example: ${keys(this, \"ui\")}";

	@Override
	public String getName() {
		return "keys()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		if (arrayHasMinLengthAndAllElementsNotNull(sources, 2) && sources[0] instanceof GraphObject) {

			final Set<String> keys = new LinkedHashSet<>();
			final GraphObject source = (GraphObject)sources[0];

			for (final PropertyKey key : source.getPropertyKeys(sources[1].toString())) {
				keys.add(key.jsonName());
			}

			return new LinkedList<>(keys);

		} else if (arrayHasMinLengthAndAllElementsNotNull(sources, 1) && sources[0] instanceof GraphObjectMap) {

			return new LinkedList<>(((GraphObjectMap)sources[0]).keySet());

		} else if (arrayHasMinLengthAndAllElementsNotNull(sources, 1) && sources[0] instanceof Map) {

			return new LinkedList<>(((Map)sources[0]).keySet());

		}

		return "";
	}


	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_KEYS;
	}

	@Override
	public String shortDescription() {
		return "Returns the property keys of the given entity";
	}

}
