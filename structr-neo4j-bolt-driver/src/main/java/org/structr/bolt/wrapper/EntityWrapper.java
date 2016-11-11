/**
 * Copyright (C) 2010-2016 Structr GmbH
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import org.neo4j.driver.v1.exceptions.NoSuchRecordException;
import org.neo4j.driver.v1.types.Entity;
import org.structr.api.NotFoundException;
import org.structr.api.NotInTransactionException;
import org.structr.api.graph.PropertyContainer;
import org.structr.api.util.Cachable;
import org.structr.bolt.BoltDatabaseService;
import org.structr.bolt.SessionTransaction;

/**
 *
 */
public abstract class EntityWrapper<T extends Entity> implements PropertyContainer, Cachable {

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

			// convert list to array
			return ((List)value).toArray(new String[0]);
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

		final SessionTransaction tx   = db.getCurrentTransaction();
		final Map<String, Object> map = new HashMap<>();
		final String query            = getQueryPrefix() + " WHERE ID(n) = {id} SET n.`" + key + "` = {value}";

		map.put("id", id);
		map.put("value", value);

		// update entity handle
		tx.set(query, map);

		// update data
		update(key, value);

		tx.modified(this);
	}

	@Override
	public void setProperties(final Map<String, Object> values) {

		assertNotStale();

		final SessionTransaction tx   = db.getCurrentTransaction();
		final Map<String, Object> map = new HashMap<>(values);
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
	public void delete() throws NotInTransactionException {

		assertNotStale();

		final SessionTransaction tx   = db.getCurrentTransaction();
		final Map<String, Object> map = new HashMap<>();

		map.put("id", id);

		tx.set(getQueryPrefix() + " WHERE ID(n) = {id} DELETE n", map);
		tx.modified(this);

		invalidate();

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
			invalidate();

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
}
