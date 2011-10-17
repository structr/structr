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
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.DummyNode;

/**
 * Controls serialization and deserialization of structr nodes.
 *
 * @author Christian Morgner
 */
public class AbstractNodeGSONAdapter implements InstanceCreator<AbstractNode>, JsonSerializer<AbstractNode>, JsonDeserializer<AbstractNode> {

	@Override
	public AbstractNode createInstance(Type type) {
		return new DummyNode();
	}

	@Override
	public JsonElement serialize(AbstractNode src, Type typeOfSrc, JsonSerializationContext context) {

		JsonObject jsonObject = new JsonObject();

		// 1: id
		jsonObject.add("id", new JsonPrimitive(src.getId()));

		// 2: property keys
		JsonArray properties = new JsonArray();
		for(String key : src.getPropertyKeys()) {

			Object value = src.getProperty(key);
			if(value != null) {

				String type = value.getClass().getSimpleName();
				JsonObject property = new JsonObject();

				property.add("key", new JsonPrimitive(key));
				property.add("value", new JsonPrimitive(value.toString()));
				property.add("type", new JsonPrimitive(type));

				properties.add(property);
			}
		}
		jsonObject.add("properties", properties);

		// 3: outgoing relationships
		Map<RelationshipType, Long> outRelStatistics = src.getRelationshipInfo(Direction.OUTGOING);
		if(outRelStatistics != null) {

			JsonArray outRels = new JsonArray();

			for(Entry<RelationshipType, Long> entry : outRelStatistics.entrySet()) {

				RelationshipType type = entry.getKey();
				Long count = entry.getValue();

				JsonObject outRelEntry = new JsonObject();
				outRelEntry.add("type", new JsonPrimitive(type.name()));
				outRelEntry.add("count", new JsonPrimitive(count));

				outRels.add(outRelEntry);
			}
			jsonObject.add("outgoingRelationships", outRels);
		}

		// 4: incoming relationships
		Map<RelationshipType, Long> inRelStatistics = src.getRelationshipInfo(Direction.INCOMING);
		if(inRelStatistics != null) {

			JsonArray inRels = new JsonArray();

			for(Entry<RelationshipType, Long> entry : inRelStatistics.entrySet()) {

				RelationshipType type = entry.getKey();
				Long count = entry.getValue();

				JsonObject inRelEntry = new JsonObject();
				inRelEntry.add("type", new JsonPrimitive(type.name()));
				inRelEntry.add("count", new JsonPrimitive(count));

				inRels.add(inRelEntry);

			}
			jsonObject.add("incomingRelationships", inRels);
		}

		return jsonObject;
	}

	@Override
	public AbstractNode deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {

		return null;
	}
}
