/**
 * Copyright (C) 2010-2018 Structr GmbH
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
package org.structr.bolt.wrapper;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import org.neo4j.driver.v1.exceptions.NoSuchRecordException;
import org.neo4j.driver.v1.types.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.NotFoundException;
import org.structr.api.NotInTransactionException;
import org.structr.api.graph.PropertyContainer;
import org.structr.api.util.Cachable;
import org.structr.bolt.BoltDatabaseService;
import org.structr.bolt.SessionTransaction;


public abstract class EntityWrapper<T extends Entity> implements PropertyContainer, Cachable {

	private static final Logger logger = LoggerFactory.getLogger(EntityWrapper.class.getName());

	protected final Map<String, Object> data = new ConcurrentHashMap<>();
	protected BoltDatabaseService db         = null;
	protected boolean stale                  = false;
	protected long id                        = -1L;

	public EntityWrapper(final BoltDatabaseService db, final T entity) {

		this.data.putAll(entity.asMap());
		this.id   = entity.id();
		this.db   = db;
	}

	protected abstract String getQueryPrefix();
	public abstract void clearCaches();
	public abstract void onClose();

	@Override
	public String toString() {
		return (this instanceof NodeWrapper ? "N" : "R") + getId();
	}

	@Override
	public long getId() {
		return id;
	}

	@Override
	public boolean hasProperty(final String name) {

		assertNotStale();

		return data.containsKey(name);
	}

	@Override
	public Object getProperty(final String name) {

		assertNotStale();

		final Object value = data.get(name);
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
				logger.warn("", t);
			}
		}

		return value;
	}

	@Override
	public Object getProperty(String name, Object defaultValue) {

		assertNotStale();

		Object value = getProperty(name);
		if (value == null) {

			return defaultValue;
		}

		return value;
	}

	@Override
	public void setProperty(final String key, final Object value) {

		assertNotStale();

		final SessionTransaction tx = db.getCurrentTransaction();

		// only update values if actually different from what is stored
		if (needsUpdate(key, value)) {

			final Map<String, Object> map = new HashMap<>();
			final String query            = getQueryPrefix() + " WHERE ID(n) = {id} SET n.`" + key + "` = {value}";

			map.put("id", id);
			map.put("value", value);

			// update entity handle
			tx.set(query, map);

			// update data
			update(key, value);
		}

		// mark node as modified
		tx.modified(this);
	}

	@Override
	public void setProperties(final Map<String, Object> values) {

		assertNotStale();

		final Map<String, Object> map = new HashMap<>();
		final SessionTransaction tx   = db.getCurrentTransaction();
		final String query            = getQueryPrefix() + " WHERE ID(n) = {id} SET n += {properties}";

		// overwrite a potential "id" property
		map.put("id", id);
		map.put("properties", values);

		// execute query
		tx.set(query, map);

		// update data
		update(values);

		tx.modified(this);
	}

	@Override
	public void removeProperty(String key) {

		assertNotStale();

		final SessionTransaction tx   = db.getCurrentTransaction();
		final Map<String, Object> map = new HashMap<>();
		final String query            = getQueryPrefix() + " WHERE ID(n) = {id} SET n.`" + key + "` = Null";

		map.put("id", id);

		// execute query
		tx.set(query, map);

		// remove key from data
		data.remove(key);

		tx.modified(this);
	}

	@Override
	public Iterable<String> getPropertyKeys() {

		assertNotStale();

		return data.keySet();
	}

	@Override
	public void delete(final boolean deleteRelationships) throws NotInTransactionException {

		assertNotStale();

		final SessionTransaction tx   = db.getCurrentTransaction();
		final Map<String, Object> map = new HashMap<>();
		final StringBuilder buf       = new StringBuilder();

		map.put("id", id);

		buf.append(getQueryPrefix());
		buf.append(" WHERE ID(n) = {id}");

		if (deleteRelationships) {

			buf.append(" DETACH");
		}

		buf.append(" DELETE n");

		tx.set(buf.toString(), map);
		tx.modified(this);

		stale = true;
	}

	@Override
	public boolean isSpatialEntity() {
		return false;
	}

	public boolean isStale() {
		return this.stale;
	}

	public void stale() {
		this.stale = true;
	}

	// ----- protected methods -----
	protected synchronized void assertNotStale() {

		if (stale) {

			// invalidate caches
			onRemoveFromCache();

			// if a node/rel was deleted in a previous transaction but the caller keeps a
			// reference to this entity, we need to make sure that the reference is fresh.

			final SessionTransaction tx   = db.getCurrentTransaction();
			final Map<String, Object> map = new HashMap<>();

			map.put("id", id);

			try {

				// update data
				data.clear();
				update(tx.getEntity(getQueryPrefix() + " WHERE ID(n) = {id} RETURN n", map).asMap());

			} catch (NoSuchRecordException nex) {
				throw new NotFoundException(nex);
			}

			stale  = false;
		}
	}

	// ----- private methods -----
	private void update(final Map<String, Object> values) {

		for (final Entry<String, Object> entry : values.entrySet()) {

			update(entry.getKey(), entry.getValue());
		}
	}

	private void update(final String key, final Object value) {

		if (value != null) {

			data.put(key, value);

		} else {

			data.remove(key);
		}
	}

	private boolean needsUpdate(final String key, final Object newValue) {

		final Object existingValue = data.get(key);

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

		return existingValue.equals(newValue);
	}
}
