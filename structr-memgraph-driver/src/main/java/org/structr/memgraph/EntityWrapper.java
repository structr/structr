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
package org.structr.memgraph;

import org.neo4j.driver.v1.exceptions.NoSuchRecordException;
import org.neo4j.driver.v1.types.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.NotFoundException;
import org.structr.api.NotInTransactionException;
import org.structr.api.graph.Identity;
import org.structr.api.graph.PropertyContainer;
import org.structr.api.util.ChangeAwareMap;

import java.lang.reflect.Array;
import java.util.*;
import java.util.Map.Entry;


abstract class EntityWrapper<T extends Entity> implements PropertyContainer {

	private static final Logger logger = LoggerFactory.getLogger(EntityWrapper.class.getName());

	private final Map<Object, ChangeAwareMap> txData = Collections.synchronizedMap(new WeakHashMap<>());
	private final ChangeAwareMap entityData          = new ChangeAwareMap();
	protected MemgraphDatabaseService db                 = null;
	protected boolean deleted                        = false;
	protected boolean stale                          = false;
	protected long id                                = -1L;

	protected EntityWrapper() {
		// nop constructor for cache access
	}

	public EntityWrapper(final MemgraphDatabaseService db, final T entity) {

		this.entityData.putAll(entity.asMap());
		this.id   = entity.id();
		this.db   = db;
	}

	protected abstract String getQueryPrefix();
	protected abstract boolean isNode();
	public abstract void removeFromCache();
	public abstract void clearCaches();
	public abstract void onClose();

	@Override
	public Identity getId() {
		return new BoltIdentity(id);
	}

	public long getDatabaseId() {
		return id;
	}

	@Override
	public boolean hasProperty(final String name) {

		assertNotStale();

		return accessData(false).containsKey(name);
	}

	@Override
	public Object getProperty(final String name) {

		assertNotStale();

		final Object value = accessData(false).get(name);
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
			final String query            = getQueryPrefix() + " WHERE ID(n) = $id SET n.`" + key + "` = $value";

			map.put("id", id);
			map.put("value", value);

			// update entity handle
			tx.set(query, map);

			// update data
			accessData(true).put(key, value);

			// mark node as modified
			setModified();
		}
	}

	@Override
	public void setProperties(final Map<String, Object> values) {

		assertNotStale();

		// remove properties that are already present
		filter(values);

		// only update values if actually different from what is stored
		if (!values.isEmpty()) {

			final Map<String, Object> map = new HashMap<>();
			final SessionTransaction tx   = db.getCurrentTransaction();
			final String query            = getQueryPrefix() + " WHERE ID(n) = $id SET n += " + MemgraphDatabaseService.createParameterMapStringFromMapAndInsertIntoQueryParameters("properties", values, map);

			// overwrite a potential "id" property
			map.put("id", id);

			// execute query
			tx.set(query, map);

			// update data
			update(values);

			setModified();
		}
	}

	@Override
	public void removeProperty(String key) {

		assertNotStale();

		final SessionTransaction tx   = db.getCurrentTransaction();
		final Map<String, Object> map = new HashMap<>();
		final String query            = getQueryPrefix() + " WHERE ID(n) = $id SET n.`" + key + "` = Null";

		map.put("id", id);

		// execute query
		tx.set(query, map);

		// remove key from data
		accessData(true).put(key, null);

		setModified();
	}

	@Override
	public Iterable<String> getPropertyKeys() {

		assertNotStale();

		return accessData(false).keySet();
	}

	@Override
	public void delete(final boolean deleteRelationships) throws NotInTransactionException {

		assertNotStale();

		final SessionTransaction tx   = db.getCurrentTransaction();
		final Map<String, Object> map = new HashMap<>();
		final StringBuilder buf       = new StringBuilder();

		map.put("id", id);

		buf.append(getQueryPrefix());
		buf.append(" WHERE ID(n) = $id");

		if (deleteRelationships) {

			buf.append(" DETACH");
		}

		buf.append(" DELETE n");

		tx.set(buf.toString(), map);
		setModified();

		stale   = true;
		deleted = true;
	}

	@Override
	public boolean isDeleted() {
		return deleted;
	}

	public void stale() {
		this.stale = true;
	}

	public void setModified() {
		db.getCurrentTransaction().accessed(this);
		db.getCurrentTransaction().modified(this);
	}

	public void rollback(final Object transactionId) {

		synchronized (this) {

			txData.remove(transactionId);

			stale = false;
		}
	}

	public void commit(final Object transactionId) {

		synchronized (this) {

			final ChangeAwareMap changes = txData.get(transactionId);
			if (changes != null) {

				for (final String key : changes.getModifiedKeys()) {

					final Object value = changes.get(key);

					if (value != null) {

						entityData.put(key, value);

					} else {

						entityData.remove(key);
					}
				}

				txData.remove(transactionId);
			}

			stale = false;
		}
	}

	// ----- protected methods -----
	protected synchronized void assertNotStale() {

		if (stale) {

			// if a node/rel was deleted in a previous transaction but the caller keeps a
			// reference to this entity, we need to make sure that the reference is fresh.

			final SessionTransaction tx   = db.getCurrentTransaction();
			final Map<String, Object> map = new HashMap<>();

			map.put("id", id);

			try {

				// update data
				update(tx.getEntity(getQueryPrefix() + " WHERE ID(n) = $id RETURN n", map).asMap());

			} catch (NoSuchRecordException nex) {
				throw new NotFoundException(nex);
			}

			stale  = false;
		}
	}

	// ----- private methods -----
	private void update(final Map<String, Object> values) {
		accessData(true).putAll(values);
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

		final Object existingValue = accessData(false).get(key);

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

	// ----- private methods -----
	private ChangeAwareMap accessData(final boolean write) {

		// read-only access does not need a transaction
		final SessionTransaction tx = db.getCurrentTransaction(false);
		if (tx != null) {

			if (deleted || tx.isDeleted(this)) {
				throw new NotFoundException("Entity with ID " + id + " not found.");
			}

			final Object transactionId = tx.getTransactionKey();
			ChangeAwareMap copy      = txData.get(transactionId);

			if (copy == null) {

				copy = new ChangeAwareMap(entityData);
				txData.put(transactionId, copy);

				if (write) {
					tx.accessed(this);
				}
			}

			return copy;

		} else {

			return entityData;
		}
	}
}
