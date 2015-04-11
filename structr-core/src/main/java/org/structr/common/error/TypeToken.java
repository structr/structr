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
 * Indicates that a property value has the wrong type.
 *
 * @author Christian Morgner
 */
public class TypeToken extends SemanticErrorToken {

	private String type = null;

	public TypeToken(PropertyKey propertyKey, String type) {

		super(propertyKey);
		this.type = type;
	}

	@Override
	public JsonElement getContent() {

		JsonObject obj = new JsonObject();

		obj.add(getErrorToken(), new JsonPrimitive(type));

		return obj;
	}

	@Override
	public String getErrorToken() {
		return "must_be_of_type";
	}
}
