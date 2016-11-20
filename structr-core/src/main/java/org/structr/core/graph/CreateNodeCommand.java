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
package org.structr.core.graph;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.structr.api.DatabaseService;
import org.structr.api.NativeResult;
import org.structr.api.graph.Node;
import org.structr.common.Permission;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.Transformation;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Principal;
import org.structr.core.entity.Security;
import org.structr.core.entity.SuperUser;
import org.structr.core.property.PropertyMap;
import org.structr.core.property.TypeProperty;
import org.structr.schema.SchemaHelper;

//~--- classes ----------------------------------------------------------------

/**
 * Creates a new node in the database with the given properties.
 *
 *
 */
public class CreateNodeCommand<T extends NodeInterface> extends NodeServiceCommand {

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
			final Object typeObject          = properties.get(AbstractNode.type);
			final Class nodeType             = getTypeOrGeneric(typeObject);
			final Set<String> labels         = TypeProperty.getLabelsForType(nodeType);
			final CreationContainer tmp      = new CreationContainer();
			final Date now                   = new Date();
			final boolean isCreation         = true;

			// use user-supplied UUID?
			String uuid = properties.get(GraphObject.id);
			if (uuid == null) {

				// no, create new one
				uuid = getNextUuid();
			}

			// use property keys to set property values on creation dummy
			GraphObject.id.setProperty(securityContext, tmp, uuid);
			GraphObject.type.setProperty(securityContext, tmp, nodeType.getSimpleName());
			AbstractNode.createdDate.setProperty(securityContext, tmp, now);
			AbstractNode.lastModifiedDate.setProperty(securityContext, tmp, now);

			if (user != null) {

				final String userId = user.getProperty(GraphObject.id);

				AbstractNode.createdBy.setProperty(securityContext, tmp, userId);
				AbstractNode.lastModifiedBy.setProperty(securityContext, tmp, userId);
			}

			// prevent double setting of properties
			properties.remove(GraphObject.id);
			properties.remove(AbstractNode.type);
			properties.remove(AbstractNode.lastModifiedDate);
			properties.remove(AbstractNode.lastModifiedBy);
			properties.remove(AbstractNode.createdDate);
			properties.remove(AbstractNode.createdBy);

			node = (T) nodeFactory.instantiateWithType(createNode(graphDb, user, nodeType, labels, tmp.getData()), nodeType, null, isCreation);
			if (node != null) {

				TransactionCommand.nodeCreated(user, node);

				node.setProperties(securityContext, properties);

				properties.clear();

				// ensure indexing of newly created node
				node.addToIndex();
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
		}

		return node;
	}

	// ----- private methods -----
	private Node createNode(final DatabaseService graphDb, final Principal user, final Class nodeType, final Set<String> labels, final Map<String, Object> properties) {

		final Map<String, Object> parameters         = new HashMap<>();
		final Map<String, Object> ownsProperties     = new HashMap<>();
		final Map<String, Object> securityProperties = new HashMap<>();
		final StringBuilder buf                      = new StringBuilder();

		if (user != null && !(user instanceof SuperUser)) {

			buf.append("MATCH (u:Principal) WHERE id(u) = {userId}");
			buf.append(" CREATE (u)-[o:OWNS {ownsProperties}]->(n");

			for (final String label : labels) {

				buf.append(":");
				buf.append(label);
			}

			buf.append(" {nodeProperties})<-[s:SECURITY {securityProperties}]-(u)");
			buf.append(" RETURN n");

			// configure OWNS relationship
			ownsProperties.put(GraphObject.id.dbName(), getNextUuid());

			// configure SECURITY relationship
			securityProperties.put(Security.allowed.dbName(), new String[] { Permission.read.name(), Permission.write.name(), Permission.delete.name(), Permission.accessControl.name() } );
			securityProperties.put(GraphObject.id.dbName(), getNextUuid());

			// store properties in statement
			parameters.put("userId",             user.getId());
			parameters.put("ownsProperties",     ownsProperties);
			parameters.put("securityProperties", securityProperties);

		} else {

			buf.append("CREATE (n");

			for (final String label : labels) {

				buf.append(":");
				buf.append(label);
			}

			buf.append(" {nodeProperties})");
			buf.append(" RETURN n");
		}

		// make properties available to Cypher statement
		parameters.put("nodeProperties", properties);


		final NativeResult result = graphDb.execute(buf.toString(), parameters);
		if (result.hasNext()) {

			final Map<String, Object> data = result.next();
			final Node newNode             = (Node)data.get("n");

			return newNode;
		}

		throw new RuntimeException("Unable to create new node.");
	}

	private NodeInterface getTemplate(final Class nodeType) {

		try {

			return (NodeInterface)nodeType.newInstance();

		} catch (Throwable ignore) {}

		return null;
	}

	private Class getTypeOrGeneric(final Object typeObject) {

		if (typeObject != null) {
			return SchemaHelper.getEntityClassForRawType(typeObject.toString());
		}

		return StructrApp.getConfiguration().getFactoryDefinition().getGenericNodeType();
	}
}
