/*
 *  Copyright (C) 2012 Axel Morgner
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

package org.structr.common.error;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author Christian Morgner
 */
public class MessageToken implements ErrorToken {

	private int status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
	private String message = null;

	public MessageToken(String message) {
		this.message = message;
	}

	public MessageToken(int status, String message) {
		this.message = message;
		this.status = status;
	}

	@Override
	public int getStatus() {
		return status;
	}

	@Override
	public JsonElement getContent() {

		JsonObject obj = new JsonObject();
		obj.add("message", new JsonPrimitive(message));

		return obj;
	}
}
