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

package org.structr.rest.adapter;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Map.Entry;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.RelationshipType;
import org.structr.core.GraphObject;
import org.structr.core.Value;
import org.structr.rest.wrapper.PropertySet.PropertyFormat;

/**
 * Controls serialization and deserialization of graph objects (nodes
 * and relationships).
 *
 * @author Christian Morgner
 */
public class GraphObjectGSONAdapter implements JsonSerializer<GraphObject> {

	private PropertyFormat propertyFormat = null;
	private Value<String> propertyView = null;
	private int outputNestingDepth = 1;
	private String idProperty = null;

	public GraphObjectGSONAdapter(PropertyFormat propertyFormat, Value<String> propertyView, String idProperty) {
		this.propertyFormat = propertyFormat;
		this.propertyView = propertyView;
		this.idProperty = idProperty;
	}

	@Override
	public JsonElement serialize(GraphObject src, Type typeOfSrc, JsonSerializationContext context) {
		
		String localPropertyView = propertyView.get();
		JsonElement serializedOutput = null;

		switch(propertyFormat) {

			case NestedKeyValueType:
				serializedOutput = serializeNestedKeyValueType(src, typeOfSrc, context, true, localPropertyView, 0);
				break;

			case NestedKeyValue:
				serializedOutput = serializeNestedKeyValueType(src, typeOfSrc, context, false, localPropertyView, 0);
				break;

			case FlatNameValue:
				serializedOutput = serializeFlatNameValue(src, typeOfSrc, context, localPropertyView, 0);
				break;
		}

		return serializedOutput;
	}

	// ----- private methods -----
	private JsonElement serializeNestedKeyValueType(GraphObject src, Type typeOfSrc, JsonSerializationContext context, boolean includeTypeInOutput, String localPropertyView, int depth) {

		// prevent endless recursion by pruning at depth 2
		if(depth > outputNestingDepth) {
			return null;
		}

		JsonObject jsonObject = new JsonObject();

		// id (only if idProperty is not set)
		if(idProperty == null) {
			jsonObject.add("id", new JsonPrimitive(src.getId()));
		} else {
			Object idPropertyValue = src.getProperty(idProperty);
			if(idPropertyValue != null) {
				String idString = idPropertyValue.toString();
				jsonObject.add("id", new JsonPrimitive(idString));
			}
		}

		String type = src.getType();
		if(type != null) {
			jsonObject.add("type", new JsonPrimitive(type));
		}

		// property keys
		JsonArray properties = new JsonArray();
		for(String key : src.getPropertyKeys(localPropertyView)) {

			Object value = src.getProperty(key);

			if(value instanceof Iterable) {

				JsonArray property = new JsonArray();

				for(Object o : (Iterable)value) {

					if(o instanceof GraphObject) {
						GraphObject obj = (GraphObject)o;
						JsonElement recursiveSerializedValue = this.serializeNestedKeyValueType(obj, typeOfSrc, context, includeTypeInOutput, localPropertyView, depth+1);
						if(recursiveSerializedValue != null) {
							property.add(recursiveSerializedValue);
						}

					} else if(o instanceof Map) {

						properties.add(serializeMap((Map)o));

					} else {

						// serialize primitive, this is for PropertyNotion
						properties.add(serializePrimitive(key, o, includeTypeInOutput));
					}

					// TODO: Unterstützung von Notions mit mehr als einem Property bei der Ausgabe!
					// => neuer Typ?
				}

				properties.add(property);

			} else if(value instanceof GraphObject) {

				GraphObject graphObject = (GraphObject)value;

				properties.add(this.serializeNestedKeyValueType(graphObject, typeOfSrc, context, includeTypeInOutput, localPropertyView, depth+1));

			} else if(value instanceof Map) {

				properties.add(serializeMap((Map)value));

			} else {

				properties.add(serializePrimitive(key, value, includeTypeInOutput));
			}
		}
		jsonObject.add("properties", properties);

		// outgoing relationships
		Map<RelationshipType, Long> outRelStatistics = src.getRelationshipInfo(Direction.OUTGOING);
		if(outRelStatistics != null) {

			JsonArray outRels = new JsonArray();

			for(Entry<RelationshipType, Long> entry : outRelStatistics.entrySet()) {

				RelationshipType relType = entry.getKey();
				Long count = entry.getValue();

				JsonObject outRelEntry = new JsonObject();
				outRelEntry.add("type", new JsonPrimitive(relType.name()));
				outRelEntry.add("count", new JsonPrimitive(count));

				outRels.add(outRelEntry);
			}
			jsonObject.add("out", outRels);
		}

		// incoming relationships
		Map<RelationshipType, Long> inRelStatistics = src.getRelationshipInfo(Direction.INCOMING);
		if(inRelStatistics != null) {

			JsonArray inRels = new JsonArray();

			for(Entry<RelationshipType, Long> entry : inRelStatistics.entrySet()) {

				RelationshipType relType = entry.getKey();
				Long count = entry.getValue();

				JsonObject inRelEntry = new JsonObject();
				inRelEntry.add("type", new JsonPrimitive(relType.name()));
				inRelEntry.add("count", new JsonPrimitive(count));

				inRels.add(inRelEntry);

			}
			jsonObject.add("in", inRels);
		}

		// start node id (for relationships)
		Long startNodeId = src.getStartNodeId();
		if(startNodeId != null) {
			jsonObject.add("startNodeId", new JsonPrimitive(startNodeId));
		}

		// end node id (for relationships)
		Long endNodeId = src.getEndNodeId();
		if(endNodeId != null) {
			jsonObject.add("endNodeId", new JsonPrimitive(endNodeId));
		}

		return jsonObject;
	}

	private JsonElement serializeFlatNameValue(GraphObject src, Type typeOfSrc, JsonSerializationContext context, String localPropertyView, int depth) {

		// prevent endless recursion by pruning at depth 2
		if(depth > outputNestingDepth) {
			return null;
		}

		JsonObject jsonObject = new JsonObject();

		// id (only if idProperty is not set)
		if(idProperty == null) {
			jsonObject.add("id", new JsonPrimitive(src.getId()));
		} else {
			Object idPropertyValue = src.getProperty(idProperty);
			if(idPropertyValue != null) {
				String idString = idPropertyValue.toString();
				jsonObject.add("id", new JsonPrimitive(idString));
			}
		}

		String type = src.getType();
		if(type != null) {
			jsonObject.add("type", new JsonPrimitive(type));
		}

		// property keys
		for(String key : src.getPropertyKeys(localPropertyView)) {

			Object value = src.getProperty(key);
			if(value != null) {

				// id property mapping
				if(key.equals(idProperty)) {
					key = "id";
				}

				if(value instanceof Iterable) {

					JsonArray property = new JsonArray();

					for(Object o : (Iterable)value) {

						if(o instanceof GraphObject) {
							GraphObject obj = (GraphObject)o;
							JsonElement recursiveSerializedValue = this.serializeFlatNameValue(obj, typeOfSrc, context, localPropertyView, depth+1);
							if(recursiveSerializedValue != null) {
								property.add(recursiveSerializedValue);
							}

						} else if(o instanceof Map) {

							property.add(serializeMap((Map)o));

						} else {

							// serialize primitive, this is for PropertyNotion
							property.add(new JsonPrimitive(o.toString()));
						}

					}

					jsonObject.add(key, property);

				} else if(value instanceof GraphObject) {

					GraphObject graphObject = (GraphObject)value;
					jsonObject.add(key, this.serializeFlatNameValue(graphObject, typeOfSrc, context, localPropertyView, depth+1));

				} else if(value instanceof Map) {

					jsonObject.add(key, serializeMap((Map)value));

				} else {

					jsonObject.add(key, new JsonPrimitive(value.toString()));
				}

			} else {

				jsonObject.add(key, new JsonNull());
			}
		}
		
		return jsonObject;
	}

	private JsonObject serializePrimitive(String key, Object value, boolean includeTypeInOutput) {

		JsonObject property = new JsonObject();

		// id property mapping
		if(key.equals(idProperty)) {
			key = "id";
		}

		property.add("key", new JsonPrimitive(key));

		if(value != null) {

			property.add("value", new JsonPrimitive(value.toString()));

			// include type?
			if(includeTypeInOutput) {
				String valueType = value.getClass().getSimpleName();
				property.add("type", new JsonPrimitive(valueType));
			}

		} else {

			property.add("value", new JsonNull());

			// include type?
			if(includeTypeInOutput) {
				property.add("type", new JsonNull());
			}
		}

		return property;
	}

	private JsonObject serializeMap(Map<String, Object> map) {

		JsonObject object = new JsonObject();

		for(Entry<String, Object> entry : map.entrySet()) {

			String key = entry.getKey();
			Object value = entry.getValue();

			if(key != null) {

				// id property mapping
				if(key.equals(idProperty)) {
					key = "id";
				}

				if(value != null) {
					object.add(key, new JsonPrimitive(value.toString()));
				} else {
					object.add(key, new JsonNull());
				}
			}
		}

		return object;
	}
}
