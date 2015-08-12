/**
 * Copyright (C) 2010-2015 Morgner UG (haftungsbeschränkt)
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
package org.structr.core.property;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.ReadOnlyPropertyToken;
import org.structr.core.GraphObject;
import org.structr.core.converter.PropertyConverter;

/**
 * Abstract base class for read-only properties.
 *
 * @author Christian Morgner
 */
public abstract class AbstractReadOnlyProperty<T> extends Property<T> {

	public AbstractReadOnlyProperty(String name) {
		this(name, name);
	}

	public AbstractReadOnlyProperty(String name, T defaultValue) {
		this(name, name, defaultValue);
	}

	public AbstractReadOnlyProperty(String jsonName, String dbName) {
		this(jsonName, dbName, null);
	}

	public AbstractReadOnlyProperty(String jsonName, String dbName, T defaultValue) {
		super(jsonName, dbName, defaultValue);
	}

	@Override
	public Property<T> indexed() {

		// related node properties are always passively indexed
		// (because they can change without setProperty())
		super.passivelyIndexed();
		return this;
	}

	@Override
	public String typeName() {
		return ""; // read-only
	}

	@Override
	public Object fixDatabaseProperty(Object value) {
		return value;
	}

	@Override
	public Object setProperty(SecurityContext securityContext, GraphObject obj, T value) throws FrameworkException {
		throw new FrameworkException(obj.getClass().getSimpleName(), new ReadOnlyPropertyToken(this));
	}

	@Override
	public PropertyConverter<T, ?> databaseConverter(SecurityContext securityContext) {
		return null;
	}

	@Override
	public PropertyConverter<T, ?> databaseConverter(SecurityContext securityContext, GraphObject entity) {
		return null;
	}

	@Override
	public PropertyConverter<?, T> inputConverter(SecurityContext securityContext) {
		return null;
	}

	@Override
	public Object getValueForEmptyFields() {
		return null;
	}
}
