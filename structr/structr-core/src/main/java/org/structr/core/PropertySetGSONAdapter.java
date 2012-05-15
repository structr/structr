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

import org.structr.core.PropertySet.PropertyFormat;

//~--- JDK imports ------------------------------------------------------------

import java.lang.reflect.Type;

import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * Controls deserialization of property sets.
 *
 * @author Christian Morgner
 */
public class PropertySetGSONAdapter implements InstanceCreator<PropertySet>, JsonSerializer<PropertySet>, JsonDeserializer<PropertySet> {

	private static final Logger logger = Logger.getLogger(PropertySetGSONAdapter.class.getName());

	//~--- fields ---------------------------------------------------------

	private GraphObjectGSONAdapter adapter = null;
	private String idProperty              = null;
	private PropertyFormat propertyFormat  = null;

	//~--- constructors ---------------------------------------------------

	public PropertySetGSONAdapter() {}

	public PropertySetGSONAdapter(PropertyFormat propertyFormat, Value<String> propertyView, String idProperty) {

		this.adapter        = new GraphObjectGSONAdapter(propertyFormat, propertyView, idProperty);
		this.propertyFormat = propertyFormat;
		this.idProperty     = idProperty;
	}

	//~--- methods --------------------------------------------------------

	@Override
	public PropertySet createInstance(Type type) {
		return new PropertySet();
	}

	@Override
	public JsonElement serialize(PropertySet src, Type typeOfSrc, JsonSerializationContext context) {
		return null;
	}

	@Override
	public PropertySet deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {

		PropertySet wrapper = null;

		switch (propertyFormat) {

			case NestedKeyValueType :
				wrapper = deserializeNestedKeyValueType(json, typeOfT, context, true);

				break;

			case NestedKeyValue :
				wrapper = deserializeNestedKeyValueType(json, typeOfT, context, false);

				break;

			case FlatNameValue :
				wrapper = deserializeFlatNameValue(json, typeOfT, context);

				break;

		}

		return wrapper;
	}

	// ----- private methods -----
	private PropertySet deserializeNestedKeyValueType(JsonElement json, Type typeOfT, JsonDeserializationContext context, boolean includeTypeInOutput) {

		PropertySet wrapper = new PropertySet();

		if (json.isJsonArray()) {

			JsonArray array = json.getAsJsonArray();

			for (JsonElement elem : array) {

				convertJsonElement(elem, wrapper);

			}

		}

		return wrapper;
	}

	private PropertySet deserializeFlatNameValue(JsonElement json, Type typeOfT, JsonDeserializationContext context) {

		PropertySet wrapper = new PropertySet();

		if (json.isJsonObject()) {

			JsonObject obj = json.getAsJsonObject();

			for (Entry<String, JsonElement> entry : obj.entrySet()) {

				String key       = entry.getKey();
				JsonElement elem = entry.getValue();

				// static mapping of IdProperty if present
				if ((idProperty != null) && "id".equals(key)) {

					key = idProperty;

				}

				if (elem.isJsonNull()) {

					wrapper.add(key, null);

				} else if (elem.isJsonObject()) {

					wrapper.add(key, deserializeFlatNameValue(elem, typeOfT, context));

				} else if (elem.isJsonArray()) {

					JsonArray array = elem.getAsJsonArray();
					List list = new LinkedList();
					for(JsonElement element : array) {
						if (element.isJsonPrimitive()) {
							list.add(fromPrimitive((element.getAsJsonPrimitive())));
						} else if(element.isJsonObject()) {
							// create map of values
							list.add(deserializeFlatNameValue(element, typeOfT, context));
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
					wrapper.add(elem.toString(), deserializeFlatNameValue(elem, typeOfT, context));
				} else if(elem.isJsonArray()) {
					wrapper.add(elem.toString(), deserializeFlatNameValue(elem, typeOfT, context));
				}
			}
		}

		return wrapper;
	}

	private void convertJsonElement(JsonElement elem, PropertySet wrapper) {

		String key   = null;
		Object value = null;
		String type  = "String";

		if (elem.isJsonObject()) {

			JsonObject obj = elem.getAsJsonObject();

			// set type (defaults to String)
			if (obj.has("type")) {

				type = obj.get("type").getAsString();

				logger.log(Level.INFO, "type: {0}", type);

			}

			if (obj.has("key")) {

				key = obj.get("key").getAsString();

				// static mapping of IdProperty if present
				if ((idProperty != null) && "id".equals(key)) {

					logger.log(Level.INFO, "mapped key: {0} -> {1}", new Object[] { key, idProperty });

					key = idProperty;

				} else {

					logger.log(Level.INFO, "key: {0}", key);

				}

			}

			if (obj.has("value")) {

				JsonElement valueElement = obj.get("value");

				value = getTypedValue(valueElement, type);

				logger.log(Level.INFO, "value: {0}", value);

			}

		}

		if ((key != null) && (value != null)) {

//                      wrapper.add(key, value, type);
			wrapper.add(key, value);
		}
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

	private Object getTypedValue(JsonElement valueElement, String type) {

		Object value = null;

		if ((type == null) || type.equals("null")) {

			value = valueElement.getAsJsonNull();

		} else if (type.equals("String")) {

			value = valueElement.getAsString();

		} else if (type.equals("Number")) {

			value = valueElement.getAsNumber();

		} else if (type.equals("Boolean")) {

			value = valueElement.getAsBoolean();

		} else if (type.equals("JsonArray")) {

			value = valueElement.getAsJsonArray();

		} else if (type.equals("JsonObject")) {

			value = valueElement.getAsJsonObject();

		} else if (type.equals("Integer")) {

			value = valueElement.getAsInt();

		} else if (type.equals("Long")) {

			value = valueElement.getAsLong();

		} else if (type.equals("Double")) {

			value = valueElement.getAsDouble();

		} else if (type.equals("Float")) {

			value = valueElement.getAsFloat();

		} else if (type.equals("Byte")) {

			value = valueElement.getAsByte();

		} else if (type.equals("Short")) {

			value = valueElement.getAsShort();

		} else if (type.equals("Character")) {

			value = valueElement.getAsCharacter();

		} else if (type.equals("BigDecimal")) {

			value = valueElement.getAsBigDecimal();

		} else if (type.equals("BigInteger")) {

			value = valueElement.getAsBigInteger();

		}

		return value;
	}
}
