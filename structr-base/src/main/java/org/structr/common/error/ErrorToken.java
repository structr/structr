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

import com.google.gson.*;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;

import java.util.Map;

/**
 * Abstract base class for all error tokens.
 *
 *
 */
public abstract class ErrorToken {

	private String property = null;
	private String type     = null;
	private Object detail   = null;
	private String token    = null;
	private Object value    = null;

	public ErrorToken(final String type, final String property, final String token, final Object detail) {
		this.type     = type;
		this.property = property;
		this.token    = token;
		this.detail   = detail;
	}

	public ErrorToken(final String type, final String property, final String token, final Object detail, final Object value) {
		this.type     = type;
		this.property = property;
		this.token    = token;
		this.detail   = detail;
		this.value    = value;
	}

	public String getProperty() {
		return property;
	}

	public String getType() {
		return type;
	}

	public String getToken() {
		return token;
	}

	public Object getDetail() {
		return detail;
	}

	public Object getValue() {
		return value;
	}

	public void setValue(final Object value) {
		this.value = value;
	}

	public JsonObject toJSON() {

		final JsonObject token = new JsonObject();

		token.add("type",     getStringOrNull(getType()));
		token.add("property", getStringOrNull(getProperty()));

		// optional value
		addIfNonNull(token, "value",  getObjectOrNull(getValue()));

		token.add("token",    getStringOrNull(getToken()));

		// optional detail
		addIfNonNull(token, "detail", getObjectOrNull(getDetail()));

		return token;
	}

	@Override
	public String toString() {

		final StringBuilder buf = new StringBuilder();

		if (type != null) {

			buf.append(type);
		}

		if (property != null) {

			buf.append(".");
			buf.append(property);
		}

		if (value != null) {

			buf.append(" ");
			buf.append(value);
		}

		if (token != null) {

			buf.append(" ");
			buf.append(token);
		}

		if (detail != null) {

			buf.append(" ");
			buf.append(detail);
		}

		return buf.toString();
	}

	// ----- protected methods -----
	protected void addIfNonNull(final JsonObject obj, final String key, final JsonElement value) {

		if (value != null && !JsonNull.INSTANCE.equals(value)) {

			obj.add(key, value);
		}
	}


	protected JsonElement getStringOrNull(final String source) {

		if (source != null) {
			return new JsonPrimitive(source);
		}

		return JsonNull.INSTANCE;
	}

	protected JsonElement getObjectOrNull(final Object source) {

		if (source != null) {

			if (source instanceof Iterable) {

				final Iterable iterable = (Iterable)source;
				final JsonArray array   = new JsonArray();

				for (final Object o : iterable) {
					array.add(getObjectOrNull(o));
				}

				return array;
			}

			if (source instanceof Map) {

				final Map<String, Object> map = (Map)source;
				final JsonObject object       = new JsonObject();

				for (final String key : map.keySet()) {

					final Object value = map.get(key);
					object.add(key, getObjectOrNull(value));
				}

				return object;
			}

			if (source instanceof PropertyMap) {

				final PropertyMap map    = (PropertyMap)source;
				final JsonObject object       = new JsonObject();

				for (final PropertyKey key : map.keySet()) {

					final Object value = map.get(key);

					object.add(key.jsonName(), getObjectOrNull(value));
				}

				return object;
			}

			if (source instanceof String) {
				return new JsonPrimitive((String)source);
			}

			if (source instanceof Number) {
				return new JsonPrimitive((Number)source);
			}

			if (source instanceof Boolean) {
				return new JsonPrimitive((Boolean)source);
			}

			return new JsonPrimitive(source.toString());
		}

		return JsonNull.INSTANCE;
	}
}
