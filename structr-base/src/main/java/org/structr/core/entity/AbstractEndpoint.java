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

import org.structr.api.Predicate;
import org.structr.api.graph.Direction;
import org.structr.api.graph.Node;
import org.structr.api.graph.Relationship;
import org.structr.api.graph.RelationshipType;
import org.structr.api.util.Iterables;
import org.structr.common.EntityAndPropertiesContainer;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.GraphTrait;
import org.structr.core.traits.Trait;
import org.structr.core.traits.Traits;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 *
 */
public abstract class AbstractEndpoint {

	public Relationship getSingle(final SecurityContext securityContext, final Node dbNode, final RelationshipType relationshipType, final Direction direction, final Trait<?> trait) {

		final Iterable<Relationship> rels     = getMultiple(securityContext, dbNode, relationshipType, direction, trait, null);
		final Iterator<Relationship> iterator = rels.iterator();

		// FIXME: this returns only the first relationship that matches, i.e. there is NO check for multiple relationships
		if (iterator.hasNext()) {
			return iterator.next();
		}

		return null;
	}

	public Iterable<Relationship> getMultiple(final SecurityContext securityContext, final Node dbNode, final RelationshipType relationshipType, final Direction direction, final Trait<?> trait, final Predicate<GraphTrait> predicate) {
		return Iterables.filter(new OtherNodeTypeFilter(securityContext, dbNode, trait, predicate), dbNode.getRelationships(direction, relationshipType));
	}

	// ----- protected methods -----
	/**
	 * Loads a PropertyMap from the current security context that was previously stored
	 * there by one of the Notions that was executed before this relationship creation.

	 * @param securityContext the security context
	 * @param traits the entity type
	 * @param storageKey the key for which the PropertyMap was stored
	 *
	 * @return a PropertyMap or null
	 */
	protected PropertyMap getNotionProperties(final SecurityContext securityContext, final Traits traits, final String storageKey) {

		final Map<String, PropertyMap> notionPropertyMap = (Map<String, PropertyMap>)securityContext.getAttribute("notionProperties");
		if (notionPropertyMap != null) {

			final Set<PropertyKey> keySet      = Services.getInstance().getConfigurationProvider().getPropertySet(traits, PropertyView.Public);
			final PropertyMap notionProperties = notionPropertyMap.get(storageKey);

			if (notionProperties != null) {

				for (final Iterator<PropertyKey> it = notionProperties.keySet().iterator(); it.hasNext();) {

					final PropertyKey key = it.next();
					if (!keySet.contains(key)) {

						it.remove();
					}
				}

				return notionProperties;
			}
		}

		return null;
	}

	protected GraphTrait unwrap(final GraphTrait node) throws FrameworkException {
		return unwrap(null, null, node, null);
	}

	protected GraphTrait unwrap(final SecurityContext securityContext, final Traits actualType, final GraphTrait node, final PropertyMap properties) throws FrameworkException {

		if (node != null && node instanceof EntityAndPropertiesContainer) {

			final EntityAndPropertiesContainer container = (EntityAndPropertiesContainer)node;

			if (securityContext != null && actualType != null && properties != null) {

				properties.putAll(PropertyMap.inputTypeToJavaType(securityContext, actualType, container.getProperties()));
			}

			return container.getEntity();
		}

		return node;
	}

	protected <T> Set<T> intersect(final Set<T> set1, final Set<T> set2) {

		final Set<T> intersection = new LinkedHashSet<>(set1);
		intersection.retainAll(set2);

		return intersection;
	}
}
