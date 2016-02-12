 /**
 * Copyright (C) 2010-2016 Structr GmbH
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
package org.structr.core.parser.function;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

public class ChangelogFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_CHANGLOG = "Usage: ${changelog(entity[, resolve=false])}. Example: ${changelog(current)}";
	public static final String ERROR_MESSAGE_CHANGLOG_JS = "Usage: ${{Structr.changelog(entity[, resolve=false])}}. Example: ${{Structr.changelog(Structr.get('current'))}}";

	private static final Logger logger = Logger.getLogger(ChangelogFunction.class.getName());


	@Override
	public String getName() {
		return "changelog()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		if (arrayHasMinLengthAndMaxLengthAndAllElementsNotNull(sources, 1, 2)) {

			final App app = StructrApp.getInstance();

			GraphObject dataObject;

			if (sources[0] instanceof GraphObject) {
				dataObject = (GraphObject)sources[0];
			} else {
				return usage(ctx.isJavaScriptContext());
			}

			final List list = new ArrayList();

			final String[] entries = dataObject.getProperty(GraphObject.structrChangeLog).split("\n");

			if (entries.length > 0) {

				final boolean resolveTargets = (sources.length >= 2 && Boolean.TRUE.equals(sources[1]));
				final JsonParser parser = new JsonParser();

				for (String entry : entries) {
					final JsonObject jsonObj = parser.parse(entry).getAsJsonObject();

					final String verb = jsonObj.get("verb").getAsString();

					final TreeMap<String, Object> obj = new TreeMap<>();
					obj.put("verb", verb);
					obj.put("time", jsonObj.get("time").getAsLong());
					obj.put("userId", jsonObj.get("userId").getAsString());
					obj.put("userName", jsonObj.get("userName").getAsString());

					if (verb.equals("create") || verb.equals("delete")) {

						obj.put("target", jsonObj.get("target").getAsString());

						if (resolveTargets) {
							obj.put("targetObj", app.get(jsonObj.get("target").getAsString()));
						}

						list.add(obj);

					} else if (verb.equals("link") || verb.equals("unlink")) {

						obj.put("rel", jsonObj.get("rel").getAsString());
						obj.put("target", jsonObj.get("target").getAsString());

						if (resolveTargets) {
							obj.put("targetObj", app.get(jsonObj.get("target").getAsString()));
						}

						list.add(obj);

					} else if (verb.equals("change")) {

						obj.put("key", jsonObj.get("key").getAsString());

						final JsonElement prev = jsonObj.get("prev");
						obj.put("prev", (prev.isJsonNull() ? null : prev.getAsString()));

						final JsonElement val = jsonObj.get("key");
						obj.put("val", (val.isJsonNull() ? null : val.getAsString()));
						list.add(obj);

					} else {

						logger.log(Level.WARNING, "Unknown verb in changelog: '{0}'", (verb == null ? "null" : verb));

					}

				}

			}

			return list;

		} else {

			return usage(ctx.isJavaScriptContext());

		}

	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_CHANGLOG_JS : ERROR_MESSAGE_CHANGLOG);
	}

	@Override
	public String shortDescription() {
		return "Returns the changelog object";
	}

}
