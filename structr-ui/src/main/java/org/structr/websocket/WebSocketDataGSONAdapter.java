/**
 * Copyright (C) 2010-2016 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.websocket;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.PropertyView;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.StaticValue;
import org.structr.core.Value;
import org.structr.core.property.PropertyKey;
import org.structr.rest.GraphObjectGSONAdapter;
import org.structr.rest.JsonInputGSONAdapter;
import org.structr.websocket.message.WebSocketMessage;

//~--- classes ----------------------------------------------------------------

/**
 *
 *
 */
public class WebSocketDataGSONAdapter implements JsonSerializer<WebSocketMessage>, JsonDeserializer<WebSocketMessage> {

	private static final Logger logger                   = Logger.getLogger(WebSocketDataGSONAdapter.class.getName());
	private final Value<String> propertyView             = new StaticValue<>(PropertyView.Public);
	private GraphObjectGSONAdapter graphObjectSerializer = null;

	//~--- constructors ---------------------------------------------------

	public WebSocketDataGSONAdapter(final int outputNestingDepth) {
		graphObjectSerializer = new GraphObjectGSONAdapter(propertyView, outputNestingDepth);
	}

	//~--- methods --------------------------------------------------------

	@Override
	public JsonElement serialize(WebSocketMessage src, Type typeOfSrc, JsonSerializationContext context) {

		JsonObject root             = new JsonObject();
		JsonObject jsonNodeData     = new JsonObject();
		JsonObject jsonRelData      = new JsonObject();
		JsonArray removedProperties = new JsonArray();
		JsonArray modifiedProperties = new JsonArray();

		if (src.getCommand() != null) {

			root.add("command", new JsonPrimitive(src.getCommand()));
		}

		if (src.getId() != null) {

			root.add("id", new JsonPrimitive(src.getId()));
		}

		if (src.getPageId() != null) {

			root.add("pageId", new JsonPrimitive(src.getPageId()));
		}

		if (src.getMessage() != null) {

			root.add("message", new JsonPrimitive(src.getMessage()));
		}

		if (src.getJsonErrorObject() != null) {
			root.add("error", src.getJsonErrorObject());
		}

		if (src.getCode() != 0) {

			root.add("code", new JsonPrimitive(src.getCode()));
		}

		if (src.getSessionId() != null) {

			root.add("sessionId", new JsonPrimitive(src.getSessionId()));
		}

		if (src.getCallback() != null) {

			root.add("callback", new JsonPrimitive(src.getCallback()));
		}

		if (src.getButton() != null) {

			root.add("button", new JsonPrimitive(src.getButton()));
		}

		if (src.getParent() != null) {

			root.add("parent", new JsonPrimitive(src.getParent()));
		}

		if (src.getView() != null) {

			root.add("view", new JsonPrimitive(src.getView()));
		}

		if (src.getSortKey() != null) {

			root.add("sort", new JsonPrimitive(src.getSortKey()));
		}

		if (src.getSortOrder() != null) {

			root.add("order", new JsonPrimitive(src.getSortOrder()));
		}

		if (src.getPageSize() > 0) {

			root.add("pageSize", new JsonPrimitive(src.getPageSize()));
		}

		if (src.getPage() > 0) {

			root.add("page", new JsonPrimitive(src.getPage()));
		}

		JsonArray nodesWithChildren = new JsonArray();
		Set<String> nwc             = src.getNodesWithChildren();

		if ((nwc != null) &&!src.getNodesWithChildren().isEmpty()) {

			for (String nodeId : nwc) {

				nodesWithChildren.add(new JsonPrimitive(nodeId));
			}

			root.add("nodesWithChildren", nodesWithChildren);

		}

		// serialize session valid flag (output only)
		root.add("sessionValid", new JsonPrimitive(src.isSessionValid()));

		// UPDATE only, serialize only removed and modified properties and use the correct values
		if ((src.getGraphObject() != null)) {

			if (!src.getModifiedProperties().isEmpty()) {

				for (PropertyKey modifiedKey : src.getModifiedProperties()) {
					modifiedProperties.add(toJsonPrimitive(modifiedKey));
				}

				root.add("modifiedProperties", modifiedProperties);

			}

			if (!src.getRemovedProperties().isEmpty()) {

				for (PropertyKey removedKey : src.getRemovedProperties()) {
					removedProperties.add(toJsonPrimitive(removedKey));
				}

				root.add("removedProperties", removedProperties);

			}

		}

		// serialize node data
		if (src.getNodeData() != null) {

			for (Entry<String, Object> entry : src.getNodeData().entrySet()) {

				Object value = entry.getValue();
				String key   = entry.getKey();

				if (value != null) {

					jsonNodeData.add(key, toJsonPrimitive(value));
				}

			}

			root.add("data", jsonNodeData);

		}

		// serialize relationship data
		if (src.getRelData() != null) {

			for (Entry<String, Object> entry : src.getRelData().entrySet()) {

				Object value = entry.getValue();
				String key   = entry.getKey();

				if (value != null) {

					jsonRelData.add(key, toJsonPrimitive(value));
				}

			}

			root.add("relData", jsonRelData);

		}

		// serialize result list
		if (src.getResult() != null) {

			if (src.getView() != null) {

				try {
					propertyView.set(null, src.getView());

				} catch(FrameworkException fex) {

					logger.log(Level.WARNING, "Unable to set property view", fex);
				}

			} else {

				try {
					propertyView.set(null, PropertyView.Ui);

				} catch(FrameworkException fex) {

					logger.log(Level.WARNING, "Unable to set property view", fex);
				}

			}

			JsonArray result = new JsonArray();

			for (GraphObject obj : src.getResult()) {

				result.add(graphObjectSerializer.serialize(obj, System.currentTimeMillis()));
			}

			root.add("result", result);
			root.add("rawResultCount", toJsonPrimitive(src.getRawResultCount()));

		}

		return root;

	}

	private JsonPrimitive toJsonPrimitive(final Object value) {

		JsonPrimitive jp;

		if (value instanceof PropertyKey) {

			jp = new JsonPrimitive(((PropertyKey)value).jsonName());
		} else if (value instanceof String) {

			jp = new JsonPrimitive((String) value);
		} else if (value instanceof Number) {

			jp = new JsonPrimitive((Number) value);
		} else if (value instanceof Boolean) {

			jp = new JsonPrimitive((Boolean) value);
		} else if (value instanceof Character) {

			jp = new JsonPrimitive((Character) value);
		} else {

			jp = new JsonPrimitive(value.toString());
		}

		return jp;

	}

	@Override
	public WebSocketMessage deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {

		WebSocketMessage webSocketData = new WebSocketMessage();

		if (json instanceof JsonObject) {

			JsonObject root     = json.getAsJsonObject();
			JsonObject nodeData = root.getAsJsonObject("data");
			JsonObject relData  = root.getAsJsonObject("relData");

			if (root.has("command")) {

				webSocketData.setCommand(root.getAsJsonPrimitive("command").getAsString());
			}

			if (root.has("id")) {

				webSocketData.setId(root.getAsJsonPrimitive("id").getAsString());
			}

			if (root.has("pageId")) {

				webSocketData.setPageId(root.getAsJsonPrimitive("pageId").getAsString());
			}

			if (root.has("sessionId")) {
				JsonPrimitive sessionId = root.getAsJsonPrimitive("sessionId");
				if (sessionId != null) {
					webSocketData.setSessionId(sessionId.getAsString());
				}
			}

			if (root.has("callback")) {

				webSocketData.setCallback(root.getAsJsonPrimitive("callback").getAsString());
			}

			if (root.has("button")) {

				webSocketData.setButton(root.getAsJsonPrimitive("button").getAsString());
			}

			if (root.has("parent")) {

				webSocketData.setParent(root.getAsJsonPrimitive("parent").getAsString());
			}

			if (root.has("view")) {

				webSocketData.setView(root.getAsJsonPrimitive("view").getAsString());
			}

			if (root.has("sort")) {

				webSocketData.setSortKey(root.getAsJsonPrimitive("sort").getAsString());
			}

			if (root.has("order")) {

				webSocketData.setSortOrder(root.getAsJsonPrimitive("order").getAsString());
			}

			if (root.has("pageSize")) {

				webSocketData.setPageSize(root.getAsJsonPrimitive("pageSize").getAsInt());
			}

			if (root.has("page")) {

				webSocketData.setPage(root.getAsJsonPrimitive("page").getAsInt());
			}

			if (nodeData != null) {

				for (Entry<String, JsonElement> entry : nodeData.entrySet()) {

					final JsonElement obj = entry.getValue();
					Object value          = null;

					if (obj instanceof JsonPrimitive) {

						value = JsonInputGSONAdapter.fromPrimitive(obj.getAsJsonPrimitive());

					} else if (obj instanceof JsonObject) {

						value = obj.toString();

					} else if (obj instanceof JsonNull) {

						value = null;

					} else if (value != null) {

						value = obj.getAsString();
					}



					webSocketData.setNodeData(entry.getKey(), value);
				}

			}

			if (relData != null) {

				for (Entry<String, JsonElement> entry : relData.entrySet()) {

					JsonElement obj = entry.getValue();

					if (obj instanceof JsonNull || obj.isJsonNull()) {

						webSocketData.setRelData(entry.getKey(), null);

					} else {

						try {
							webSocketData.setRelData(entry.getKey(), obj.getAsString());
						} catch (Throwable t) {
							webSocketData.setRelData(entry.getKey(), null);
						}
					}
				}
			}
		}

		return webSocketData;

	}

}
