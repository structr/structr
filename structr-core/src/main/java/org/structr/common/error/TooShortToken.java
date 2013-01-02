/*
 *  Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
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
import org.structr.core.property.PropertyKey;

/**
 * Indicates that the length of a property value is too short.
 *
 * @author Christian Morgner
 */
public class TooShortToken extends SemanticErrorToken {

	private int minLength = -1;

	public TooShortToken(PropertyKey<String> propertyKey, int minLength) {

		super(propertyKey);
		this.minLength = minLength;
	}

	@Override
	public JsonElement getContent() {

		JsonObject obj = new JsonObject();

		obj.add(getErrorToken(), new JsonPrimitive(minLength - 1));

		return obj;
	}

	@Override
	public String getErrorToken() {
		return "must_be_longer_than";
	}
}
