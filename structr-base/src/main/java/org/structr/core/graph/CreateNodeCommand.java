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
import org.structr.schema.SchemaHelper;

import java.util.*;
import java.util.Map.Entry;

/**
 * Creates a new node in the database with the given properties.
 */
public class CreateNodeCommand<T extends NodeInterface> extends NodeServiceCommand {

	private static final Logger logger = LoggerFactory.getLogger(CreateNodeCommand.class);

	public T execute(final Collection<NodeAttribute<?>> attributes) throws FrameworkException {

		PropertyMap properties = new PropertyMap();
		for (NodeAttribute attribute : attributes) {

			properties.put(attribute.getKey(), attribute.getValue());
		}

		return execute(properties);

	}

	public T execute(final NodeAttribute<?>... attributes) throws FrameworkException {

		PropertyMap properties = new PropertyMap();
		for (NodeAttribute attribute : attributes) {

			properties.put(attribute.getKey(), attribute.getValue());
		}

		return execute(properties);
	}

	public T execute(final PropertyMap attributes) throws FrameworkException {

		final DatabaseService graphDb = (DatabaseService) arguments.get("graphDb");
		final Principal user          = securityContext.getUser(false);
		T node	                      = null;

		if (graphDb != null) {

			final NodeFactory<T> nodeFactory = new NodeFactory<>(securityContext);
			final PropertyMap properties     = new PropertyMap(attributes);
			final PropertyMap toNotify       = new PropertyMap();
			final Object typeObject          = properties.get(AbstractNode.type);
			final Class nodeType             = getTypeOrGeneric(typeObject);
			final String typeName            = nodeType.getSimpleName();
			final Set<String> labels         = TypeProperty.getLabelsForType(nodeType);
			final CreationContainer tmp      = new CreationContainer(true);
			final Date now                   = new Date();
			final boolean isCreation         = true;

			// use user-supplied UUID?
			String uuid = properties.get(GraphObject.id);
			if (uuid == null) {

				// no, create new one
				uuid = getNextUuid();

				properties.put(GraphObject.id, uuid);

			} else {

				// enable UUID validation
				securityContext.uuidWasSetManually(true);
			}

			// use property keys to set property values on creation dummy
			// set default values for common properties in creation query
			GraphObject.id.setProperty(securityContext, tmp, uuid);
			GraphObject.type.setProperty(securityContext, tmp, typeName);
			AbstractNode.createdDate.setProperty(securityContext, tmp, now);
			AbstractNode.lastModifiedDate.setProperty(securityContext, tmp, now);

			// default property values
			AbstractNode.visibleToPublicUsers.setProperty(securityContext, tmp,        getOrDefault(properties, AbstractNode.visibleToPublicUsers, false));
			AbstractNode.visibleToAuthenticatedUsers.setProperty(securityContext, tmp, getOrDefault(properties, AbstractNode.visibleToAuthenticatedUsers, false));
			AbstractNode.hidden.setProperty(securityContext, tmp,                      getOrDefault(properties, AbstractNode.hidden, false));

			if (user != null) {

				final String userId = user.getProperty(GraphObject.id);

				AbstractNode.createdBy.setProperty(securityContext, tmp, userId);
				AbstractNode.lastModifiedBy.setProperty(securityContext, tmp, userId);
			}

			// prevent double setting of properties
			properties.remove(AbstractNode.id);
			properties.remove(AbstractNode.type);
			properties.remove(AbstractNode.visibleToPublicUsers);
			properties.remove(AbstractNode.visibleToAuthenticatedUsers);
			properties.remove(AbstractNode.hidden);
			properties.remove(AbstractNode.lastModifiedDate);
			properties.remove(AbstractNode.lastModifiedBy);
			properties.remove(AbstractNode.createdDate);
			properties.remove(AbstractNode.createdBy);

			if (Principal.class.isAssignableFrom(nodeType) && !Group.class.isAssignableFrom(nodeType)) {
				// If we are creating a node inheriting from Principal, force existence of password property
				// to enable complexity enforcement on creation (otherwise PasswordProperty.setProperty is not called)
				final PropertyKey passwordKey = StructrApp.key(Principal.class, "password", false);
				if (isCreation && !properties.containsKey(passwordKey)) {
					properties.put(passwordKey, null);
				}
			}

			// move properties to creation container that can be set directly on creation
			tmp.filterIndexableForCreation(securityContext, properties, tmp, toNotify);

			// collect default values and try to set them on creation
			for (final PropertyKey key : StructrApp.getConfiguration().getPropertySet(nodeType, PropertyView.All)) {

				if (key instanceof AbstractPrimitiveProperty && !tmp.hasProperty(key.jsonName())) {

					final Object defaultValue = key.defaultValue();
					if (defaultValue != null) {

						key.setProperty(securityContext, tmp, defaultValue);
					}
				}
			}

			node = (T) nodeFactory.instantiateWithType(createNode(graphDb, user, typeName, labels, tmp.getData()), nodeType, null, isCreation);
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

			// notify node of its creation
			node.onNodeCreation();

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
			ownsProperties.put(GraphObject.id.dbName(),                          getNextUuid());
			ownsProperties.put(GraphObject.type.dbName(),                        PrincipalOwnsNode.class.getSimpleName());
			ownsProperties.put(GraphObject.visibleToPublicUsers.dbName(),        false);
			ownsProperties.put(GraphObject.visibleToAuthenticatedUsers.dbName(), false);
			ownsProperties.put(AbstractRelationship.relType.dbName(),            "OWNS");
			ownsProperties.put(AbstractRelationship.sourceId.dbName(),           userId);
			ownsProperties.put(AbstractRelationship.targetId.dbName(),           newUuid);
			ownsProperties.put(AbstractRelationship.internalTimestamp.dbName(),  graphDb.getInternalTimestamp(0, 0));

			// configure SECURITY relationship creation statement for maximum performance
			securityProperties.put(GraphObject.id.dbName(),                          getNextUuid());
			securityProperties.put(GraphObject.type.dbName(),                        Security.class.getSimpleName());
			securityProperties.put(GraphObject.visibleToPublicUsers.dbName(),        false);
			securityProperties.put(GraphObject.visibleToAuthenticatedUsers.dbName(), false);
			securityProperties.put(AbstractRelationship.relType.dbName(),            "SECURITY");
			securityProperties.put(AbstractRelationship.sourceId.dbName(),           userId);
			securityProperties.put(AbstractRelationship.targetId.dbName(),           newUuid);
			securityProperties.put(AbstractRelationship.internalTimestamp.dbName(),  graphDb.getInternalTimestamp(0, 1));
			securityProperties.put(Security.allowed.dbName(),                        new String[] { Permission.read.name(), Permission.write.name(), Permission.delete.name(), Permission.accessControl.name() } );
			securityProperties.put(Security.principalId.dbName(),                    userId);
			securityProperties.put(Security.accessControllableId.dbName(),           newUuid);

			try {

				final NodeWithOwnerResult result = graphDb.createNodeWithOwner(user.getNode().getId(), type, labels, properties, ownsProperties, securityProperties);
				final Relationship securityRel   = result.getSecurityRelationship();
				final Relationship ownsRel       = result.getOwnsRelationship();
				final Node newNode               = result.getNewNode();

				notifySecurityRelCreation(user, securityRel);
				notifyOwnsRelCreation(user, ownsRel);

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

	private Class getTypeOrGeneric(final Object typeObject) {

		if (typeObject != null) {
			return SchemaHelper.getEntityClassForRawType(typeObject.toString());
		}

		return StructrApp.getConfiguration().getFactoryDefinition().getGenericNodeType();
	}

	private <T> T getOrDefault(final PropertyMap src, final PropertyKey<T> key, final T defaultValue) {

		final T value = src.get(key);
		if (value != null) {

			return value;
		}

		return defaultValue;
	}

	private void notifySecurityRelCreation(final Principal user, final Relationship rel) {

		final RelationshipFactory<Security> factory = new RelationshipFactory<>(securityContext);

		try {

			final Security securityRelationship = factory.instantiate(rel);
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
				final Security securityRelationship = factory.instantiate(rel);
				if (securityRelationship != null) {

					TransactionCommand.relationshipCreated(user, securityRelationship);
				}

			} catch (ClassCastException cce2) {
				logger.warn("ClassCastException still exists, giving up.");
			}

		}
	}

	private void notifyOwnsRelCreation(final Principal user, final Relationship rel) {

		final RelationshipFactory<PrincipalOwnsNode> factory = new RelationshipFactory<>(securityContext);

		try {

			final PrincipalOwnsNode ownsRelationship = factory.instantiate(rel);
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
				final PrincipalOwnsNode ownsRelationship = factory.instantiate(rel);
				if (ownsRelationship != null) {

					TransactionCommand.relationshipCreated(user, ownsRelationship);
				}

			} catch (ClassCastException cce2) {
				logger.warn("ClassCastException still exists, giving up.");
			}

		}
	}
}
