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
package org.structr.core;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Wrapper class for JSON input via {@link JsonRestServlet}.
 *
 *
 */
public class JsonInput implements Map<String, Object> {

	private Map<String, Object> attributes = null;

	public JsonInput() {
		this.attributes = new LinkedHashMap<>();
	}

	/**
	 * Add a key-value-pair of type String to this property set.
	 *
	 * @param key the key
	 * @param value the value
	 */
	public void add(String key, Object value) {

		attributes.put(key, value);
	}

	public Map<String, Object> getAttributes() {
		return attributes;
	}

	@Override
	public String toString() {

		StringBuilder builder = new StringBuilder();

		for (Entry<String, Object> entry : attributes.entrySet()) {

			builder.append(entry.getKey()).append(" = '").append(entry.getValue()).append("', ");
		}

		return builder.toString();
	}

	// ----- interface Map -----
	@Override
	public int size() {
		return attributes.size();
	}

	@Override
	public boolean isEmpty() {
		return attributes.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return attributes.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return attributes.containsValue(value);
	}

	@Override
	public Object get(Object key) {
		return attributes.get(key);
	}

	@Override
	public Object put(String key, Object value) {
		return attributes.put(key, value);
	}

	@Override
	public Object remove(Object key) {
		return attributes.remove(key);
	}

	@Override
	public void putAll(Map<? extends String, ? extends Object> m) {
		attributes.putAll(m);
	}

	@Override
	public void clear() {
		attributes.clear();
	}

	@Override
	public Set<String> keySet() {
		return attributes.keySet();
	}

	@Override
	public Collection<Object> values() {
		return attributes.values();
	}

	@Override
	public Set<Entry<String, Object>> entrySet() {
		return attributes.entrySet();
	}
}
