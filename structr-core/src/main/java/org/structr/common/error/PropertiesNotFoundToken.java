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
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.Map;
import java.util.Map.Entry;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;

/**
 * Indicates that a desired object could not be found.
 *
 * @author Christian Morgner
 */
public class PropertiesNotFoundToken extends NotFoundToken {

	private Map<PropertyKey, Object> attributes = null;

	public PropertiesNotFoundToken(PropertyKey key, PropertyMap attributes) {
		super(key);
		this.attributes = attributes.getRawMap();
	}

	public PropertiesNotFoundToken(PropertyKey key, Map<PropertyKey, Object> attributes) {
		super(key);
		this.attributes = attributes;
	}

	@Override
	public JsonElement getContent() {

		JsonObject obj = new JsonObject();
		JsonObject vals = new JsonObject();

		for(Entry<PropertyKey, Object> entry : attributes.entrySet()) {

			PropertyKey key = entry.getKey();
			Object value    = entry.getValue();
			
			if (value == null) {
				vals.add(key.jsonName(), null);
			} else {
				vals.add(key.jsonName(), new JsonPrimitive(value.toString()));
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
