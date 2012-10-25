/*
 *  Copyright (C) 2010-2012 Axel Morgner
 *
 *  This file is part of structr <http://structr.org>.
 *
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */



package org.structr.core;

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
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.property.PropertyKey;

//~--- classes ----------------------------------------------------------------

/**
 * Controls deserialization of property sets.
 *
 * @author Christian Morgner
 */
public class JsonInputGSONAdapter implements InstanceCreator<JsonInput>, JsonSerializer<JsonInput>, JsonDeserializer<JsonInput> {

	private static final Logger logger = Logger.getLogger(JsonInputGSONAdapter.class.getName());

	//~--- fields ---------------------------------------------------------

	private PropertyKey idProperty         = null;

	//~--- constructors ---------------------------------------------------

	public JsonInputGSONAdapter() {}

	public JsonInputGSONAdapter(Value<String> propertyView, PropertyKey idProperty) {

		this.idProperty     = idProperty;
	}

	//~--- methods --------------------------------------------------------

	@Override
	public JsonInput createInstance(Type type) {
		return new JsonInput();
	}

	@Override
	public JsonElement serialize(JsonInput src, Type typeOfSrc, JsonSerializationContext context) {
		return null;
	}

	@Override
	public JsonInput deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {

		JsonInput wrapper = new JsonInput();

		if (json.isJsonObject()) {

			JsonObject obj = json.getAsJsonObject();

			for (Entry<String, JsonElement> entry : obj.entrySet()) {

				String key       = entry.getKey();
				JsonElement elem = entry.getValue();

				// static mapping of IdProperty if present
				if ((idProperty != null) && "id".equals(key)) {

					key = idProperty.name();

				}

				if (elem.isJsonNull()) {

					wrapper.add(key, null);

				} else if (elem.isJsonObject()) {

					wrapper.add(key, deserialize(elem, typeOfT, context));

				} else if (elem.isJsonArray()) {

					JsonArray array = elem.getAsJsonArray();
					List list = new LinkedList();
					for(JsonElement element : array) {
						if (element.isJsonPrimitive()) {
							list.add(fromPrimitive((element.getAsJsonPrimitive())));
						} else if(element.isJsonObject()) {
							// create map of values
							list.add(deserialize(element, typeOfT, context));
						}
					}

					wrapper.add(key, list);

				} else if (elem.isJsonPrimitive()) {

					// wrapper.add(key, elem.getAsString());
					wrapper.add(key, fromPrimitive((JsonPrimitive) elem));
				}

			}

		} else if(json.isJsonArray()) {

			JsonArray array = json.getAsJsonArray();
			for(JsonElement elem : array) {

				if(elem.isJsonPrimitive()) {
					wrapper.add(elem.toString(), ((JsonPrimitive)elem).getAsString());
				} else if(elem.isJsonObject()) {
					wrapper.add(elem.toString(), deserialize(elem, typeOfT, context));
				} else if(elem.isJsonArray()) {
					wrapper.add(elem.toString(), deserialize(elem, typeOfT, context));
				}
			}
		}

		return wrapper;
	}

	private static Object fromPrimitive(final JsonPrimitive p) {

		if (p.isNumber()) {

			Number number = p.getAsNumber();
			if (number instanceof Integer) {
				return number.intValue();
			} else {
				return number.doubleValue();
			}

		}

		return p.getAsString();
	}

	//~--- get methods ----------------------------------------------------
}
