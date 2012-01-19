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

/**
 *
 * @author Christian Morgner
 */
public class ChronologicalOrderToken extends SemanticErrorToken {

	private String propertyKey2 = null;

	public ChronologicalOrderToken(String propertyKey1, String propertyKey2) {
		super(propertyKey1);
		this.propertyKey2 = propertyKey2;
	}

	@Override
	public JsonElement getErrors() {

		JsonObject obj = new JsonObject();

		obj.add("must_lie_after", new JsonPrimitive(propertyKey2));

		return obj;
	}
}
