/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.entity;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.helpers.Predicate;
import org.neo4j.helpers.collection.Iterables;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;

/**
 *
 * @author Christian Morgner
 */
public abstract class AbstractEndpoint {

	private static final Logger logger = Logger.getLogger(AbstractEndpoint.class.getName());

	public Relationship getSingle(final SecurityContext securityContext, final Node dbNode, final RelationshipType relationshipType, final Direction direction, final Class otherNodeType) {

		final Iterable<Relationship> rels     = getMultiple(securityContext, dbNode, relationshipType, direction, otherNodeType, null);
		final Iterator<Relationship> iterator = rels.iterator();

		// FIXME: this returns only the first relationship that matches, i.e. there is NO check for multiple relationships
		if (iterator.hasNext()) {
			return iterator.next();
		}

		return null;
	}

	public Iterable<Relationship> getMultiple(final SecurityContext securityContext, final Node dbNode, final RelationshipType relationshipType, final Direction direction, final Class otherNodeType, final Predicate<GraphObject> predicate) {
		return Iterables.filter(new OtherNodeTypeFilter(securityContext, dbNode, otherNodeType, predicate), dbNode.getRelationships(direction, relationshipType));
	}

	// ----- protected methods -----
	/**
	 * Loads a PropertyMap from the current security context that was previously stored
	 * there by one of the Notions that was executed before this relationship creation.

	 * @param securityContext the security context
	 * @param type the entity type
	 * @param entityKey the key for which the PropertyMap was stored
	 *
	 * @return a PropertyMap or null
	 */
	protected PropertyMap getNotionProperties(final SecurityContext securityContext, final Class type, final String entityKey) {


		final Map<String, PropertyMap> notionPropertyMap = (Map<String, PropertyMap>)securityContext.getAttribute("notionProperties");
		if (notionPropertyMap != null) {

			final Set<PropertyKey> keySet      = Services.getInstance().getConfigurationProvider().getPropertySet(type, PropertyView.Public);
			final PropertyMap notionProperties = notionPropertyMap.get(entityKey);

			for (final Iterator<PropertyKey> it = notionProperties.keySet().iterator(); it.hasNext();) {

				final PropertyKey key = it.next();
				if (!keySet.contains(key)) {

					it.remove();
				}
			}

			return notionProperties;
		}

		return null;
	}
}
