/**
 * Copyright (C) 2010-2017 Structr GmbH
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
package org.structr.core.graph;

import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.graph.Node;
import org.structr.api.graph.Relationship;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.Transformation;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.entity.Principal;
import org.structr.core.entity.Relation;
import org.structr.core.property.PropertyMap;

/**
 * Creates a relationship between two NodeInterface instances. The execute
 * method of this command takes the following parameters.
 *
 */
public class CreateRelationshipCommand extends NodeServiceCommand {

	private static final Logger logger = LoggerFactory.getLogger(CreateRelationshipCommand.class.getName());

	public <A extends NodeInterface, B extends NodeInterface, R extends Relation<A, B, ?, ?>> R execute(final A fromNode, final B toNode, final Class<R> relType) throws FrameworkException {
		return createRelationship(fromNode, toNode, relType, null);
	}

	public <A extends NodeInterface, B extends NodeInterface, R extends Relation<A, B, ?, ?>> R execute(final A fromNode, final B toNode, final Class<R> relType, final PropertyMap properties) throws FrameworkException {
		return createRelationship(fromNode, toNode, relType, properties);
	}

	private synchronized <A extends NodeInterface, B extends NodeInterface, R extends Relation<A, B, ?, ?>> R createRelationship(final A fromNode, final B toNode, final Class<R> relType, final PropertyMap attributes) throws FrameworkException {

		// disable updating access time when creating relationships
		securityContext.disableModificationOfAccessTime();

		final RelationshipFactory<R> factory = new RelationshipFactory(securityContext);
		final PropertyMap properties         = new PropertyMap(attributes);
		final CreationContainer tmp          = new CreationContainer();
		final R template                     = instantiate(relType);
		final Node startNode                 = fromNode.getNode();
		final Node endNode                   = toNode.getNode();
		final Date now                       = new Date();
		final Principal user                 = securityContext.getCachedUser();

		template.ensureCardinality(securityContext, fromNode, toNode);

		// date properties need converter
		AbstractRelationship.createdDate.setProperty(securityContext, tmp, now);
		AbstractRelationship.lastModifiedDate.setProperty(securityContext, tmp, now);

		// set initial properties manually (caution, this can only be used for primitive properties!)
		tmp.getData().put(GraphObject.id.jsonName(), getNextUuid());
		tmp.getData().put(GraphObject.type.jsonName(), relType.getSimpleName());
		tmp.getData().put(AbstractRelationship.relType.jsonName(), template.name());
		tmp.getData().put(AbstractRelationship.sourceId.jsonName(), fromNode.getUuid());
		tmp.getData().put(AbstractRelationship.targetId.jsonName(), toNode.getUuid());
		tmp.getData().put(AbstractRelationship.visibleToPublicUsers.jsonName(), false);
		tmp.getData().put(AbstractRelationship.visibleToAuthenticatedUsers.jsonName(), false);
		tmp.getData().put(AbstractRelationship.cascadeDelete.jsonName(), template.getCascadingDeleteFlag());

		if (user != null) {
			tmp.getData().put(AbstractRelationship.createdBy.jsonName(), user.getUuid());
		}

		// create relationship including initial properties
		final Relationship rel = startNode.createRelationshipTo(endNode, template, tmp.getData());
		final R newRel         = factory.instantiateWithType(rel, relType, null, true);
		if (newRel != null) {

			newRel.setProperties(securityContext, properties);

			// notify transaction handler
			TransactionCommand.relationshipCreated(user, newRel);

			// notify relationship of its creation
			newRel.onRelationshipCreation();

			// iterate post creation transformations
			for (Transformation<GraphObject> transformation : StructrApp.getConfiguration().getEntityCreationTransformations(newRel.getClass())) {

				transformation.apply(securityContext, newRel);
			}
		}

		// enable access time update again for subsequent calls
		securityContext.enableModificationOfAccessTime();

		return newRel;
	}

	private <T extends Relation> T instantiate(final Class<T> type) {

		try {

			return type.newInstance();

		} catch(Throwable t) {
			logger.warn("", t);
		}

		return null;
	}
}
