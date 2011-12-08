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
import java.util.Map.Entry;
import org.structr.common.PropertyView;
import org.structr.core.GraphObject;
import org.structr.core.Value;
import org.structr.core.validator.GenericValue;
import org.structr.rest.adapter.GraphObjectGSONAdapter;
import org.structr.rest.wrapper.PropertySet.PropertyFormat;

/**
 *
 * @author Christian Morgner
 */
public class WebSocketDataGSONAdapter implements JsonSerializer<WebSocketMessage>, JsonDeserializer<WebSocketMessage> {

	private Value<String> propertyView = new GenericValue<String>(PropertyView.All);
	private GraphObjectGSONAdapter graphObjectSerializer = null;

	public WebSocketDataGSONAdapter(String idProperty) {
		graphObjectSerializer = new GraphObjectGSONAdapter(PropertyFormat.FlatNameValue, propertyView, idProperty);
	}

	@Override
	public JsonElement serialize(WebSocketMessage src, Type typeOfSrc, JsonSerializationContext context) {

		JsonObject root = new JsonObject();
		JsonObject data = new JsonObject();

		if(src.getCommand() != null)	{ root.add("command",  new JsonPrimitive(src.getCommand())); }
		if(src.getId() != null)		{ root.add("id",       new JsonPrimitive(src.getId())); }
		if(src.getToken() != null)	{ root.add("token",    new JsonPrimitive(src.getToken())); }
		if(src.getCallback() != null)	{ root.add("callback", new JsonPrimitive(src.getCallback())); }
		if(src.getButton() != null)	{ root.add("button",   new JsonPrimitive(src.getButton())); }
		if(src.getParent() != null)	{ root.add("parent",   new JsonPrimitive(src.getParent())); }
		if(src.getView() != null)	{ root.add("view",     new JsonPrimitive(src.getView())); }
		if(src.getSortKey() != null)	{ root.add("sort",     new JsonPrimitive(src.getSortKey())); }
		if(src.getSortOrder() != null)	{ root.add("order",    new JsonPrimitive(src.getSortOrder())); }
		if(src.getPageSize() > 0)	{ root.add("pageSize", new JsonPrimitive(src.getPageSize())); }
		if(src.getPage() > 0)		{ root.add("page",     new JsonPrimitive(src.getPage())); }

		// serialize session valid flag (output only)
		root.add("sessionValid", new JsonPrimitive(src.isSessionValid()));

		// serialize data
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

		// serialize result list
		if(src.getResult() != null) {

			if(src.getView() != null) {
				propertyView.set(src.getView());
			} else {
				propertyView.set(PropertyView.All);
			}

			JsonArray result = new JsonArray();
			for(GraphObject obj : src.getResult()) {
				result.add(graphObjectSerializer.serialize(obj, GraphObject.class, context));
			}

			root.add("result", result);
		}

		return root;
	}

	@Override
	public WebSocketMessage deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {

		WebSocketMessage webSocketData = new WebSocketMessage();

		if(json instanceof JsonObject) {

			JsonObject root = json.getAsJsonObject();
			JsonObject data = root.getAsJsonObject("data");

			if(root.has("command"))		{ webSocketData.setCommand(root.getAsJsonPrimitive("command").getAsString()); }
			if(root.has("id"))		{ webSocketData.setId(root.getAsJsonPrimitive("id").getAsString()); }
			if(root.has("token"))		{ webSocketData.setToken(root.getAsJsonPrimitive("token").getAsString()); }
			if(root.has("callback"))	{ webSocketData.setCallback(root.getAsJsonPrimitive("callback").getAsString()); }
			if(root.has("button"))		{ webSocketData.setButton(root.getAsJsonPrimitive("button").getAsString()); }
			if(root.has("parent"))		{ webSocketData.setParent(root.getAsJsonPrimitive("parent").getAsString()); }
			if(root.has("view"))		{ webSocketData.setView(root.getAsJsonPrimitive("view").getAsString()); }
			if(root.has("sort"))		{ webSocketData.setSortKey(root.getAsJsonPrimitive("sort").getAsString()); }
			if(root.has("order"))		{ webSocketData.setSortOrder(root.getAsJsonPrimitive("order").getAsString()); }
			if(root.has("pageSize"))	{ webSocketData.setPageSize(root.getAsJsonPrimitive("pageSize").getAsInt()); }
			if(root.has("page"))		{ webSocketData.setPage(root.getAsJsonPrimitive("page").getAsInt()); }

			if(data != null) {
				for(Entry<String, JsonElement> entry : data.entrySet()) {
					webSocketData.setData(entry.getKey(), entry.getValue().getAsString());
				}
			}
		}

		return webSocketData;
	}
}
