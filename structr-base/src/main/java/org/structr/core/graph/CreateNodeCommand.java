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

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.ConstraintViolationException;
import org.structr.api.DataFormatException;
import org.structr.api.DatabaseService;
import org.structr.api.graph.Node;
import org.structr.api.graph.Relationship;
import org.structr.api.util.NodeWithOwnerResult;
import org.structr.common.Permission;
import org.structr.common.PropertyView;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.Transformation;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.*;
import org.structr.core.entity.relationship.PrincipalOwnsNode;
import org.structr.core.property.AbstractPrimitiveProperty;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.property.TypeProperty;
import org.structr.core.traits.Traits;

import java.util.*;
import java.util.Map.Entry;

/**
 * Creates a new node in the database with the given properties.
 */
public class CreateNodeCommand extends NodeServiceCommand {

	private static final Logger logger = LoggerFactory.getLogger(CreateNodeCommand.class);

	private final PropertyKey<String> idKey                           = Traits.of("GraphObject").key("id");
	private final PropertyKey<String> typeKey                         = Traits.of("GraphObject").key("type");
	private final PropertyKey<Date> createdDateKey                    = Traits.of("GraphObject").key("createdDate");
	private final PropertyKey<Date> lastModifiedDateKey               = Traits.of("GraphObject").key("lastModifiedDate");
	private final PropertyKey<String> createdByKey                    = Traits.of("GraphObject").key("createdBy");
	private final PropertyKey<String> lastModifiedByKey               = Traits.of("GraphObject").key("lastModifiedBy");
	private final PropertyKey<Boolean> visibleToPublicUsersKey        = Traits.of("GraphObject").key("visibleToPublicUsers");
	private final PropertyKey<Boolean> visibleToAuthenticatedUsersKey = Traits.of("GraphObject").key("visibleToAuthenticatedUsers");
	private final PropertyKey<Boolean> hiddenKey                      = Traits.of("NodeInterface").key("hidden");

	public NodeInterface execute(final Collection<NodeAttribute<?>> attributes) throws FrameworkException {

		PropertyMap properties = new PropertyMap();
		for (NodeAttribute attribute : attributes) {

			properties.put(attribute.getKey(), attribute.getValue());
		}

		return execute(properties);

	}

	public NodeInterface execute(final NodeAttribute<?>... attributes) throws FrameworkException {

		PropertyMap properties = new PropertyMap();
		for (NodeAttribute attribute : attributes) {

			properties.put(attribute.getKey(), attribute.getValue());
		}

		return execute(properties);
	}

	public NodeInterface execute(final PropertyMap attributes) throws FrameworkException {

		final DatabaseService graphDb = (DatabaseService) arguments.get("graphDb");
		final Principal user          = securityContext.getUser(false);
		NodeInterface node	      = null;

		if (graphDb != null) {

			final NodeFactory nodeFactory = new NodeFactory(securityContext);
			final PropertyMap properties  = new PropertyMap(attributes);
			final PropertyMap toNotify    = new PropertyMap();
			final Object typeObject       = properties.get(typeKey);
			final Traits nodeType         = Traits.of(typeObject.toString());
			final String typeName         = nodeType.getName();
			final Set<String> labels      = nodeType.getLabels();
			final CreationContainer tmp   = new CreationContainer(true);
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
			idKey.setProperty(securityContext, tmp, uuid);
			typeKey.setProperty(securityContext, tmp, typeName);
			createdDateKey.setProperty(securityContext, tmp, now);
			lastModifiedDateKey.setProperty(securityContext, tmp, now);

			// default property values
			visibleToPublicUsersKey.setProperty(securityContext, tmp,        getOrDefault(properties, visibleToPublicUsersKey, false));
			visibleToAuthenticatedUsersKey.setProperty(securityContext, tmp, getOrDefault(properties, visibleToAuthenticatedUsersKey, false));
			hiddenKey.setProperty(securityContext, tmp,                      getOrDefault(properties, hiddenKey, false));

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

			if (nodeType.contains("Principal") && !nodeType.contains("Group")) {
				// If we are creating a node inheriting from Principal, force existence of password property
				// to enable complexity enforcement on creation (otherwise PasswordProperty.setProperty is not called)
				final PropertyKey<String> passwordKey = nodeType.key("password");
				if (isCreation && !properties.containsKey(passwordKey)) {
					properties.put(passwordKey, null);
				}
			}

			// move properties to creation container that can be set directly on creation
			tmp.filterIndexableForCreation(securityContext, properties, tmp, toNotify);

			// collect default values and try to set them on creation
			for (final PropertyKey key : nodeType.getFullPropertySet(PropertyView.All)) {

				if (key instanceof AbstractPrimitiveProperty && !tmp.hasProperty(key.jsonName())) {

					final Object defaultValue = key.defaultValue();
					if (defaultValue != null) {

						key.setProperty(securityContext, tmp, defaultValue);
					}
				}
			}

			node = nodeFactory.instantiateWithType(createNode(graphDb, user, typeName, labels, tmp.getData()), typeName, null, isCreation);
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
						TransactionCommand.nodeModified(securityContext.getCachedUser(), (AbstractNode)node, key, null, value);
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

			// iterate post creation transformations
			final Set<Transformation<GraphObject>> transformations = StructrApp.getConfiguration().getEntityCreationTransformations(node.getClass());
			for (Transformation<GraphObject> transformation : transformations) {

				transformation.apply(securityContext, node);
			}

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

		final Map<String, Object> ownsProperties     = new HashMap<>();
		final Map<String, Object> securityProperties = new HashMap<>();
		final String newUuid                         = (String)properties.get("id");

		if (user != null && user.shouldSkipSecurityRelationships() == false) {

			final String userId = user.getUuid();

			// configure OWNS relationship creation statement for maximum performance
			ownsProperties.put(idKey.dbName(),                          getNextUuid());
			ownsProperties.put(typeKey.dbName(),                        PrincipalOwnsNode.class.getSimpleName());
			ownsProperties.put(visibleToPublicUsersKey.dbName(),        false);
			ownsProperties.put(visibleToAuthenticatedUsersKey.dbName(), false);
			ownsProperties.put(AbstractRelationship.relType.dbName(),            "OWNS");
			ownsProperties.put(AbstractRelationship.sourceId.dbName(),           userId);
			ownsProperties.put(AbstractRelationship.targetId.dbName(),           newUuid);
			ownsProperties.put(AbstractRelationship.internalTimestamp.dbName(),  graphDb.getInternalTimestamp(0, 0));

			// configure SECURITY relationship creation statement for maximum performance
			securityProperties.put(idKey.dbName(),                          getNextUuid());
			securityProperties.put(typeKey.dbName(),                        SecurityRelationship.class.getSimpleName());
			securityProperties.put(visibleToPublicUsersKey.dbName(),        false);
			securityProperties.put(visibleToAuthenticatedUsersKey.dbName(), false);
			securityProperties.put(AbstractRelationship.relType.dbName(),            "SECURITY");
			securityProperties.put(AbstractRelationship.sourceId.dbName(),           userId);
			securityProperties.put(AbstractRelationship.targetId.dbName(),           newUuid);
			securityProperties.put(AbstractRelationship.internalTimestamp.dbName(),  graphDb.getInternalTimestamp(0, 1));
			securityProperties.put(SecurityRelationship.allowed.dbName(),                        new String[] { Permission.read.name(), Permission.write.name(), Permission.delete.name(), Permission.accessControl.name() } );
			securityProperties.put(SecurityRelationship.principalId.dbName(),                    userId);
			securityProperties.put(SecurityRelationship.accessControllableId.dbName(),           newUuid);

			try {

				final RelationshipFactory factory = new RelationshipFactory(securityContext);
				final NodeInterface userNode      = user.getWrappedNode();
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
			}


		} else {

			try {

				return graphDb.createNode(type, labels, properties);

			} catch (DataFormatException dex) {
				throw new FrameworkException(422, dex.getMessage());
			} catch (ConstraintViolationException qex) {
				throw new FrameworkException(422, qex.getMessage());
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
