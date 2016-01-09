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
package org.structr.rest;

import com.google.gson.InstanceCreator;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;


//~--- JDK imports ------------------------------------------------------------






import java.lang.reflect.Type;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.structr.core.IJsonInput;
import org.structr.core.JsonInput;
import org.structr.core.JsonSingleInput;

//~--- classes ----------------------------------------------------------------

/**
 * Controls deserialization of property sets.
 *
 *
 */
public class JsonInputGSONAdapter implements InstanceCreator<IJsonInput>, JsonSerializer<IJsonInput>, JsonDeserializer<IJsonInput> {

	private static final Logger logger = Logger.getLogger(JsonInputGSONAdapter.class.getName());

	@Override
	public IJsonInput createInstance(Type type) {

		try {
			return (IJsonInput)type.getClass().newInstance();

		} catch (InstantiationException | IllegalAccessException e) {
			e.printStackTrace();
		}

		return null;
	}

	@Override
	public JsonElement serialize(IJsonInput src, Type typeOfSrc, JsonSerializationContext context) {
		return null;
	}

	@Override
	public IJsonInput deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {

		IJsonInput jsonInput = null;
		JsonInput wrapper    = null;

		if (json.isJsonObject()) {

			jsonInput = new JsonSingleInput();
			wrapper   = deserialize(json, context);
			jsonInput.add(wrapper);

		} else if(json.isJsonArray()) {

			jsonInput = new JsonSingleInput();

			JsonArray array = json.getAsJsonArray();
			for(JsonElement elem : array) {

				wrapper = deserialize(elem, context);
				jsonInput.add(wrapper);
			}
		}

		return jsonInput;
	}


	private JsonInput deserialize(JsonElement json, JsonDeserializationContext context) throws JsonParseException {

		JsonInput wrapper = new JsonInput();
		if (json.isJsonObject()) {

			JsonObject obj = json.getAsJsonObject();

			for (Entry<String, JsonElement> entry : obj.entrySet()) {

				String key       = entry.getKey();
				JsonElement elem = entry.getValue();

				if (elem.isJsonNull()) {

					wrapper.add(key, null);

				} else if (elem.isJsonObject()) {

					wrapper.add(key, deserialize(elem, context));

				} else if (elem.isJsonArray()) {

					final JsonArray array = elem.getAsJsonArray();
					final List list       = new LinkedList();

					for(JsonElement element : array) {

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

		} else if(json.isJsonArray()) {

			JsonArray array = json.getAsJsonArray();
			for(JsonElement elem : array) {

				if (elem.isJsonPrimitive()) {

					wrapper.add(elem.toString(), fromPrimitive(elem.getAsJsonPrimitive()));

				} else if(elem.isJsonObject()) {

					wrapper.add(elem.toString(), deserialize(elem, context));

				} else if(elem.isJsonArray()) {

					wrapper.add(elem.toString(), deserialize(elem, context));
				}
			}
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
