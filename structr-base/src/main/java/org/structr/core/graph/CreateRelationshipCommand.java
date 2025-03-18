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
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.Principal;
import org.structr.core.entity.Relation;
import org.structr.core.property.AbstractPrimitiveProperty;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;

import java.util.Date;
import java.util.Map.Entry;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.definitions.RelationshipInterfaceTraitDefinition;

/**
 * Creates a relationship between two NodeInterface instances. The execute
 * method of this command takes the following parameters.
 *
 */
public class CreateRelationshipCommand extends NodeServiceCommand {

	private final PropertyKey<String> idKey                           = Traits.key(StructrTraits.GRAPH_OBJECT, GraphObjectTraitDefinition.ID_PROPERTY);
	private final PropertyKey<String> typeKey                         = Traits.key(StructrTraits.GRAPH_OBJECT, GraphObjectTraitDefinition.TYPE_PROPERTY);
	private final PropertyKey<Date> createdDateKey                    = Traits.key(StructrTraits.GRAPH_OBJECT, GraphObjectTraitDefinition.CREATED_DATE_PROPERTY);
	private final PropertyKey<Date> lastModifiedDateKey               = Traits.key(StructrTraits.GRAPH_OBJECT, GraphObjectTraitDefinition.LAST_MODIFIED_DATE_PROPERTY);
	private final PropertyKey<String> createdByKey                    = Traits.key(StructrTraits.GRAPH_OBJECT, GraphObjectTraitDefinition.CREATED_BY_PROPERTY);
	private final PropertyKey<Boolean> visibleToPublicUsersKey        = Traits.key(StructrTraits.GRAPH_OBJECT, GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY);
	private final PropertyKey<Boolean> visibleToAuthenticatedUsersKey = Traits.key(StructrTraits.GRAPH_OBJECT, GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY);

	public RelationshipInterface execute(final NodeInterface fromNode, final NodeInterface toNode, final String relType) throws FrameworkException {
		return createRelationship(fromNode, toNode, relType, null);
	}

	public RelationshipInterface execute(final NodeInterface fromNode, final NodeInterface toNode, final String relType, final PropertyMap properties) throws FrameworkException {
		return createRelationship(fromNode, toNode, relType, properties);
	}

	private RelationshipInterface createRelationship(final NodeInterface fromNode, final NodeInterface toNode, final String entityType, final PropertyMap attributes) throws FrameworkException {

		// disable updating access time when creating relationships
		securityContext.disableModificationOfAccessTime();

		final Traits relationshipTraits                = Traits.of(entityType);
		final PropertyKey<String> internalTimestampKey = relationshipTraits.key(RelationshipInterfaceTraitDefinition.INTERNAL_TIMESTAMP_PROPERTY);
		final PropertyKey<String> sourceIdKey          = relationshipTraits.key(RelationshipInterfaceTraitDefinition.SOURCE_ID_PROPERTY);
		final PropertyKey<String> targetIdKey          = relationshipTraits.key(RelationshipInterfaceTraitDefinition.TARGET_ID_PROPERTY);
		final PropertyKey<String> relTypeKey           = relationshipTraits.key(RelationshipInterfaceTraitDefinition.REL_TYPE_PROPERTY);
		final DatabaseService db                       = (DatabaseService)this.getArgument("graphDb");
		final RelationshipFactory factory              = new RelationshipFactory(securityContext);
		final PropertyMap properties                   = new PropertyMap(attributes);
		final PropertyMap toNotify                     = new PropertyMap();
		final CreationContainer tmp                    = new CreationContainer(false);
		final Traits traits                            = Traits.of(entityType);
		final Relation relation                        = traits.getRelation();
		final Date now                                 = new Date();
		final Principal user                           = securityContext.getCachedUser();

		relation.ensureCardinality(securityContext, fromNode, toNode);

		// date properties need converter
		internalTimestampKey.setProperty(securityContext, tmp, db.getInternalTimestamp(0, 0));
		createdDateKey.setProperty(securityContext, tmp, now);
		lastModifiedDateKey.setProperty(securityContext, tmp, now);

		// set initial properties manually (caution, this can only be used for primitive properties!)
		tmp.getData().put(idKey.dbName(), getNextUuid());
		tmp.getData().put(typeKey.dbName(), entityType);
		tmp.getData().put(relTypeKey.dbName(), relation.name());
		tmp.getData().put(sourceIdKey.dbName(), fromNode.getUuid());
		tmp.getData().put(targetIdKey.dbName(), toNode.getUuid());
		tmp.getData().put(visibleToPublicUsersKey.dbName(), false);
		tmp.getData().put(visibleToAuthenticatedUsersKey.dbName(), false);

		if (user != null) {
			tmp.getData().put(createdByKey.dbName(), user.getUuid());
		}

		// move properties to creation container that can be set directly on creation
		tmp.filterIndexableForCreation(securityContext, properties, tmp, toNotify);

		// collect default values and try to set them on creation
		for (final PropertyKey key : traits.getAllPropertyKeys()) {

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
		final Relationship rel             = startNode.createRelationshipTo(endNode, relation, tmp.getData());
		final RelationshipInterface newRel = factory.instantiateWithType(rel, null, true);

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
					TransactionCommand.relationshipModified(securityContext.getCachedUser(), newRel, key, null, value);
				}
			}

			properties.clear();

			// ensure indexing of newly created node
			newRel.addToIndex();

		}

		// enable access time update again for subsequent calls
		securityContext.enableModificationOfAccessTime();

		return newRel;
	}
}
