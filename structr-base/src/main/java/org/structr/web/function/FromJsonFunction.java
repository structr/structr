/*
 * Copyright (C) 2010-2025 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.web.function;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.lang3.StringUtils;
import org.structr.core.GraphObjectMap;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class FromJsonFunction extends UiCommunityFunction {

	@Override
	public String getName() {
		return "from_json";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("source");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) {

		if (sources != null && sources.length > 0) {

			if (sources[0] == null) {
				return "";
			}

			final String source = sources[0].toString();

			try {

				final Object parsed = parseJson(source);

				if (parsed != null) {
					return parsed;
				}

			} catch (Throwable t) {

				logException(caller, t, sources);
			}

			return "";

		} else {

			logParameterError(caller, sources, ctx.isJavaScriptContext());
		}

		return usage(ctx.isJavaScriptContext());
	}

	public static Object parseJson(final String source) throws JsonSyntaxException {

		final Gson gson = new GsonBuilder().create();
		List<Map<String, Object>> objects = new LinkedList<>();

		try {

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

		} catch (JsonSyntaxException jse) {
			// Exception while parsing as Map - try default as object next
		}

		// Fallback on default behavior (works for primitives and arrays of primitives or mixed content)
		final Object value = gson.fromJson(source, Object.class);

		if (value != null) {

			return UiFunction.toGraphObject(value, 3);
		}

		return null;
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${from_json(src)}. Example: ${from_json('{name:test}')}"),
			Usage.javaScript("Usage: ${{Structr.fromJson(src)}}. Example: ${{Structr.fromJson('{name:test}')}}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Parses the given JSON string and returns an object.";
	}

	@Override
	public String getLongDescription() {
		return "";
	}

}
