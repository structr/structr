/*
 * Copyright (C) 2010-2025 Structr GmbH
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

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.ConstraintViolationException;
import org.structr.api.DataFormatException;
import org.structr.api.DatabaseService;
import org.structr.api.UnknownDatabaseException;
import org.structr.api.graph.Node;
import org.structr.api.graph.Relationship;
import org.structr.api.util.NodeWithOwnerResult;
import org.structr.common.Permission;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Principal;
import org.structr.core.property.AbstractPrimitiveProperty;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.definitions.PrincipalTraitDefinition;
import org.structr.core.traits.definitions.RelationshipInterfaceTraitDefinition;
import org.structr.core.traits.relationships.SecurityRelationshipDefinition;

import java.util.*;
import java.util.Map.Entry;

/**
 * Creates a new node in the database with the given properties.
 */
public class CreateNodeCommand extends NodeServiceCommand {

	private static final Logger logger = LoggerFactory.getLogger(CreateNodeCommand.class);

	public NodeInterface execute(final Collection<NodeAttribute<?>> attributes) throws FrameworkException {

		final PropertyMap properties = new PropertyMap();
		for (NodeAttribute attribute : attributes) {

			properties.put(attribute.getKey(), attribute.getValue());
		}

		return execute(properties);

	}

	public NodeInterface execute(final NodeAttribute<?>... attributes) throws FrameworkException {

		final PropertyMap properties = new PropertyMap();
		for (NodeAttribute attribute : attributes) {

			properties.put(attribute.getKey(), attribute.getValue());
		}

		return execute(properties);
	}

	public NodeInterface execute(final PropertyMap attributes) throws FrameworkException {

		final PropertyKey<String> idKey                           = Traits.key(StructrTraits.GRAPH_OBJECT, GraphObjectTraitDefinition.ID_PROPERTY);
		final PropertyKey<String> typeKey                         = Traits.key(StructrTraits.GRAPH_OBJECT, GraphObjectTraitDefinition.TYPE_PROPERTY);
		final PropertyKey<Date> createdDateKey                    = Traits.key(StructrTraits.GRAPH_OBJECT, GraphObjectTraitDefinition.CREATED_DATE_PROPERTY);
		final PropertyKey<Date> lastModifiedDateKey               = Traits.key(StructrTraits.GRAPH_OBJECT, GraphObjectTraitDefinition.LAST_MODIFIED_DATE_PROPERTY);
		final PropertyKey<String> createdByKey                    = Traits.key(StructrTraits.GRAPH_OBJECT, GraphObjectTraitDefinition.CREATED_BY_PROPERTY);
		final PropertyKey<String> lastModifiedByKey               = Traits.key(StructrTraits.GRAPH_OBJECT, GraphObjectTraitDefinition.LAST_MODIFIED_BY_PROPERTY);
		final PropertyKey<Boolean> visibleToPublicUsersKey        = Traits.key(StructrTraits.GRAPH_OBJECT, GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY);
		final PropertyKey<Boolean> visibleToAuthenticatedUsersKey = Traits.key(StructrTraits.GRAPH_OBJECT, GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY);
		final PropertyKey<Boolean> hiddenKey                      = Traits.key(StructrTraits.NODE_INTERFACE, NodeInterfaceTraitDefinition.HIDDEN_PROPERTY);
		final PropertyKey<String> passwordKey                     = Traits.key(StructrTraits.PRINCIPAL, PrincipalTraitDefinition.PASSWORD_PROPERTY);
		final DatabaseService graphDb                             = (DatabaseService) arguments.get("graphDb");
		final Principal user                                      = securityContext.getUser(false);
		NodeInterface node	                                  = null;

		if (graphDb != null) {

			final NodeFactory nodeFactory = new NodeFactory(securityContext);
			final PropertyMap properties  = new PropertyMap(attributes);
			final PropertyMap toNotify    = new PropertyMap();
			final Object typeObject       = properties.get(typeKey);
			final Traits nodeType         = Traits.of(typeObject.toString());
			final String typeName         = nodeType.getName();
			final Set<String> labels      = nodeType.getLabels();
			final CreationContainer tmp   = new CreationContainer(nodeType, true);
			final Date now                = new Date();
			final boolean isCreation      = true;

			// use user-supplied UUID?
			String uuid = properties.get(idKey);
			if (uuid == null) {

				// no, create new one
				uuid = getNextUuid();

				properties.put(idKey, uuid);

			} else {

				// enable UUID validation
				securityContext.uuidWasSetManually(true);
			}

			// use property keys to set property values on creation dummy
			// set default values for common properties in creation query
			idKey.setProperty(securityContext,               tmp, uuid);
			typeKey.setProperty(securityContext,             tmp, typeName);
			createdDateKey.setProperty(securityContext,      tmp, now);
			lastModifiedDateKey.setProperty(securityContext, tmp, now);

			// default property values
			visibleToPublicUsersKey.setProperty(securityContext,        tmp, getOrDefault(properties, visibleToPublicUsersKey,false));
			visibleToAuthenticatedUsersKey.setProperty(securityContext, tmp, getOrDefault(properties, visibleToAuthenticatedUsersKey, false));
			hiddenKey.setProperty(securityContext,                      tmp, getOrDefault(properties, hiddenKey,false));

			if (user != null) {

				final String userId = user.getUuid();

				createdByKey.setProperty(securityContext, tmp, userId);
				lastModifiedByKey.setProperty(securityContext, tmp, userId);
			}

			// prevent double setting of properties
			properties.remove(idKey);
			properties.remove(typeKey);
			properties.remove(visibleToPublicUsersKey);
			properties.remove(visibleToAuthenticatedUsersKey);
			properties.remove(hiddenKey);
			properties.remove(lastModifiedDateKey);
			properties.remove(lastModifiedByKey);
			properties.remove(createdDateKey);
			properties.remove(createdByKey);

			if (nodeType.contains(StructrTraits.PRINCIPAL) && !nodeType.contains(StructrTraits.GROUP)) {
				// If we are creating a node inheriting from Principal, force existence of password property
				// to enable complexity enforcement on creation (otherwise PasswordProperty.setProperty is not called)
				if (isCreation && !properties.containsKey(passwordKey)) {
					properties.put(passwordKey, null);
				}
			}

			// move properties to creation container that can be set directly on creation
			tmp.filterIndexableForCreation(securityContext, properties, tmp, toNotify);

			// collect default values and try to set them on creation
			for (final PropertyKey key : nodeType.getAllPropertyKeys()) {

				if (key instanceof AbstractPrimitiveProperty && !tmp.hasProperty(key.jsonName())) {

					final Object defaultValue = key.defaultValue();
					if (defaultValue != null) {

						key.setProperty(securityContext, tmp, defaultValue);
					}
				}
			}

			node = nodeFactory.instantiateWithType(createNode(graphDb, user, typeName, labels, tmp.getData()), null, isCreation);
			if (node != null) {

				TransactionCommand.nodeCreated(user, node);

				securityContext.disableModificationOfAccessTime();
				node.setProperties(securityContext, properties, true);
				securityContext.enableModificationOfAccessTime();

				// ensure modification callbacks are called (necessary for validation)
				for (final Entry<PropertyKey, Object> entry : toNotify.entrySet()) {

					final PropertyKey key = entry.getKey();
					final Object value    = entry.getValue();

					if (!key.isUnvalidated()) {
						TransactionCommand.nodeModified(securityContext.getCachedUser(), node, key, null, value);
					}
				}

				properties.clear();

				// ensure indexing of newly created node
				node.addToIndex();

				// invalidate UUID cache
				StructrApp.invalidate(uuid);
			}
		}

		if (node != null) {

			// return creation details?
			if (securityContext.returnDetailedCreationResults()) {

				final Map obj = new LinkedHashMap();

				obj.put("type", node.getType());
				obj.put("id", node.getUuid());

				securityContext.getCreationDetails().add(obj);
			}

			// notify node of its creation
			node.onNodeCreation(securityContext);
		}

		return node;
	}

	// ----- private methods -----
	private Node createNode(final DatabaseService graphDb, final Principal user, final String type, final Set<String> labels, final Map<String, Object> properties) throws FrameworkException {


		final PropertyKey<String> idKey                           = Traits.key(StructrTraits.GRAPH_OBJECT, GraphObjectTraitDefinition.ID_PROPERTY);
		final PropertyKey<String> typeKey                         = Traits.key(StructrTraits.GRAPH_OBJECT, GraphObjectTraitDefinition.TYPE_PROPERTY);
		final PropertyKey<Boolean> visibleToPublicUsersKey        = Traits.key(StructrTraits.GRAPH_OBJECT, GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY);
		final PropertyKey<Boolean> visibleToAuthenticatedUsersKey = Traits.key(StructrTraits.GRAPH_OBJECT, GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY);
		final Map<String, Object> ownsProperties                  = new HashMap<>();
		final Map<String, Object> securityProperties              = new HashMap<>();
		final String newUuid                                      = (String)properties.get("id");

		if (user != null && !user.shouldSkipSecurityRelationships()) {

			final Traits securityTraits                       = Traits.of(StructrTraits.SECURITY);
			final PropertyKey<String> internalTimestampKey    = securityTraits.key(RelationshipInterfaceTraitDefinition.INTERNAL_TIMESTAMP_PROPERTY);
			final PropertyKey<String> sourceIdKey             = securityTraits.key(RelationshipInterfaceTraitDefinition.SOURCE_ID_PROPERTY);
			final PropertyKey<String> targetIdKey             = securityTraits.key(RelationshipInterfaceTraitDefinition.TARGET_ID_PROPERTY);
			final PropertyKey<String> relTypeKey              = securityTraits.key(RelationshipInterfaceTraitDefinition.REL_TYPE_PROPERTY);
			final PropertyKey<String> principalIdKey          = securityTraits.key(SecurityRelationshipDefinition.PRINCIPAL_ID_PROPERTY);
			final PropertyKey<String> accessControllableIdKey = securityTraits.key(SecurityRelationshipDefinition.ACCESS_CONTROLLABLE_ID_PROPERTY);
			final PropertyKey<String[]> allowedKey            = securityTraits.key(SecurityRelationshipDefinition.ALLOWED_PROPERTY);
			final String userId                               = user.getUuid();

			// configure OWNS relationship creation statement for maximum performance
			ownsProperties.put(idKey.dbName(),                          getNextUuid());
			ownsProperties.put(typeKey.dbName(),                        StructrTraits.PRINCIPAL_OWNS_NODE);
			ownsProperties.put(visibleToPublicUsersKey.dbName(),        false);
			ownsProperties.put(visibleToAuthenticatedUsersKey.dbName(), false);
			ownsProperties.put(relTypeKey.dbName(),                     "OWNS");
			ownsProperties.put(sourceIdKey.dbName(),                    userId);
			ownsProperties.put(targetIdKey.dbName(),                    newUuid);
			ownsProperties.put(internalTimestampKey.dbName(),           graphDb.getInternalTimestamp(0, 0));

			// configure SECURITY relationship creation statement for maximum performance
			securityProperties.put(idKey.dbName(),                          getNextUuid());
			securityProperties.put(typeKey.dbName(),                        StructrTraits.SECURITY);
			securityProperties.put(visibleToPublicUsersKey.dbName(),        false);
			securityProperties.put(visibleToAuthenticatedUsersKey.dbName(), false);
			securityProperties.put(relTypeKey.dbName(),                     "SECURITY");
			securityProperties.put(sourceIdKey.dbName(),                    userId);
			securityProperties.put(targetIdKey.dbName(),                    newUuid);
			securityProperties.put(internalTimestampKey.dbName(),           graphDb.getInternalTimestamp(0, 1));
			securityProperties.put(allowedKey.dbName(),                     new String[] { Permission.read.name(), Permission.write.name(), Permission.delete.name(), Permission.accessControl.name() } );
			securityProperties.put(principalIdKey.dbName(),                 userId);
			securityProperties.put(accessControllableIdKey.dbName(),        newUuid);

			try {

				final RelationshipFactory factory = new RelationshipFactory(securityContext);
				final NodeInterface userNode      = user;
				final Node userDatabaseNode       = userNode.getNode();
				final NodeWithOwnerResult result  = graphDb.createNodeWithOwner(userDatabaseNode.getId(), type, labels, properties, ownsProperties, securityProperties);
				final Relationship securityRel    = result.getSecurityRelationship();
				final Relationship ownsRel        = result.getOwnsRelationship();
				final Node newNode                = result.getNewNode();

				notifySecurityRelCreation(factory, user, securityRel);
				notifyOwnsRelCreation(factory, user, ownsRel);

				return newNode;

			} catch (DataFormatException dex) {
				throw new FrameworkException(422, dex.getMessage());
			} catch (ConstraintViolationException qex) {
				throw new FrameworkException(422, qex.getMessage());
			} catch (UnknownDatabaseException udbex) {
				throw new FrameworkException(422, udbex.getMessage());
			}


		} else {

			try {

				return graphDb.createNode(type, labels, properties);

			} catch (DataFormatException dex) {
				throw new FrameworkException(422, dex.getMessage());
			} catch (ConstraintViolationException qex) {
				throw new FrameworkException(422, qex.getMessage());
			} catch (UnknownDatabaseException udbex) {
				throw new FrameworkException(422, udbex.getMessage());
			}
		}
	}

	private <T> T getOrDefault(final PropertyMap src, final PropertyKey<T> key, final T defaultValue) {

		final T value = src.get(key);
		if (value != null) {

			return value;
		}

		return defaultValue;
	}

	private void notifySecurityRelCreation(final RelationshipFactory factory, final Principal user, final Relationship rel) {


		try {

			final RelationshipInterface securityRelationship = factory.instantiate(rel);
			if (securityRelationship != null) {

				TransactionCommand.relationshipCreated(user, securityRelationship);
			}

		} catch (ClassCastException cce) {

			logger.warn("Encountered ClassCastException which is likely caused by faulty cache invalidation. Relationship ID {} of type {}, start and end node IDs: {}, {}",
				rel.getId(),
				rel.getType().name(),
				rel.getStartNode() != null ? rel.getStartNode().getId() : "null",
				rel.getEndNode()   != null ? rel.getEndNode().getId()   : "null"
			);
			logger.warn(ExceptionUtils.getStackTrace(cce));

			try {

				// try again
				final RelationshipInterface securityRelationship = factory.instantiate(rel);
				if (securityRelationship != null) {

					TransactionCommand.relationshipCreated(user, securityRelationship);
				}

			} catch (ClassCastException cce2) {
				logger.warn("ClassCastException still exists, giving up.");
			}

		}
	}

	private void notifyOwnsRelCreation(final RelationshipFactory factory, final Principal user, final Relationship rel) {

		try {

			final RelationshipInterface ownsRelationship = factory.instantiate(rel);
			if (ownsRelationship != null) {

				TransactionCommand.relationshipCreated(user, ownsRelationship);
			}

		} catch (ClassCastException cce) {

			logger.warn("Encountered ClassCastException which is likely caused by faulty cache invalidation. Relationship ID {} of type {}, start and end node IDs: {}, {}",
				rel.getId(),
				rel.getType().name(),
				rel.getStartNode() != null ? rel.getStartNode().getId() : "null",
				rel.getEndNode()   != null ? rel.getEndNode().getId()   : "null"
			);
			logger.warn(ExceptionUtils.getStackTrace(cce));

			try {
				// try again
				final RelationshipInterface ownsRelationship = factory.instantiate(rel);
				if (ownsRelationship != null) {

					TransactionCommand.relationshipCreated(user, ownsRelationship);
				}

			} catch (ClassCastException cce2) {
				logger.warn("ClassCastException still exists, giving up.");
			}

		}
	}
}
