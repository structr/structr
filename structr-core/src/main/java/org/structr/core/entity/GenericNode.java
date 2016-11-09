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
package org.structr.core.entity;

import java.util.Collections;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import org.structr.api.graph.Node;
import org.structr.api.util.FixedSizeCache;
import org.structr.api.util.Iterables;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.property.GenericProperty;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.schema.NonIndexed;

//~--- classes ----------------------------------------------------------------

/**
 * A generic node entity that will be instantiated when a node with an unknown
 * type is encountered.
 *
 *
 */
public class GenericNode extends AbstractNode implements NonIndexed {

	private static final FixedSizeCache<Long, Set<PropertyKey>> propertyKeys = new FixedSizeCache<>(1000);

	@Override
	public int hashCode() {
		return getNodeId().hashCode();
	}

	@Override
	public boolean equals(final Object o) {

		if (o instanceof GenericNode) {
			return o.hashCode() == hashCode();
		}

		return false;
	}

	@Override
	public boolean onCreation(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {
		return true;
	}

	@Override
	public boolean onModification(SecurityContext securityContext, ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {
		return true;
	}

	@Override
	public boolean onDeletion(SecurityContext securityContext, ErrorBuffer errorBuffer, PropertyMap properties) throws FrameworkException {
		return true;
	}

	@Override
	public boolean isValid(ErrorBuffer errorBuffer) {
		return true;
	}

	@Override
	public Iterable<PropertyKey> getPropertyKeys(final String propertyView) {

		final Node node = getNode();
		if (node != null) {

			final long id = node.getId();
			Set<PropertyKey> keys = propertyKeys.get(id);
			if (keys == null) {

				keys = new TreeSet<>(new PropertyKeyComparator());

				// add all properties from a schema entity (if existing)
				keys.addAll(Iterables.toList(super.getPropertyKeys(propertyView)));

				// add properties that are physically present on the node
				keys.addAll(Iterables.toList(Iterables.map(new GenericPropertyKeyMapper(), dbNode.getPropertyKeys())));

				propertyKeys.put(id, keys);
			}

			return keys;
		}

		// return the whole set
		return Collections.EMPTY_SET;
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

	private class GenericRelation extends ManyToMany {

		private String relType = null;

		public GenericRelation(final String relType) {
			this.relType = relType;
		}

		@Override
		public Class getSourceType() {
			return GenericNode.class;
		}

		@Override
		public Class getTargetType() {
			return GenericNode.class;
		}

		@Override
		public String name() {
			return relType;
		}

		@Override
		public boolean isInternal() {
			return false;
		}
	}
}
