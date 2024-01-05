/*
 * Copyright (C) 2010-2024 Structr GmbH
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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.structr.core.property.PropertyKey;

/**
 * Indicates that a specific property value already exists in the database.
 *
 *
 */
public class CompoundToken extends ErrorToken {

	private PropertyKey[] keys = null;

	public CompoundToken(final String type, final PropertyKey[] keys, final String uuid) {

		super(type, null, "already_taken", uuid);

		this.keys = keys;
	}

	@Override
	public Object getValue() {
		return keys;
	}

	@Override
	public JsonObject toJSON() {

		final JsonObject token = new JsonObject();
		final JsonArray array  = new JsonArray();

		// add all keys that form the compound index
		for (final PropertyKey key : keys) {
			array.add(new JsonPrimitive(key.jsonName()));
		}

		token.add("type",       getStringOrNull(getType()));
		token.add("properties", array);
		token.add("token",      getStringOrNull(getToken()));

		// optional
		addIfNonNull(token, "detail", getObjectOrNull(getDetail()));

		return token;
	}
}
