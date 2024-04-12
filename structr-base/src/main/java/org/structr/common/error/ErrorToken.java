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
import java.util.LinkedHashMap;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;

import java.util.Map;

/**
 * Abstract base class for all error tokens.
 *
 *
 */
public abstract class ErrorToken {

	protected final Map<String, Object> data = new LinkedHashMap<>();

	public ErrorToken(final String token) {
		data.put("token", token);
	}

	public ErrorToken with(final String key, final Object value) {
		data.put(key, value);
		return this;
	}

	public ErrorToken with(final String key1, final Object value1, final String key2, final Object value2) {

		data.put(key1, value1);
		data.put(key2, value2);

		return this;
	}

	public ErrorToken with(final String key1, final Object value1, final String key2, final Object value2, final String key3, final Object value3) {

		data.put(key1, value1);
		data.put(key2, value2);
		data.put(key3, value3);

		return this;
	}

	public ErrorToken with(final String key1, final Object value1, final String key2, final Object value2, final String key3, final Object value3, final String key4, final Object value4) {

		data.put(key1, value1);
		data.put(key2, value2);
		data.put(key3, value3);
		data.put(key4, value4);

		return this;
	}

	public ErrorToken with(final String key1, final Object value1, final String key2, final Object value2, final String key3, final Object value3, final String key4, final Object value4, final String key5, final Object value5) {

		data.put(key1, value1);
		data.put(key2, value2);
		data.put(key3, value3);
		data.put(key4, value4);
		data.put(key5, value5);

		return this;
	}

	public ErrorToken withType(final String type) {
		return with("type", type);
	}

	public ErrorToken withProperty(final String property) {
		return with("property", property);
	}

	public ErrorToken withDetail(final Object detail) {
		return with("detail", detail);
	}

	public ErrorToken withValue(final Object value) {
		return with("value", value);
	}

	public String getProperty() {
		return (String)data.get("property");
	}

	public String getType() {
		return (String)data.get("type");
	}

	public String getToken() {
		return (String)data.get("token");
	}

	public Object getDetail() {
		return data.get("detail");
	}

	public Object getValue() {
		return data.get("value");
	}

	public void setValue(final Object value) {
		data.put("value", value);
	}

	public JsonObject toJSON() {

		final JsonObject token = new JsonObject();

		for (final String key : data.keySet()) {

			addIfNonNull(token, key, getObjectOrNull(data.get(key)));
		}

		/*
		addIfNonNull(token, "type",     getStringOrNull(getType()));
		addIfNonNull(token, "property", getStringOrNull(getProperty()));
		addIfNonNull(token, "value",    getObjectOrNull(getValue()));
		addIfNonNull(token, "token",    getStringOrNull(getToken()));
		addIfNonNull(token, "detail",   getObjectOrNull(getDetail()));
		*/

		return token;
	}

	@Override
	public String toString() {

		final StringBuilder buf = new StringBuilder();

		final String type = getType();
		if (type != null) {

			buf.append(type);
		}

		final String property = getProperty();
		if (property != null) {

			buf.append(".");
			buf.append(property);
		}

		final Object value = getValue();
		if (value != null) {

			buf.append(" ");
			buf.append(value);
		}

		final String token = getToken();
		if (token != null) {

			buf.append(" ");
			buf.append(token);
		}

		final Object detail = getDetail();
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

			if (source instanceof Iterable iterable) {

				final JsonArray array = new JsonArray();

				for (final Object o : iterable) {
					array.add(getObjectOrNull(o));
				}

				return array;
			}

			if (source instanceof Map sourceMap) {

				final Map<String, Object> map = sourceMap;
				final JsonObject object       = new JsonObject();

				for (final String key : map.keySet()) {

					final Object value = map.get(key);
					object.add(key, getObjectOrNull(value));
				}

				return object;
			}

			if (source instanceof PropertyMap map) {

				final JsonObject object = new JsonObject();

				for (final PropertyKey key : map.keySet()) {

					final Object value = map.get(key);

					object.add(key.jsonName(), getObjectOrNull(value));
				}

				return object;
			}

			if (source instanceof PropertyKey key) {
				return new JsonPrimitive(key.jsonName());
			}

			if (source instanceof String string) {
				return new JsonPrimitive(string);
			}

			if (source instanceof Number number) {
				return new JsonPrimitive(number);
			}

			if (source instanceof Boolean bool) {
				return new JsonPrimitive(bool);
			}

			return new JsonPrimitive(source.toString());
		}

		return JsonNull.INSTANCE;
	}
}
