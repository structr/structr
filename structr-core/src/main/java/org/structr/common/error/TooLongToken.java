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
 * Indicates that the length of a property value is too large.
 *
 * @author Christian Morgner
 */
public class TooLongToken extends SemanticErrorToken {

	private int maxLength = -1;

	public TooLongToken(PropertyKey propertyKey, int maxLength) {

		super(propertyKey);
		this.maxLength = maxLength;
	}

	@Override
	public JsonElement getContent() {

		JsonObject obj = new JsonObject();

		obj.add(getErrorToken(), new JsonPrimitive(maxLength));

		return obj;
	}

	@Override
	public String getErrorToken() {
		return "must_be_shorter_than";
	}
}
