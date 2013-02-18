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
package org.structr.common.error;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.structr.core.property.PropertyKey;

/**
 * Indicates that a required object could not be found.
 *
 * @author Christian Morgner
 */
public class PropertyNotFoundToken extends NotFoundToken {

	private Object value = null;

	public PropertyNotFoundToken(PropertyKey key, Object value) {
		super(key);
		this.value = value;
	}

	@Override
	public JsonElement getContent() {

		JsonObject obj = new JsonObject();
		JsonObject vals = new JsonObject();

		if (value != null) {
			vals.add(getKey(), new JsonPrimitive(value.toString()));
		}
		obj.add(getErrorToken(), vals);

		return obj;
	}

	@Override
	public String getErrorToken() {
		return "object_not_found";
	}
}
