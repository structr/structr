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
package org.structr.sql;

import java.util.Map;
import org.structr.api.NotInTransactionException;
import org.structr.api.graph.Identity;
import org.structr.api.graph.PropertyContainer;

/**
 */
public abstract class SQLEntity implements PropertyContainer {

	protected Map<String, Object> data = null;
	protected SQLDatabaseService db    = null;
	protected SQLIdentity id           = null;
	protected boolean stale            = false;

	SQLEntity(final SQLDatabaseService db, final PropertySetResult result) {

		this.db   = db;
		this.id   = result.id();
		this.data = result.data();
	}

	SQLEntity(final SQLIdentity id) {
		this.id = id;
	}

	SQLIdentity getIdentity() {
		return id;
	}

	@Override
	public Identity getId() {
		return id;
	}

	@Override
	public boolean hasProperty(final String name) {
		return data.containsKey(name);
	}

	@Override
	public Object getProperty(final String name) {
		return data.get(name);
	}

	@Override
	public Object getProperty(final String name, final Object defaultValue) {

		if (data.containsKey(name)) {

			return data.get(name);
		}

		return defaultValue;
	}

	@Override
	public void setProperty(final String name, final Object value) {
	}

	@Override
	public void setProperties(final Map<String, Object> values) {
	}

	@Override
	public void removeProperty(final String name) {
	}

	@Override
	public Iterable<String> getPropertyKeys() {
		return data.keySet();
	}

	@Override
	public void delete(final boolean deleteRelationships) throws NotInTransactionException {
	}
}
