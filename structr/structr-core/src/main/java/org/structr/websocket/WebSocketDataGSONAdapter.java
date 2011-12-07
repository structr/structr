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

package org.structr.websocket;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import java.util.Map.Entry;

/**
 *
 * @author Christian Morgner
 */
public class WebSocketDataGSONAdapter implements JsonSerializer<WebSocketData>, JsonDeserializer<WebSocketData> {

	@Override
	public JsonElement serialize(WebSocketData src, Type typeOfSrc, JsonSerializationContext context) {

		JsonObject root = new JsonObject();
		JsonObject data = new JsonObject();

		if(src.getCommand() != null)	{ root.add("command", new JsonPrimitive(src.getCommand())); }
		if(src.getId() != null)		{ root.add("id", new JsonPrimitive(src.getId())); }
		if(src.getCallback() != null)	{ root.add("callback", new JsonPrimitive(src.getCallback())); }
		if(src.getButton() != null)	{ root.add("button",   new JsonPrimitive(src.getButton())); }
		if(src.getParent() != null)	{ root.add("parent",   new JsonPrimitive(src.getParent())); }
		if(src.getView() != null)	{ root.add("view",     new JsonPrimitive(src.getView())); }

		if(src.getData() != null) {

			for(Entry<String, String> entry : src.getData().entrySet()) {
				String value = entry.getValue();
				String key = entry.getKey();
				if(value != null) {
					data.add(key, new JsonPrimitive(value));
				}
			}

			root.add("data", data);
		}

		return root;
	}

	@Override
	public WebSocketData deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {

		WebSocketData webSocketData = new WebSocketData();

		if(json instanceof JsonObject) {

			JsonObject root = json.getAsJsonObject();
			JsonObject data = root.getAsJsonObject("data");

			if(root.has("command"))		{ webSocketData.setCommand(root.getAsJsonPrimitive("command").getAsString()); }
			if(root.has("id"))		{ webSocketData.setId(root.getAsJsonPrimitive("id").getAsString()); }
			if(root.has("callback"))	{ webSocketData.setCallback(root.getAsJsonPrimitive("callback").getAsString()); }
			if(root.has("button"))		{ webSocketData.setButton(root.getAsJsonPrimitive("button").getAsString()); }
			if(root.has("parent"))		{ webSocketData.setParent(root.getAsJsonPrimitive("parent").getAsString()); }
			if(root.has("view"))		{ webSocketData.setView(root.getAsJsonPrimitive("view").getAsString()); }

			if(data != null) {
				for(Entry<String, JsonElement> entry : data.entrySet()) {
					webSocketData.setData(entry.getKey(), entry.getValue().getAsString());
				}
			}
		}

		return webSocketData;
	}
}
