/*
 * Copyright (C) 2010-2024 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.websocket.message;

import com.google.gson.JsonElement;

import java.util.Map;
import java.util.Map.Entry;

/**
 *
 *
 */
public class MessageBuilder {

	private WebSocketMessage data = null;

	public MessageBuilder() {
		data = new WebSocketMessage();
	}

	// ----- static methods -----
	public static MessageBuilder progress() {
		return builder().command("PROGRESS");
	}

	public static MessageBuilder finished() {
		return builder().command("FINISHED");
	}

	public static MessageBuilder status() {
		return builder().command("STATUS");
	}

	public static MessageBuilder create() {
		return builder().command("CREATE");
	}

	public static MessageBuilder delete() {
		return builder().command("DELETE");
	}

	public static MessageBuilder update() {
		return builder().command("UPDATE");
	}

	public static MessageBuilder wrappedRest() {
		return builder().command("WRAPPED_REST");
	}

	public static MessageBuilder forName(final String command) {
		return builder().command(command);
	}

	// ----- non-static methods -----
	public MessageBuilder command(String command) {
		data.setCommand(command);
		return this;
	}

	public MessageBuilder code(int code) {
		data.setCode(code);
		return this;
	}

	public MessageBuilder id(String id) {
		data.setId(id);
		return this;
	}

	public MessageBuilder message(String message) {
		data.setMessage(message);
		return this;
	}

	public MessageBuilder callback(String callback) {
		if (callback != null) {
			data.setCallback(callback);
		}
		return this;
	}

	public MessageBuilder data(String key, Object value) {
		data.setNodeData(key, value);
		return this;
	}

	public MessageBuilder data(Map<String, Object> data) {
		for(Entry<String, Object> entry : data.entrySet()) {
			this.data.setNodeData(entry.getKey(), entry.getValue());
		}
		return this;
	}

	public MessageBuilder jsonErrorObject(JsonElement error) {
		data.setJsonErrorObject(error);
		return this;
	}

	public WebSocketMessage build() {
		return data;
	}

	// ----- private methods -----
	private static MessageBuilder builder() {
		return new MessageBuilder();
	}
}
