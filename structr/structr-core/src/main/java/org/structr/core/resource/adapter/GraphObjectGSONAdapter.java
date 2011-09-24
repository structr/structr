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
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Map.Entry;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.RelationshipType;
import org.structr.core.GraphObject;
import org.structr.core.entity.DummyNode;

/**
 * Controls serialization and deserialization of structr nodes.
 *
 * @author Christian Morgner
 */
public class GraphObjectGSONAdapter implements InstanceCreator<GraphObject>, JsonSerializer<GraphObject>, JsonDeserializer<GraphObject> {

	private ThreadLocal<OutputMode> threadLocalOutputMode = new ThreadLocal<OutputMode>();

	public enum OutputMode {
		NestedKeyValue,			// "properties" : [ { "key" : "name", "value" : "Test" }, ... ]
		NestedKeyValueType,		// "properties" : [ { "key" : "name", "value" : "Test", "type" : "String" }, ... ]
		FlatNameValue			// { "name" : "Test" }
	}

	public void setOutputMode(OutputMode outputMode) {
		this.threadLocalOutputMode.set(outputMode);
	}

	public OutputMode getOutputMode() {
		return this.threadLocalOutputMode.get();
	}

	@Override
	public GraphObject createInstance(Type type) {
		return new DummyNode();
	}

	@Override
	public JsonElement serialize(GraphObject src, Type typeOfSrc, JsonSerializationContext context) {

		OutputMode outputMode = threadLocalOutputMode.get();
		if(outputMode == null) {
			outputMode = OutputMode.NestedKeyValueType;
		}

		JsonElement serializedOutput = null;

		switch(threadLocalOutputMode.get()) {

			case NestedKeyValueType:
				serializedOutput = serializeNestedKeyValueType(src, typeOfSrc, context, true);
				break;

			case NestedKeyValue:
				serializedOutput = serializeNestedKeyValueType(src, typeOfSrc, context, false);
				break;

			case FlatNameValue:
				serializedOutput = serializeFlatNameValue(src, typeOfSrc, context);
				break;
		}

		return serializedOutput;
	}

	@Override
	public GraphObject deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {

		return null;
	}

	// ----- private methods -----
	private JsonElement serializeNestedKeyValueType(GraphObject src, Type typeOfSrc, JsonSerializationContext context, boolean includeTypeInOutput) {

		JsonObject jsonObject = new JsonObject();

		// id
		jsonObject.add("id", new JsonPrimitive(src.getId()));

		String type = src.getType();
		if(type != null) {
			jsonObject.add("type", new JsonPrimitive(type));
		}

		// property keys
		JsonArray properties = new JsonArray();
		for(String key : src.getPropertyKeys()) {

			Object value = src.getProperty(key);
			if(value != null) {

				JsonObject property = new JsonObject();
				property.add("key", new JsonPrimitive(key));
				property.add("value", new JsonPrimitive(value.toString()));

				// include type?
				if(includeTypeInOutput) {
					String valueType = value.getClass().getSimpleName();
					property.add("type", new JsonPrimitive(valueType));
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

	private JsonElement serializeFlatNameValue(GraphObject src, Type typeOfSrc, JsonSerializationContext context) {

		JsonObject jsonObject = new JsonObject();

		// id
		jsonObject.add("id", new JsonPrimitive(src.getId()));

		String type = src.getType();
		if(type != null) {
			jsonObject.add("type", new JsonPrimitive(type));
		}

		// property keys
		for(String key : src.getPropertyKeys()) {

			Object value = src.getProperty(key);
			if(value != null) {
				jsonObject.add(key, new JsonPrimitive(value.toString()));
			}
		}

		/*
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
		*/
		
		return jsonObject;
	}
}
