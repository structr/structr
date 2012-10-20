/*
 *  Copyright (C) 2010-2012 Axel Morgner, structr <structr@structr.org>
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
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.Map;
import java.util.Map.Entry;
import org.structr.common.PropertyKey;

/**
 *
 * @author Christian Morgner
 */
public class PropertiesNotFoundToken extends NotFoundToken {

	private Map<String, Object> attributes = null;

	public PropertiesNotFoundToken(PropertyKey key, Map<String, Object> attributes) {
		super(key);
		this.attributes = attributes;
	}

	@Override
	public JsonElement getContent() {

		JsonObject obj = new JsonObject();
		JsonObject vals = new JsonObject();

		for(Entry<String, Object> entry : attributes.entrySet()) {

			String key = entry.getKey();
			Object value = entry.getValue();
			
			if (value == null) {
				vals.add(key, new JsonNull());
			} else {
				vals.add(key, new JsonPrimitive(value.toString()));
			}
		}

		obj.add(getErrorToken(), vals);

		return obj;
	}

	@Override
	public String getErrorToken() {
		return "object_not_found";
	}
}
