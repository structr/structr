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
package org.structr.bolt;

import java.util.*;

/**
 *
 */
class MapResultWrapper implements Map<String, Object> {

	protected Map<String, Object> source = null;
	protected MixedResultWrapper wrapper = null;
	protected BoltDatabaseService db     = null;

	public MapResultWrapper(final BoltDatabaseService db, final Map<String, Object> source) {

		this.wrapper = new MixedResultWrapper<>(db);
		this.source  = source;
		this.db      = db;
	}

	@Override
	public int size() {
		return source.size();
	}

	@Override
	public boolean isEmpty() {
		return source.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return source.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return source.containsValue(value);
	}

	@Override
	public Object get(Object key) {
		return wrap(source.get(key));
	}

	@Override
	public Object put(String key, Object value) {
		throw new UnsupportedOperationException("This result object is read-only");
	}

	@Override
	public Object remove(Object key) {
		throw new UnsupportedOperationException("This result object is read-only");
	}

	@Override
	public void putAll(Map<? extends String, ? extends Object> m) {
		throw new UnsupportedOperationException("This result object is read-only");
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException("This result object is read-only");
	}

	@Override
	public Set<String> keySet() {
		return source.keySet();
	}

	@Override
	public Collection<Object> values() {
		return wrap(source.values());
	}

	@Override
	public Set<Entry<String, Object>> entrySet() {

		final Set<Entry<String, Object>> wrappedSet = new LinkedHashSet<>();

		for (final Entry<String, Object> entry : source.entrySet()) {

			wrappedSet.add(new WrappedEntry(entry));
		}

		return wrappedSet;
	}

	// ----- private methods -----
	private Object wrap(final Object value) {

		if (value instanceof Map) {

			return new MapResultWrapper(db, (Map)value);
		}

		if (value instanceof Collection) {

			return wrap((Collection)value);
		}

		return wrapper.apply(value);
	}

	private Collection<Object> wrap(final Collection<Object> source) {

		final List<Object> values = new ArrayList<>();

		for (final Object obj : source) {
			values.add(wrap(obj));
		}

		return values;
	}

	// ----- nested classes -----
	private class WrappedEntry implements Entry<String, Object> {

		private Entry<String, Object> source = null;

		public WrappedEntry(final Entry<String, Object> source) {
			this.source = source;
		}

		@Override
		public String getKey() {
			return source.getKey();
		}

		@Override
		public Object getValue() {
			return wrap(source.getValue());
		}

		@Override
		public Object setValue(Object value) {
			throw new UnsupportedOperationException("This result object is read-only");
		}
	}
}
