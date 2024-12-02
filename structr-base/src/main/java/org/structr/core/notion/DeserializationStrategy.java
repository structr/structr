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
package org.structr.core.notion;

import org.structr.api.util.Iterables;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.property.RelationProperty;
import org.structr.core.traits.GraphObject;
import org.structr.core.traits.NodeInterface;
import org.structr.core.traits.Trait;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Defines a strategy for deserializing a {@link org.structr.core.GraphObject} from an input
 * object.
 *
 *
 * @param <T>
 */
public abstract class DeserializationStrategy<S, T extends NodeInterface> {

	public abstract T deserialize(final SecurityContext securityContext, Trait<T> type, S source, final Object context) throws FrameworkException;

	public abstract void setRelationProperty(final RelationProperty<S> parentProperty);

	protected void setProperties(final SecurityContext securityContext, final GraphObject obj, final PropertyMap properties) throws FrameworkException {

		// are we allowed to set properties on related nodes?
		final Boolean allowed = (Boolean)securityContext.getAttribute("setNestedProperties");
		if (allowed != null && allowed == true) {

			if (securityContext.forceMergeOfNestedProperties()) {

				final PropertyMap mergedProperties = new PropertyMap();

				for (final Entry<PropertyKey, Object> entry : properties.entrySet()) {

					final PropertyKey key = entry.getKey();
					final Object newValue  = entry.getValue();
					final Object oldValue = obj.getProperty(key);

					if (newValue != null && !newValue.equals(oldValue)) {

						mergedProperties.put(key, merge(oldValue, newValue));
					}
				}

				obj.setProperties(securityContext, mergedProperties);

			} else {

				obj.setProperties(securityContext, properties);
			}
		}
	}

	protected Object merge(final Object oldValue, final Object newValue) {

		if (oldValue instanceof Iterable && newValue instanceof Iterable) {

			final Iterable oldCollection = (Iterable)oldValue;
			final Iterable newCollection = (Iterable)newValue;
			final Set merged               = new LinkedHashSet<>();

			Iterables.addAll(merged, newCollection);
			Iterables.addAll(merged, oldCollection);

			return new LinkedList<>(merged);
		};

		return newValue;
	}

}
