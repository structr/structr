/**
 * Copyright (C) 2010-2015 Morgner UG (haftungsbeschränkt)
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
package org.structr.common.error;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.structr.core.property.PropertyKey;

/**
 * Indicates that a specific property value already exists in the database.
 * 
 * @author Christian Morgner
 */
public class UniqueToken extends SemanticErrorToken {

	private Object value = null;
        private String uuid = null;

	public UniqueToken(String uuid, PropertyKey propertyKey, Object value) {
		
		super(propertyKey);
                this.uuid = uuid;
		this.value = value;
	}

	@Override
	public JsonElement getContent() {

		JsonObject obj = new JsonObject();

                obj.add(getErrorToken(), new JsonPrimitive(value.toString()));
		obj.add("id", new JsonPrimitive(uuid.toString()));

		return obj;
	}

	@Override
	public String getErrorToken() {
		return "already_taken";
	}
}
