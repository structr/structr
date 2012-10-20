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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.structr.common.PropertyKey;

/**
 *
 * @author Christian Morgner
 */
public class ValueToken extends SemanticErrorToken {

	private Object[] values = null;

	public ValueToken(PropertyKey propertyKey, Object[] values) {
		super(propertyKey);
		this.values = values;
	}

	@Override
	public JsonElement getContent() {

		JsonObject obj = new JsonObject();
		JsonArray array = new JsonArray();
		
		for(Object o : values) {
			array.add(new JsonPrimitive(o.toString()));
		}

		obj.add(getErrorToken(), array);

		return obj;
	}

	@Override
	public String getErrorToken() {
		return "must_be_one_of";
	}
}
