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

package org.structr.websocket.message;

/**
 *
 * @author Christian Morgner
 */
public class MessageBuilder {

	private WebSocketMessage data = null;
	private String command = null;

	public MessageBuilder() {
		data = new WebSocketMessage();
	}

	// ----- static methods -----
	public static MessageBuilder status() {
		return builder().command("STATUS");
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

	public MessageBuilder message(String message) {
		data.setMessage(message);
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
