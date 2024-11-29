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
package org.structr.core.graph;

import org.structr.api.DatabaseService;
import org.structr.api.graph.Node;
import org.structr.api.graph.Relationship;
import org.structr.common.PropertyView;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.Transformation;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.entity.Principal;
import org.structr.core.entity.Relation;
import org.structr.core.property.AbstractPrimitiveProperty;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;

import java.util.Date;
import java.util.Map.Entry;

/**
 * Creates a relationship between two NodeInterface instances. The execute
 * method of this command takes the following parameters.
 *
 */
public class CreateRelationshipCommand extends NodeServiceCommand {

	public <A extends NodeInterface, B extends NodeInterface, R extends Relation<A, B, ?, ?>> RelationshipInterface<A, B> execute(final A fromNode, final B toNode, final Class<R> relType) throws FrameworkException {
		return createRelationship(fromNode, toNode, relType, null);
	}

	public <A extends NodeInterface, B extends NodeInterface, R extends Relation<A, B, ?, ?>> RelationshipInterface<A, B> execute(final A fromNode, final B toNode, final Class<R> relType, final PropertyMap properties) throws FrameworkException {
		return createRelationship(fromNode, toNode, relType, properties);
	}

	private <A extends NodeInterface, B extends NodeInterface, R extends Relation<A, B, ?, ?>> RelationshipInterface<A, B> createRelationship(final A fromNode, final B toNode, final Class<R> relType, final PropertyMap attributes) throws FrameworkException {

		// disable updating access time when creating relationships
		securityContext.disableModificationOfAccessTime();

		final DatabaseService db                                             = (DatabaseService)this.getArgument("graphDb");
		final RelationshipFactory<A, B, RelationshipInterface<A, B>> factory = new RelationshipFactory(securityContext);
		final PropertyMap properties                                         = new PropertyMap(attributes);
		final PropertyMap toNotify                                           = new PropertyMap();
		final CreationContainer tmp                                          = new CreationContainer(false);
		final R template                                                     = (R)Relation.getInstance(relType);
		final Date now                                                       = new Date();
		final Principal user                                        = securityContext.getCachedUser();

		template.ensureCardinality(securityContext, fromNode, toNode);

		// date properties need converter
		AbstractRelationship.internalTimestamp.setProperty(securityContext, tmp, db.getInternalTimestamp(0, 0));
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

		if (user != null) {
			tmp.getData().put(AbstractRelationship.createdBy.jsonName(), user.getUuid());
		}

		// move properties to creation container that can be set directly on creation
		tmp.filterIndexableForCreation(securityContext, properties, tmp, toNotify);

		// collect default values and try to set them on creation
		for (final PropertyKey key : StructrApp.getConfiguration().getPropertySet(relType, PropertyView.All)) {

			if (key instanceof AbstractPrimitiveProperty && !tmp.hasProperty(key.jsonName())) {

				final Object defaultValue = key.defaultValue();
				if (defaultValue != null) {

					key.setProperty(securityContext, tmp, defaultValue);
				}
			}
		}

		// create relationship including initial properties
		final Node startNode               = fromNode.getNode();
		final Node endNode                 = toNode.getNode();
		final Relationship rel             = startNode.createRelationshipTo(endNode, template, tmp.getData());
		final RelationshipInterface newRel = factory.instantiateWithType(rel, relType, null, true);

		if (newRel != null) {

			// notify transaction handler
			TransactionCommand.relationshipCreated(user, newRel);

			securityContext.disableModificationOfAccessTime();
			newRel.setProperties(securityContext, properties);
			securityContext.enableModificationOfAccessTime();

			// ensure modification callbacks are called (necessary for validation)
			for (final Entry<PropertyKey, Object> entry : toNotify.entrySet()) {

				final PropertyKey key = entry.getKey();
				final Object value    = entry.getValue();

				if (!key.isUnvalidated()) {
					TransactionCommand.relationshipModified(securityContext.getCachedUser(), (AbstractRelationship)newRel, key, null, value);
				}
			}

			properties.clear();

			// ensure indexing of newly created node
			newRel.addToIndex();

			// iterate post creation transformations
			for (Transformation<GraphObject> transformation : StructrApp.getConfiguration().getEntityCreationTransformations(newRel.getClass())) {

				transformation.apply(securityContext, newRel);
			}
		}

		// enable access time update again for subsequent calls
		securityContext.enableModificationOfAccessTime();

		return newRel;
	}
}
