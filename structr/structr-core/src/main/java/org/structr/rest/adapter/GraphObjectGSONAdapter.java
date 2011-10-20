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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.RelationshipType;
import org.structr.common.PropertyView;
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

	private Value<PropertyView> propertyView = null;
	private PropertyFormat propertyFormat = null;
	private int outputNestingDepth = 1;

	public GraphObjectGSONAdapter(PropertyFormat propertyFormat, Value<PropertyView> propertyView) {
		this.propertyFormat = propertyFormat;
		this.propertyView = propertyView;
	}

	@Override
	public JsonElement serialize(GraphObject src, Type typeOfSrc, JsonSerializationContext context) {
		
		PropertyView localPropertyView = propertyView.get();
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
	private JsonElement serializeNestedKeyValueType(GraphObject src, Type typeOfSrc, JsonSerializationContext context, boolean includeTypeInOutput, PropertyView localPropertyView, int depth) {

		// prevent endless recursion by pruning at depth 2
		if(depth > outputNestingDepth) {
			return null;
		}

		JsonObject jsonObject = new JsonObject();

		// id
		jsonObject.add("id", new JsonPrimitive(src.getId()));

		String type = src.getType();
		if(type != null) {
			jsonObject.add("type", new JsonPrimitive(type));
		}

		// property keys
		JsonArray properties = new JsonArray();
		for(String key : src.getPropertyKeys(localPropertyView)) {

			Object value = src.getProperty(key);

			if(value instanceof List) {

				List<GraphObject> values = (List<GraphObject>)value;
				JsonArray property = new JsonArray();

				for(GraphObject obj : values) {
					JsonElement recursiveSerializedValue = this.serializeNestedKeyValueType(obj, typeOfSrc, context, includeTypeInOutput, localPropertyView, depth+1);
					if(recursiveSerializedValue != null) {
						property.add(recursiveSerializedValue);
					}
				}

				properties.add(property);

			} else {

				JsonObject property = new JsonObject();
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

				properties.add(property);
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

	private JsonElement serializeFlatNameValue(GraphObject src, Type typeOfSrc, JsonSerializationContext context, PropertyView localPropertyView, int depth) {

		// prevent endless recursion by pruning at depth 2
		if(depth > outputNestingDepth) {
			return null;
		}

		JsonObject jsonObject = new JsonObject();

		// id
		jsonObject.add("id", new JsonPrimitive(src.getId()));

		String type = src.getType();
		if(type != null) {
			jsonObject.add("type", new JsonPrimitive(type));
		}

		// property keys
		for(String key : src.getPropertyKeys(localPropertyView)) {

			Object value = src.getProperty(key);
			if(value != null) {

				if(value instanceof List) {

					JsonArray property = new JsonArray();
					List<GraphObject> values = (List<GraphObject>)value;
					for(GraphObject obj : values) {
						JsonElement recursiveSerializedValue = this.serializeFlatNameValue(obj, typeOfSrc, context, localPropertyView, depth+1);
						if(recursiveSerializedValue != null) {
							property.add(recursiveSerializedValue);
						}
					}

					jsonObject.add(key, property);

				} else {

					jsonObject.add(key, new JsonPrimitive(value.toString()));
				}

			} else {

				jsonObject.add(key, new JsonNull());
			}
		}
		
		return jsonObject;
	}
}
