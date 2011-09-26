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
import org.structr.core.node.NodeAttribute;
import org.structr.core.resource.wrapper.NodeAttributeWrapper;

/**
 * Controls serialization and deserialization of structr nodes.
 *
 * @author Christian Morgner
 */
public class NodeAttributeGSONAdapter implements InstanceCreator<NodeAttributeWrapper>, JsonSerializer<NodeAttributeWrapper>, JsonDeserializer<NodeAttributeWrapper> {

	private static final Logger logger = Logger.getLogger(NodeAttributeGSONAdapter.class.getName());
	
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
	public NodeAttributeWrapper createInstance(Type type) {
		return new NodeAttributeWrapper();
	}

	@Override
	public JsonElement serialize(NodeAttributeWrapper src, Type typeOfSrc, JsonSerializationContext context) {

		return null;
	}

	@Override
	public NodeAttributeWrapper deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {

		PropertyFormat propertyFormat = getLocalPropertyFormat();
		NodeAttributeWrapper wrapper = null;

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
	private NodeAttributeWrapper deserializeNestedKeyValueType(JsonElement json, Type typeOfT, JsonDeserializationContext context, boolean includeTypeInOutput) {

		NodeAttributeWrapper wrapper = new NodeAttributeWrapper();
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

	private NodeAttributeWrapper deserializeFlatNameValue(JsonElement json, Type typeOfT, JsonDeserializationContext context) {

		NodeAttributeWrapper wrapper = new NodeAttributeWrapper();
		if(json.isJsonObject()) {

			JsonObject obj = json.getAsJsonObject();
			for(Entry<String, JsonElement> entry : obj.entrySet()) {

				String key = entry.getKey();
				JsonElement elem = entry.getValue();

				wrapper.add(key, elem.getAsString());
			}

		} else {

			logger.log(Level.INFO, "other type..");
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
}
