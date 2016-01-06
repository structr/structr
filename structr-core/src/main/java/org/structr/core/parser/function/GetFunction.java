package org.structr.core.parser.function;

import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.GraphObjectMap;
import org.structr.core.app.StructrApp;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.property.PropertyKey;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class GetFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_GET        = "Usage: ${get(entity, propertyKey)}. Example: ${get(this, \"children\")}";
	public static final String ERROR_MESSAGE_GET_ENTITY = "Cannot evaluate first argument to entity, must be entity or single element list of entities.";

	@Override
	public String getName() {
		return "get()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		final SecurityContext securityContext = entity != null ? entity.getSecurityContext() : ctx.getSecurityContext();
		if (arrayHasLengthAndAllElementsNotNull(sources, 2)) {

			final String keyName = sources[1].toString();
			GraphObject dataObject = null;

			// handle GraphObject
			if (sources[0] instanceof GraphObject) {
				dataObject = (GraphObject)sources[0];
			}

			// handle first element of a list of graph objects
			if (sources[0] instanceof List) {

				final List list = (List)sources[0];
				final int size = list.size();

				if (size == 1) {

					final Object value = list.get(0);
					if (value != null) {

						if (value instanceof GraphObject) {

							dataObject = (GraphObject)list.get(0);

						} else {

							return "get(): first element of collection is of type " + value.getClass() + " which is not supported.";
						}

					} else {

						return "get(): first element of collection is null.";
					}
				}
			}

			// handle map separately
			if (sources[0] instanceof Map && !(sources[0] instanceof GraphObjectMap)) {

				final Map map = (Map)sources[0];
				return map.get(keyName);
			}

			// handle request object
			if (sources[0] instanceof HttpServletRequest) {

				final HttpServletRequest request = (HttpServletRequest)sources[0];
				return request.getParameter(keyName);
			}

			if (dataObject != null) {

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

			} else {

				return ERROR_MESSAGE_GET_ENTITY;
			}
		}

		return usage(ctx.isJavaScriptContext());
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_GET;
	}

	@Override
	public String shortDescription() {
		return "Returns the value with the given name of the given entity, or an empty string";
	}

}
