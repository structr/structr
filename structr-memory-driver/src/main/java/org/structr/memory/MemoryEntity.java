/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.memory;

import org.structr.api.graph.Identity;
import org.structr.api.graph.PropertyContainer;
import org.structr.api.util.ChangeAwareMap;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

/**
 */
public abstract class MemoryEntity implements PropertyContainer {

	private final Map<String, Object> dataCache    = new HashMap<>();
	private final Map<Long, ChangeAwareMap> txData = new HashMap<>();
	private final ChangeAwareMap data              = new ChangeAwareMap();
	private final Set<String> labels               = new LinkedHashSet<>();
	private ReentrantLock lock                     = new ReentrantLock();
	protected MemoryDatabaseService db             = null;
	private MemoryIdentity id                      = null;

	protected MemoryEntity(final MemoryDatabaseService db) {
		this.db = db;
	}

	public MemoryEntity(final MemoryDatabaseService db, final MemoryIdentity identity) {

		this.id = identity;
		this.db = db;

		lock();
	}

	protected abstract void updateCache();

	@Override
	public Identity getId() {
		return id;
	}

	public MemoryIdentity getIdentity() {
		return id;
	}

	@Override
	public boolean hasProperty(final String name) {
		return getData(true).containsKey(name);
	}

	@Override
	public Object getProperty(String name) {
		return getData(true).get(name);
	}

	@Override
	public Object getProperty(String name, Object defaultValue) {

		final Object value = getProperty(name);
		if (value != null) {

			return value;
		}

		return defaultValue;
	}

	@Override
	public void setProperty(final String name, final Object value) {
		lock();
		getData(false).put(name, value);
	}

	@Override
	public void setProperties(final Map<String, Object> values) {
		lock();
		getData(false).putAll(values);
	}

	@Override
	public void removeProperty(final String name) {
		lock();
		getData(false).put(name, null);
	}

	@Override
	public Iterable<String> getPropertyKeys() {
		return getData(true).keySet();
	}

	@Override
	public boolean isDeleted() {
		return false;
	}

	public Map<String, Object> getCache() {
		return dataCache;
	}

	public void addLabels(final Set<String> labels) {
		addLabels(labels, true);
	}

	void addLabels(final Set<String> labels, final boolean updateCache) {

		lock();
		this.labels.addAll(labels);

		if (updateCache) {
			updateCache();
		}
	}

	public void removeLabel(final String label) {
		removeLabel(label, true);
	}

	public void removeLabel(final String label, final boolean updateCache) {

		lock();
		labels.remove(label);

		if (updateCache) {
			updateCache();
		}
	}

	public boolean hasLabel(final String label) {
		return labels.contains(label);
	}

	public Iterable<String> getLabels() {
		return labels;
	}

	// ----- package-private methods -----
	void commit(final long transactionId) {

		final ChangeAwareMap changes = txData.get(transactionId);
		if (changes != null) {

			for (final String key : changes.getModifiedKeys()) {

				final Object value = changes.get(key);
				if (value != null) {

					data.put(key, value);

				} else {

					data.remove(key);
				}
			}

			txData.remove(transactionId);
		}

		unlock();
	}

	void rollback(final long transactionId) {
		txData.remove(transactionId);
		unlock();
	}

	MemoryEntity lock() {

		if (!lock.isHeldByCurrentThread()) {
			lock.lock();
		}

		return this;
	}

	MemoryEntity unlock() {

		if (lock.isHeldByCurrentThread()) {
			lock.unlock();
		}

		return this;
	}

	// ----- package-private methods -----
	void loadFromStorage(final ObjectInputStream in) throws IOException, ClassNotFoundException {

		// read identity first
		id = MemoryIdentity.loadFromStorage(in);

		// read label count
		final int labelCount = in.readInt();

		// read labels
		for (int i=0; i<labelCount; i++) {

			labels.add(in.readUTF());
		}

		// read properties
		final int propertyCount = in.readInt();

		for (int i=0; i<propertyCount; i++) {

			final String key   = in.readUTF();
			final Object value = in.readObject();

			if (key != null && value != null) {

				data.put(key, value);
			}
		}
	}

	void writeToStorage(final ObjectOutputStream out) throws IOException {

		// write identity first
		id.writeToStorage(out);

		// then label count
		out.writeInt(labels.size());

		// and labels
		for (final String label : labels) {

			out.writeUTF(label);
		}

		// then properties
		out.writeInt(data.size());

		for (final Entry<String, Object> entry : data.entrySet()) {

			out.writeUTF(entry.getKey());
			out.writeObject(entry.getValue());
		}
	}


	// ----- private methods -----
	private ChangeAwareMap getData(final boolean read) {

		// read-only access does not need a transaction
		final MemoryTransaction tx = db.getCurrentTransaction(!read);
		if (tx != null) {

			final long transactionId = tx.getTransactionId();
			ChangeAwareMap copy      = txData.get(transactionId);
			if (copy == null) {

				copy = new ChangeAwareMap(data);
				txData.put(transactionId, copy);

				tx.modify(this);
			}

			return copy;

		} else {

			return data;
		}
	}

}
