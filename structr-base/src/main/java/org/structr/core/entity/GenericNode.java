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
package org.structr.core.entity;

import org.structr.api.util.Iterables;
import org.structr.common.error.ErrorBuffer;
import org.structr.core.property.GenericProperty;
import org.structr.core.property.PropertyKey;
import org.structr.schema.NonIndexed;

import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

/**
 * A generic node entity that will be instantiated when a node with an unknown
 * type is encountered.
 */
public class GenericNode extends AbstractNode implements NonIndexed {

	@Override
	public int hashCode() {
		final String uuid = getUuid();
		if (uuid != null) {
			return uuid.hashCode();
		} else {
			return dbNode.getId().hashCode();
		}
	}

	@Override
	public boolean equals(final Object o) {

		if (o instanceof GenericNode) {
			return o.hashCode() == hashCode();
		}

		return false;
	}

	@Override
	public boolean isValid(ErrorBuffer errorBuffer) {
		return true;
	}

	@Override
	public Set<PropertyKey> getPropertyKeys(final String propertyView) {

		final Set<PropertyKey> keys = new TreeSet<>(new PropertyKeyComparator());

		// add all properties from a schema entity (if existing)
		keys.addAll(Iterables.toList(super.getPropertyKeys(propertyView)));

		// add properties that are physically present on the node
		keys.addAll(Iterables.toList(Iterables.map(new GenericPropertyKeyMapper(), dbNode.getPropertyKeys())));

		return keys;
	}

	@Override
	protected boolean isGenericNode() {
		return true;
	}

	// ----- nested classes -----
	private class GenericPropertyKeyMapper implements Function<String, PropertyKey> {

		@Override
		public PropertyKey apply(final String from) throws RuntimeException {
			return new GenericProperty(from);
		}
	}

	private class PropertyKeyComparator implements Comparator<PropertyKey> {

		@Override
		public int compare(final PropertyKey o1, final PropertyKey o2) {

			if (o1 != null && o2 != null) {

				return o1.jsonName().compareTo(o2.jsonName());
			}

			throw new NullPointerException();
		}
	}
}
