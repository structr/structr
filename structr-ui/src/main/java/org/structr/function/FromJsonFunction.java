package org.structr.function;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.structr.core.GraphObject;
import org.structr.core.GraphObjectMap;
import org.structr.schema.action.ActionContext;

/**
 *
 */
public class FromJsonFunction extends UiFunction {

	public static final String ERROR_MESSAGE_FROM_JSON    = "Usage: ${from_json(src)}. Example: ${from_json('{name:test}')}";
	public static final String ERROR_MESSAGE_FROM_JSON_JS = "Usage: ${{Structr.from_json(src)}}. Example: ${{Structr.from_json('{name:test}')}}";

	@Override
	public String getName() {
		return "from_json()";
	}

	@Override
	public Object apply(ActionContext ctx, final GraphObject entity, final Object[] sources) {

		if (sources != null && sources.length > 0) {

			if (sources[0] != null) {

				try {

					final String source = sources[0].toString();
					final Gson gson = new GsonBuilder().create();
					List<Map<String, Object>> objects = new LinkedList<>();

					if (StringUtils.startsWith(source, "[")) {

						final List<Map<String, Object>> list = gson.fromJson(source, new TypeToken<List<Map<String, Object>>>() {
						}.getType());
						final List<GraphObjectMap> elements = new LinkedList<>();

						if (list != null) {

							objects.addAll(list);
						}

						for (final Map<String, Object> src : objects) {

							final GraphObjectMap destination = new GraphObjectMap();
							elements.add(destination);

							recursivelyConvertMapToGraphObjectMap(destination, src, 0);
						}

						return elements;

					} else if (StringUtils.startsWith(source, "{")) {

						final Map<String, Object> value = gson.fromJson(source, new TypeToken<Map<String, Object>>() {
						}.getType());
						final GraphObjectMap destination = new GraphObjectMap();

						if (value != null) {

							recursivelyConvertMapToGraphObjectMap(destination, value, 0);
						}

						return destination;
					}

				} catch (Throwable t) {
					t.printStackTrace();
				}
			}

			return "";
		}

		return usage(ctx.isJavaScriptContext());
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_FROM_JSON_JS : ERROR_MESSAGE_FROM_JSON);
	}

	@Override
	public String shortDescription() {
		return "Parses the given JSON string and returns an object";
	}

}
