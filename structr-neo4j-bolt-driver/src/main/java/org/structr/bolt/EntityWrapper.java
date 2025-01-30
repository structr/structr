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

import org.neo4j.driver.types.Entity;
import org.structr.api.NotInTransactionException;
import org.structr.api.graph.Identity;
import org.structr.api.graph.PropertyContainer;

import java.lang.reflect.Array;
import java.util.*;
import java.util.Map.Entry;


abstract class EntityWrapper<T extends Entity> implements PropertyContainer {

	protected final BoltDatabaseService db;
	protected final long id;
	protected T entity;

	protected boolean deleted = false;

	public EntityWrapper(final BoltDatabaseService db, final T entity) {

		this.entity = entity;
		this.id     = entity.id();
		this.db     = db;
	}

	protected abstract String getQueryPrefix();
	protected abstract boolean isNode();

	@Override
	public int hashCode() {
		return getId().hashCode();
	}

	@Override
	public boolean equals(final Object other) {
		return other.hashCode() == this.hashCode();
	}

	@Override
	public Identity getId() {
		return new BoltIdentity(id);
	}

	public long getDatabaseId() {
		return id;
	}

	@Override
	public boolean hasProperty(final String name) {
		return entity.containsKey(name);
	}

	@Override
	public Object getProperty(final String name) {

		final Object value = entity.get(name).asObject();
		if (value instanceof List) {

			try {

				final List list = (List)value;
				if (!list.isEmpty()) {

					final Object firstElement = ((List)value).get(0);
					final Object[] arr        = (Object[])Array.newInstance(firstElement.getClass(), 0);

					// convert list to array
					return ((List)value).toArray(arr);
				}

				// empty array => return null?
				return null;

			} catch (Throwable t) {
				t.printStackTrace();
			}
		}

		return value;
	}

	@Override
	public Object getProperty(String name, Object defaultValue) {

		final Object value = getProperty(name);
		if (value == null) {

			return defaultValue;
		}

		return value;
	}

	@Override
	public void setProperty(final String key, final Object value) {

		final SessionTransaction tx = db.getCurrentTransaction();

		// only update values if actually different from what is stored
		if (needsUpdate(key, value)) {

			final Map<String, Object> map = new HashMap<>();
			final String query            = getQueryPrefix() + " WHERE ID(n) = $id SET n.`" + key + "` = $value RETURN n";

			map.put("id", id);
			map.put("value", value);

			updateEntity(tx, query, map);
		}
	}

	@Override
	public void setProperties(final Map<String, Object> values) {

		// remove properties that are already present
		filter(values);

		// only update values if actually different from what is stored
		if (!values.isEmpty()) {

			final Map<String, Object> map = new HashMap<>();
			final SessionTransaction tx   = db.getCurrentTransaction();
			final String query            = getQueryPrefix() + " WHERE ID(n) = $id SET n += $properties RETURN n";

			// overwrite a potential "id" property
			map.put("id", id);
			map.put("properties", values);

			updateEntity(tx, query, map);
		}
	}

	@Override
	public void removeProperty(String key) {

		final SessionTransaction tx   = db.getCurrentTransaction();
		final Map<String, Object> map = new HashMap<>();
		final String query            = getQueryPrefix() + " WHERE ID(n) = $id SET n.`" + key + "` = Null RETURN n";

		map.put("id", id);

		updateEntity(tx, query, map);
	}

	@Override
	public Iterable<String> getPropertyKeys() {
		return entity.keys();
	}

	@Override
	public void delete(final boolean deleteRelationships) throws NotInTransactionException {

		final SessionTransaction tx   = db.getCurrentTransaction();
		final Map<String, Object> map = new HashMap<>();
		final StringBuilder buf       = new StringBuilder();

		map.put("id", id);

		buf.append(getQueryPrefix());
		buf.append(" WHERE ID(n) = $id");

		if (deleteRelationships && isNode()) {

			buf.append(" DETACH");
		}

		buf.append(" DELETE n");

		tx.set(buf.toString(), map);

		deleted = true;
	}

	@Override
	public boolean isDeleted() {
		return deleted;
	}

	// ----- protected methods -----
	protected void updateEntity(final T entity) {
		this.entity = entity;
	}

	// ----- private methods -----
	private void updateEntity(final SessionTransaction tx, final String query, final Map<String, Object> map) {

		// execute query
		if (isNode()) {

			this.updateEntity((T)tx.getNode(new SimpleCypherQuery(query, map)));

		} else {

			updateEntity((T)tx.getRelationship(new SimpleCypherQuery(query, map)));
		}
	}

	private void filter(final Map<String, Object> data) {

		final Iterator<Entry<String, Object>> it = data.entrySet().iterator();

		while (it.hasNext()) {

			final Entry<String, Object> entry = it.next();

			// remove all keys that are already present
			if (!needsUpdate(entry.getKey(), entry.getValue())) {

				it.remove();
			}
		}
	}

	private boolean needsUpdate(final String key, final Object newValue) {

		final Object existingValue = getProperty(key);

		if (existingValue == null && newValue == null) {
			return false;
		}

		if (existingValue == null && newValue != null) {
			return true;
		}

		if (existingValue != null && newValue == null) {
			return true;
		}

		if (existingValue != null && newValue != null) {
			return !equal(existingValue, newValue);
		}

		return false;
	}

	private boolean equal(final Object existingValue, final Object newValue) {

		if (existingValue instanceof List) {

			final List list1 = (List)existingValue;
			final List list2 = Arrays.asList((Object[])newValue);

			return list1.equals(list2);
		}

		// special handling for the case that Neo4j always returns long values for both int and long
		if ((existingValue instanceof Long && newValue instanceof Integer) || (existingValue instanceof Integer && newValue instanceof Long)) {

			final long v1 = ((Number)existingValue).longValue();
			final long v2 = ((Number)newValue).longValue();

			return v1 == v2;
		}

		return existingValue.equals(newValue);
	}
}
