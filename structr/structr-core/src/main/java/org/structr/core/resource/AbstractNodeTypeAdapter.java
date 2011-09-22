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

package org.structr.core.resource;

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
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.DummyNode;
import org.structr.core.entity.StructrRelationship;

/**
 * Controls serialization and deserialization of structr nodes.
 *
 * @author Christian Morgner
 */
public class AbstractNodeTypeAdapter implements InstanceCreator<AbstractNode>, JsonSerializer<AbstractNode>, JsonDeserializer<AbstractNode> {

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

				JsonObject property = new JsonObject();
				String type = "unknown";

				property.add("key", new JsonPrimitive(key));
				property.add("value", new JsonPrimitive(value.toString()));
				property.add("type", new JsonPrimitive(type));

				properties.add(property);
			}
		}
		jsonObject.add("properties", properties);

		// 3: outgoing relationships
		JsonArray outRels = new JsonArray();
		for(StructrRelationship outRel : src.getOutgoingRelationships()) {

			JsonObject rel = new JsonObject();
			rel.add("id", new JsonPrimitive(outRel.getId()));
			rel.add("type", new JsonPrimitive(outRel.getRelType().name()));
			rel.add("endNodeId", new JsonPrimitive(outRel.getEndNode().getId()));

			outRels.add(rel);
		}
		jsonObject.add("outgoingRelationships", outRels);

		// 4: incoming relationships
		JsonArray inRels = new JsonArray();
		for(StructrRelationship inRel : src.getIncomingRelationships()) {

			JsonObject rel = new JsonObject();
			rel.add("id", new JsonPrimitive(inRel.getId()));
			rel.add("type", new JsonPrimitive(inRel.getRelType().name()));
			rel.add("startNodeId", new JsonPrimitive(inRel.getStartNode().getId()));

			inRels.add(rel);

		}
		jsonObject.add("incomingRelationships", inRels);

		return jsonObject;
	}

	@Override
	public AbstractNode deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {

		return null;
	}
}
