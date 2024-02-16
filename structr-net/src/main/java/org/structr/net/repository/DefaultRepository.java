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
package org.structr.net.repository;

import org.structr.net.data.time.PseudoTime;
import org.structr.net.peer.Peer;

import java.util.*;
import java.util.Map.Entry;

/**
 *
 */
public class DefaultRepository implements Repository {

	private final List<InternalChangeListener> internalChangeListeners = new ArrayList<>();
	private final List<ExternalChangeListener> externalChangeListeners = new ArrayList<>();
	private final Map<String, DefaultPossibility> possibilities        = new LinkedHashMap<>();
	private final Map<String, RepositoryObject> objects                = new LinkedHashMap<>();
	private Peer peer                                                  = null;
	private String uuid                                                = null;

	public DefaultRepository(final String uuid) {
		this.uuid = uuid;
	}

	public void setPeer(final Peer peer) {
		this.peer = peer;
	}

	@Override
	public String getUuid() {
		return uuid;
	}

	@Override
	public void addInternalChangeListener(final InternalChangeListener listener) {
		internalChangeListeners.add(listener);
	}

	@Override
	public void removeInternalChangeListener(final InternalChangeListener listener) {
		internalChangeListeners.remove(listener);
	}

	@Override
	public void addExternalChangeListener(final ExternalChangeListener listener) {
		externalChangeListeners.add(listener);
		listener.onAdd();
	}

	@Override
	public void removeExternalChangeListener(final ExternalChangeListener listener) {
		externalChangeListeners.remove(listener);
		listener.onRemove();
	}

	public RepositoryObject add(final String id, final String type, final String deviceId, final String userId, final PseudoTime creationTime) {

		synchronized (objects) {

			final RepositoryObject obj = new DefaultRepositoryObject(this, id, type, deviceId, userId, creationTime);

			objects.put(id, obj);

			return obj;
		}
	}

	@Override
	public RepositoryObject create(final String id, final String type, final String deviceId, final String userId, final PseudoTime created, final Map<String, Object> data) {

		RepositoryObject obj = getObject(id);
		if (obj == null) {

			obj = new DefaultRepositoryObject(this, id, type, deviceId, userId, created);

			synchronized (objects) {

				final String transactionId = UUID.randomUUID().toString().replaceAll("\\-", "");
				for (final Entry<String, Object> entry : data.entrySet()) {

					obj.setProperty(created, transactionId, entry.getKey(), entry.getValue());
				}

				complete(transactionId);
				objects.put(id, obj);
			}

			for (final InternalChangeListener listener : internalChangeListeners) {
				listener.onObjectCreation(obj, data);
			}

			notifyCreation(obj, data);
		}

		return obj;
	}

	@Override
	public void update(final RepositoryObject obj, final String type, final String deviceId, final String userId, final PseudoTime lastModified, final Map<String, Object> data) {

		final String transactionId = UUID.randomUUID().toString().replaceAll("\\-", "");
		for (final Entry<String, Object> entry : data.entrySet()) {

			obj.setProperty(lastModified, transactionId, entry.getKey(), entry.getValue());
		}

		complete(transactionId);

		notifyModification(obj, data);
	}

	@Override
	public void delete(final String id, final PseudoTime timestamp) {

		synchronized (objects) {

			final RepositoryObject obj = objects.get(id);
			if (obj != null) {

				objects.remove(id);
				notifyDeletion(obj);
			}

			for (final InternalChangeListener listener : internalChangeListeners) {
				listener.onObjectDeletion(id);
			}
		}
	}

	@Override
	public RepositoryObject objectCreated(final String id, final String type, final String deviceId, final String userId, final PseudoTime created, final PseudoTime lastModified, final Map<String, Object> data) {

		synchronized (objects) {

			RepositoryObject obj = getObject(id);

			if (obj == null) {

				obj = new DefaultRepositoryObject(this, id, type, deviceId, userId, created);
				final String transactionId = UUID.randomUUID().toString().replaceAll("\\-", "");

				if (data != null) {

					for (final Entry<String, Object> entry : data.entrySet()) {

						obj.setProperty(lastModified, transactionId, entry.getKey(), entry.getValue());
					}
				}

				objects.put(id, obj);

				complete(transactionId);

				notifyCreation(obj, data);
			}

			return obj;
		}
	}

	@Override
	public void objectDeleted(final String id, final PseudoTime timestamp) {

		synchronized (objects) {

			final RepositoryObject obj = objects.get(id);
			if (obj != null) {

				objects.remove(id);
				notifyDeletion(obj);
			}
		}
	}

	@Override
	public boolean contains(final String id) {

		notifyRepositoryQuery();

		synchronized (objects) {
			return objects.containsKey(id);
		}
	}

	@Override
	public RepositoryObject getObject(final String id) {

		notifyRepositoryQuery();

		synchronized (objects) {
			return objects.get(id);
		}
	}

	@Override
	public Collection<RepositoryObject> getObjects() {

		notifyRepositoryQuery();

		final List<RepositoryObject> list = new ArrayList<>();
		synchronized (objects) {

			list.addAll(objects.values());
		}

		return list;
	}

	@Override
	public int objectCount() {
		return objects.size();
	}

	@Override
	public String beginTransaction(final long timeout) {

		final DefaultPossibility p = new DefaultPossibility(peer, timeout);

		synchronized (possibilities) {
			possibilities.put(p.getUuid(), p);
		}

		return p.getUuid();
	}

	@Override
	public void complete(final String transactionId) {

		final DefaultPossibility p = getPossibility(transactionId);
		if (p != null) {

			if (!p.isAborted()) {

				p.complete();
			}
		}
	}

	public DefaultPossibility getPossibility(final String transactionId) {

		synchronized (possibilities) {

			DefaultPossibility p = possibilities.get(transactionId);
			if (p == null) {

				p = new DefaultPossibility(peer, transactionId);
				possibilities.put(transactionId, p);

			}

			return p;
		}
	}

	public void clear() {
		objects.clear();
	}

	// ----- private methods -----
	private void notifyCreation(final RepositoryObject object, final Map<String, Object> data) {

		for (final ExternalChangeListener listener : externalChangeListeners) {
			listener.onCreate(object, data);
		}
	}

	private void notifyDeletion(final RepositoryObject object) {

		for (final ExternalChangeListener listener : externalChangeListeners) {
			listener.onDelete(object);
		}
	}

	private void notifyModification(final RepositoryObject object, final Map<String, Object> data) {

		for (final ExternalChangeListener listener : externalChangeListeners) {
			listener.onModify(object, data);
		}
	}

	private void notifyRepositoryQuery() {

		for (final ExternalChangeListener listener : externalChangeListeners) {
			listener.onQuery();
		}
	}
}
