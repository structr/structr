/**
 * Copyright (C) 2010-2019 Structr GmbH
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
package org.structr.api.util;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * An extended LinkedHashMap that records write operations that happened after
 * the map was initialized in order to be able to identify modified values.
 */
public class ChangeAwareMap implements Map<String, Object> {

	private final Map<String, Object> data = new LinkedHashMap<>();
	private final Set<String> modifiedKeys = new LinkedHashSet<>();

	public ChangeAwareMap() {
	}

	public ChangeAwareMap(final Map<String, Object> initialData) {
		data.putAll(initialData);
	}

	public ChangeAwareMap(final ChangeAwareMap initialData) {
		data.putAll(initialData.data);
	}

	@Override
	public void putAll(final Map<? extends String, ? extends Object> input) {

		for (final Entry<? extends String, ? extends Object> entry : input.entrySet()) {

			final String key   = entry.getKey();
			final Object value = entry.getValue();

			put(key, value);
		}
	}

	@Override
	public boolean containsKey(final Object key) {
		return data.containsKey(key);
	}

	@Override
	public Object get(final Object key) {
		return data.get(key);
	}

	@Override
	public Object put(final String key, final Object value) {
		modifiedKeys.add(key);
		return data.put(key, value);
	}

	@Override
	public Set<String> keySet() {
		return data.keySet();
	}

	public Set<String> getModifiedKeys() {
		return modifiedKeys;
	}

	@Override
	public Object remove(final Object key) {
		return data.remove(key);
	}

	// other map methods
	@Override
	public int size() {
		return data.size();
	}

	@Override
	public boolean isEmpty() {
		return data.isEmpty();
	}

	@Override
	public boolean containsValue(Object value) {
		return data.containsValue(value);
	}

	@Override
	public void clear() {
		data.clear();
		modifiedKeys.clear();
	}

	@Override
	public Collection<Object> values() {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public Set<Entry<String, Object>> entrySet() {
		throw new UnsupportedOperationException("Not supported.");
	}

	public Map<String, Object> immutable() {
		return Collections.unmodifiableMap(data);
	}
}
