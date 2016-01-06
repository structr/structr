package org.structr.core.parser.function;

import java.util.List;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.property.PropertyKey;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class GetOrNullFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_GET_OR_NULL    = "Usage: ${get_or_null(entity, propertyKey)}. Example: ${get_or_null(this, \"children\")}";
	public static final String ERROR_MESSAGE_GET_OR_NULL_JS = "Usage: ${{Structr.getOrNull(entity, propertyKey)}}. Example: ${{Structr.getOrNull(this, \"children\")}}";

	@Override
	public String getName() {
		return "get_or_null()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		final SecurityContext securityContext = entity != null ? entity.getSecurityContext() : ctx.getSecurityContext();
		if (arrayHasLengthAndAllElementsNotNull(sources, 2)) {

			GraphObject dataObject = null;

			if (sources[0] instanceof GraphObject) {
				dataObject = (GraphObject)sources[0];
			}

			if (sources[0] instanceof List) {

				final List list = (List)sources[0];
				if (list.size() == 1 && list.get(0) instanceof GraphObject) {

					dataObject = (GraphObject)list.get(0);
				}
			}

			if (dataObject != null) {

				final String keyName = sources[1].toString();
				final PropertyKey key = StructrApp.getConfiguration().getPropertyKeyForJSONName(dataObject.getClass(), keyName);

				if (key != null) {

					final PropertyConverter inputConverter = key.inputConverter(securityContext);
					Object value = dataObject.getProperty(key);

					if (inputConverter != null) {
						return inputConverter.revert(value);
					}

					return dataObject.getProperty(key);
				}

				return "";
			}
		}

		return null;
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		
		if (inJavaScriptContext) {
			return ERROR_MESSAGE_GET_OR_NULL_JS;
		}

		return ERROR_MESSAGE_GET_OR_NULL;
	}

	@Override
	public String shortDescription() {
		return "Returns the value with the given name of the given entity, or null";
	}

}
