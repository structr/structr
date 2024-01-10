/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.core.rest;

import com.google.gson.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.core.IJsonInput;
import org.structr.core.JsonInput;
import org.structr.core.JsonSingleInput;

import java.lang.reflect.Type;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

//~--- classes ----------------------------------------------------------------

/**
 * Controls deserialization of property sets.
 *
 *
 */
public class JsonInputGSONAdapter implements InstanceCreator<IJsonInput>, JsonSerializer<IJsonInput>, JsonDeserializer<IJsonInput> {

	private static final Logger logger = LoggerFactory.getLogger(JsonInputGSONAdapter.class.getName());

	@Override
	public IJsonInput createInstance(final Type type) {

		try {
			return (IJsonInput)type.getClass().newInstance();

		} catch (InstantiationException | IllegalAccessException e) {
			logger.warn("", e);
		}

		return null;
	}

	@Override
	public JsonElement serialize(final IJsonInput src, final Type typeOfSrc, final JsonSerializationContext context) {
		return null;
	}

	@Override
	public IJsonInput deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) throws JsonParseException {

		IJsonInput jsonInput = null;
		JsonInput wrapper    = null;

		if (json.isJsonObject()) {

			jsonInput = new JsonSingleInput();
			wrapper   = deserialize(json, context);
			jsonInput.add(wrapper);

		} else if (json.isJsonArray()) {

			jsonInput = new JsonSingleInput();

			JsonArray array = json.getAsJsonArray();
			for (final JsonElement elem : array) {

				wrapper = deserialize(elem, context);
				jsonInput.add(wrapper);
			}

		} else {

			// when we arrive here, the input element was
			// not one of the expected types => error
			throw new JsonSyntaxException("Invalid JSON, expecting object or array");
		}

		return jsonInput;
	}


	public static JsonInput deserialize(final JsonElement json, final JsonDeserializationContext context) throws JsonParseException {

		final JsonInput wrapper = new JsonInput();
		if (json.isJsonObject()) {

			final JsonObject obj = json.getAsJsonObject();

			for (final Entry<String, JsonElement> entry : obj.entrySet()) {

				final String key       = entry.getKey();
				final JsonElement elem = entry.getValue();

				if (elem.isJsonNull()) {

					wrapper.add(key, null);

				} else if (elem.isJsonObject()) {

					wrapper.add(key, deserialize(elem, context));

				} else if (elem.isJsonArray()) {

					final JsonArray array = elem.getAsJsonArray();
					final List list       = new LinkedList();

					for (final JsonElement element : array) {

						if (element.isJsonPrimitive()) {

							list.add(fromPrimitive((element.getAsJsonPrimitive())));

						} else if(element.isJsonObject()) {

							// create map of values
							list.add(deserialize(element, context));
						}
					}

					wrapper.add(key, list);

				} else if (elem.isJsonPrimitive()) {

					// wrapper.add(key, elem.getAsString());
					wrapper.add(key, fromPrimitive(elem.getAsJsonPrimitive()));
				}

			}

		} else if (json.isJsonArray()) {

			final JsonArray array = json.getAsJsonArray();
			for (final JsonElement elem : array) {

				if (elem.isJsonPrimitive()) {

					wrapper.add(elem.toString(), fromPrimitive(elem.getAsJsonPrimitive()));

				} else if(elem.isJsonObject()) {

					wrapper.add(elem.toString(), deserialize(elem, context));

				} else if(elem.isJsonArray()) {

					wrapper.add(elem.toString(), deserialize(elem, context));
				}
			}

		} else {

			// when we arrive here, the input element was
			// not one of the expected types => error
			throw new JsonSyntaxException("Invalid JSON, expecting object or array");
		}

		return wrapper;
	}

	public static Object fromPrimitive(final JsonPrimitive p) {

		if (p.isNumber()) {

			Number number = p.getAsNumber();

			// Detect if value is floating point
			if (p.getAsString().contains(".")) {

				return number.doubleValue();

			} else {

				return number.longValue();

			}

		} else if (p.isBoolean()) {

			return p.getAsBoolean();

		}

		return p.getAsString();
	}
}
