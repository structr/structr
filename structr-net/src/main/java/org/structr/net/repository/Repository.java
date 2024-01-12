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

import java.util.Collection;
import java.util.Map;

/**
 */
public interface Repository {

	/**
	 * Returns the UUID of this repository which will be used to determine
	 * the "owner" of an object in the shared database.
	 *
	 * @return the string-based UUID of this repository
	 */
	String getUuid();

	void addInternalChangeListener(final InternalChangeListener listener);
	void removeInternalChangeListener(final InternalChangeListener listener);

	void addExternalChangeListener(final ExternalChangeListener listener);
	void removeExternalChangeListener(final ExternalChangeListener listener);

	// Methods that are called when a shared object is modified externally
	RepositoryObject objectCreated(final String id, final String type, final String deviceId, final String userId, final PseudoTime created, final PseudoTime lastModified, final Map<String, Object> data);
	void objectDeleted(final String id, final PseudoTime timestamp);

	// Methods that allow the local repository contents to be modified
	RepositoryObject create(final String id, final String type, final String deviceId, final String userId, final PseudoTime created, final Map<String, Object> data);
	void update(final RepositoryObject obj, final String type, final String deviceId, final String userId, final PseudoTime lastModified, final Map<String, Object> data);
	void delete(final String id, final PseudoTime deleted);

	String beginTransaction(final long timeout);
	void complete(final String transactionId);

	/**
	 * Indicated whether this repository contains and object with the given
	 * UUID.
	 *
	 * @param id the UUID of an object
	 * @return whether this repository contains the given ID
	 */
	boolean contains(final String id);

	/**
	 * Returns the object with the given UUID, or null.
	 *
	 * @param id
	 * @return the RepositoryObject with the given ID or null
	 */
	RepositoryObject getObject(final String id);

	/**
	 * Returns a collection of all the objects in this repository.
	 *
	 * @return a collection of all the objects in this repository
	 */
	Collection<RepositoryObject> getObjects();


	int objectCount();
}
