package org.structr.core.parser.function;

import java.util.Map;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.Query;
import org.structr.core.app.StructrApp;
import org.structr.core.converter.PropertyConverter;
import static org.structr.core.parser.function.FindFunction.ERROR_MESSAGE_FIND;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.schema.ConfigurationProvider;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class SearchFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_SEARCH    = "Usage: ${search(type, key, value)}. Example: ${search(\"User\", \"name\", \"abc\")}";
	public static final String ERROR_MESSAGE_SEARCH_JS = "Usage: ${{Structr.search(type, key, value)}}. Example: ${{Structr.search(\"User\", \"name\", \"abc\")}}";

	@Override
	public String getName() {
		return "search()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		if (sources != null) {

			final SecurityContext securityContext = entity != null ? entity.getSecurityContext() : ctx.getSecurityContext();
			final ConfigurationProvider config = StructrApp.getConfiguration();
			final Query query = StructrApp.getInstance(securityContext).nodeQuery();
			Class type = null;

			if (sources.length >= 1 && sources[0] != null) {

				type = config.getNodeEntityClass(sources[0].toString());

				if (type != null) {

					query.andTypes(type);
				}
			}

			// extension for native javascript objects
			if (sources.length == 2 && sources[1] instanceof Map) {

				final PropertyMap map = PropertyMap.inputTypeToJavaType(securityContext, type, (Map)sources[1]);
				for (final Map.Entry<PropertyKey, Object> entry : map.entrySet()) {

					query.and(entry.getKey(), entry.getValue(), false);
				}

			} else {

				final Integer parameter_count = sources.length;

				if (parameter_count % 2 == 0) {

					throw new FrameworkException(400, "Invalid number of parameters: " + parameter_count + ". Should be uneven: " + ERROR_MESSAGE_FIND);
				}

				for (Integer c = 1; c < parameter_count; c += 2) {

					final PropertyKey key = config.getPropertyKeyForJSONName(type, sources[c].toString());

					if (key != null) {

						// throw exception if key is not indexed (otherwise the user will never know)
						if (!key.isSearchable()) {

							throw new FrameworkException(400, "Search key " + key.jsonName() + " is not indexed.");
						}

						final PropertyConverter inputConverter = key.inputConverter(securityContext);
						Object value = sources[c + 1];

						if (inputConverter != null) {

							value = inputConverter.convert(value);
						}

						query.and(key, value, false);
					}

				}
			}

			final Object x = query.getAsList();

			// return search results
			return x;
		}

		return "";
	}


	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_SEARCH;
	}

	@Override
	public String shortDescription() {
		return "";
	}
}
