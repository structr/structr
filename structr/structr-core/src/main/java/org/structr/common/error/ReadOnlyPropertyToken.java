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
public class ReadOnlyPropertyToken implements ErrorToken {

	private String propertyName = null;

	public ReadOnlyPropertyToken(String propertyName) {
		this.propertyName = propertyName;
	}

	@Override
	public int getStatus() {
		return HttpServletResponse.SC_FORBIDDEN;
	}

	@Override
	public JsonElement getContent() {

		JsonObject obj = new JsonObject();

		obj.add(propertyName, new JsonPrimitive("is_read_only_property"));

		return obj;
	}
}
