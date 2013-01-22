/**
 * Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 * This file is part of structr <http://structr.org>.
 *
 * structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.websocket.message;

import java.util.Map;
import java.util.Map.Entry;

/**
 *
 * @author Christian Morgner
 */
public class MessageBuilder {

	private WebSocketMessage data = null;

	public MessageBuilder() {
		data = new WebSocketMessage();
	}

	// ----- static methods -----
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

	public WebSocketMessage build() {
		return data;
	}

	// ----- private methods -----
	private static MessageBuilder builder() {
		return new MessageBuilder();
	}
}
