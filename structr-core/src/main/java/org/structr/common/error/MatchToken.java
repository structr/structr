/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
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
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.common.error;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.structr.core.property.PropertyKey;

/**
 * Indicates that a property value must match a given expression.
 *
 * @author Christian Morgner
 */
public class MatchToken extends SemanticErrorToken {

	private Object value      = null;
	private String expression = null;

	public MatchToken(PropertyKey propertyKey, Object value, String expression) {

		super(propertyKey);
		this.value      = value;
		this.expression = expression;
	}

	@Override
	public JsonElement getContent() {

		JsonObject obj = new JsonObject();

		obj.add("value",         value != null ? new JsonPrimitive(value.toString()) : JsonNull.INSTANCE);
		obj.add(getErrorToken(), new JsonPrimitive(expression));

		return obj;
	}

	@Override
	public String getErrorToken() {
		return "must_match";
	}
}
