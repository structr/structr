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
package org.structr.api.graph;

import org.structr.api.NotInTransactionException;

import java.util.Map;

/**
 *
 */
public interface PropertyContainer {

	Identity getId();

	default void invalidate() {}

	boolean hasProperty(final String name);
	Object getProperty(final String name);
	Object getProperty(final String name, final Object defaultValue);
	void setProperty(final String name, final Object value);
	void setProperties(final Map<String, Object> values);
	void removeProperty(final String name);

	Iterable<String> getPropertyKeys();

	void delete(final boolean deleteRelationships) throws NotInTransactionException;

	boolean isDeleted();
	boolean isNode();

	default int compare(final String key, final PropertyContainer a, final PropertyContainer b) {

		if (!a.hasProperty(key) && b.hasProperty(key)) {
			return -1;
		}

		if (a.hasProperty(key) && !b.hasProperty(key)) {
			return 1;
		}

		if (a.hasProperty(key) && b.hasProperty(key)) {

			final String t1 = (String)a.getProperty(key);
			final String t2 = (String)b.getProperty(key);

			int result =  t1.compareTo(t2);

			if (result != 0) {
				return result;
			}
		}

		// do not return 0 since that would cause objects without the
		// above property to be considered equal which is not wanted.
		return a.getId().compareTo(b.getId());
	}
}
