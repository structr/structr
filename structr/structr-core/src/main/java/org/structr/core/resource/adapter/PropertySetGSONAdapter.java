/*
 *  Copyright (C) 2011 Axel Morgner
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

package org.structr.core.resource.adapter;

import com.google.gson.InstanceCreator;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.core.resource.IllegalPathException;
import org.structr.core.resource.PathException;
import org.structr.core.resource.wrapper.PropertySet;

/**
 * Controls serialization and deserialization of structr nodes.
 *
 * @author Christian Morgner
 */
public class PropertySetGSONAdapter implements InstanceCreator<PropertySet>, JsonSerializer<PropertySet>, JsonDeserializer<PropertySet> {

	private static final Logger logger = Logger.getLogger(PropertySetGSONAdapter.class.getName());
	
	private ThreadLocal<PropertyFormat> threadLocalPropertyFormat = new ThreadLocal<PropertyFormat>();
	private PropertyFormat defaultPropertyFormat = PropertyFormat.NestedKeyValueType;

	public enum PropertyFormat {
		NestedKeyValue,			// "properties" : [ { "key" : "name", "value" : "Test" }, ... ]
		NestedKeyValueType,		// "properties" : [ { "key" : "name", "value" : "Test", "type" : "String" }, ... ]
		FlatNameValue			// { "name" : "Test" }
	}

	public void setDefaultPropertyFormat(PropertyFormat propertyFormat) {
		this.defaultPropertyFormat = propertyFormat;
	}

	public void setPropertyFormat(PropertyFormat propertyFormat) {
		this.threadLocalPropertyFormat.set(propertyFormat);
	}

	public PropertyFormat getPropertyFormat() {
		return this.threadLocalPropertyFormat.get();
	}

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

		PropertyFormat propertyFormat = getLocalPropertyFormat();
		PropertySet wrapper = null;

		switch(propertyFormat) {

			case NestedKeyValueType:
				wrapper = deserializeNestedKeyValueType(json, typeOfT, context, true);
				break;

			case NestedKeyValue:
				wrapper = deserializeNestedKeyValueType(json, typeOfT, context, false);
				break;

			case FlatNameValue:
				wrapper = deserializeFlatNameValue(json, typeOfT, context);
				break;
		}

		return wrapper;
	}

	// ----- private methods -----
	private PropertySet deserializeNestedKeyValueType(JsonElement json, Type typeOfT, JsonDeserializationContext context, boolean includeTypeInOutput) {

		PropertySet wrapper = new PropertySet();
		if(json.isJsonArray()) {

			JsonArray array = json.getAsJsonArray();
			for(JsonElement elem : array) {
				convertJsonElement(elem, wrapper);
			}
		}

		return wrapper;
	}

	private PropertySet deserializeFlatNameValue(JsonElement json, Type typeOfT, JsonDeserializationContext context) {

		PropertySet wrapper = new PropertySet();
		if(json.isJsonObject()) {

			JsonObject obj = json.getAsJsonObject();
			for(Entry<String, JsonElement> entry : obj.entrySet()) {

				String key = entry.getKey();
				JsonElement elem = entry.getValue();

				wrapper.add(key, elem.getAsString());
			}
		}

		return wrapper;
	}

	private PropertyFormat getLocalPropertyFormat() {

		PropertyFormat propertyFormat = threadLocalPropertyFormat.get();
		if(propertyFormat == null) {
			propertyFormat = defaultPropertyFormat;
		}

		return propertyFormat;
	}

	private void convertJsonElement(JsonElement elem, PropertySet wrapper) {

		String key = null;
		Object value = null;
		String type = "String";

		if(elem.isJsonObject()) {

			JsonObject obj = elem.getAsJsonObject();

			// set type (defaults to String)
			if(obj.has("type")) {
				type = obj.get("type").getAsString();
				logger.log(Level.INFO, "type: {0}", type);
			}

			if(obj.has("key")) {
				key = obj.get("key").getAsString();
				logger.log(Level.INFO, "key: {0}", key);
			}
			
			if(obj.has("value")) {
				
				JsonElement valueElement = obj.get("value");
				value = getTypedValue(valueElement, type);
				logger.log(Level.INFO, "value: {0}", value);
			}
		}

		if(key != null && value != null) {
			wrapper.add(key, value, type);
		}
	}

	private Object getTypedValue(JsonElement valueElement, String type) {

		Object value = null;

		if(type.equals("String"))	value = valueElement.getAsString(); else
		if(type.equals("Integer"))	value = valueElement.getAsInt(); else
		if(type.equals("Long"))		value = valueElement.getAsLong(); else
		if(type.equals("Double"))	value = valueElement.getAsDouble(); else
		if(type.equals("Float"))	value = valueElement.getAsFloat(); else
		if(type.equals("Byte"))		value = valueElement.getAsByte(); else
		if(type.equals("Short"))	value = valueElement.getAsShort(); else
		if(type.equals("Character"))	value = valueElement.getAsCharacter(); else
		if(type.equals("BigDecimal"))	value = valueElement.getAsBigDecimal(); else
		if(type.equals("BigInteger"))	value = valueElement.getAsBigInteger();

		return value;
	}
}
