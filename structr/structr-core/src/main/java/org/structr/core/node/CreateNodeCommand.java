/*
 *  Copyright (C) 2010-2012 Axel Morgner, structr <structr@structr.org>
 *
 *  This file is part of structr <http://structr.org>.
 *
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */



package org.structr.core.node;

import org.neo4j.graphdb.GraphDatabaseService;

import org.structr.common.RelType;
import org.structr.common.error.FrameworkException;
import org.structr.core.Command;
import org.structr.core.EntityContext;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.Transformation;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.entity.SuperUser;
import org.structr.core.entity.Principal;

//~--- JDK imports ------------------------------------------------------------

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.Permission;
import org.structr.common.SecurityContext;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author cmorgner
 */
public class CreateNodeCommand extends NodeServiceCommand {

	private static final Logger logger = Logger.getLogger(CreateNodeCommand.class.getName());

	//~--- methods --------------------------------------------------------

	@Override
	public Object execute(Object... parameters) throws FrameworkException {

		GraphDatabaseService graphDb = (GraphDatabaseService) arguments.get("graphDb");
		Principal user               = securityContext.getUser();
		AbstractNode node            = null;

		if (graphDb != null) {

			Date now                  = new Date();
			Command createRel         = Services.command(securityContext, CreateRelationshipCommand.class);
			Map<String, Object> attrs = new HashMap<String, Object>();

			// initialize node from parameters...
			for (Object o : parameters) {

				if (o instanceof Map) {

					Map<String, Object> map = (Map<String, Object>) o;

					attrs.putAll(map);

				} else if (o instanceof Collection) {

					Collection<NodeAttribute> c = (Collection) o;

					for (NodeAttribute attr : c) {

						attrs.put(attr.getKey(), attr.getValue());

					}

				} else if (o instanceof NodeAttribute) {

					NodeAttribute attr = (NodeAttribute) o;

					attrs.put(attr.getKey(), attr.getValue());

				}

			}

			// Determine node type
			Object typeObject = attrs.get(AbstractNode.Key.type.name());
			String nodeType   = (typeObject != null)
					    ? typeObject.toString()
					    : "GenericNode";

			NodeFactory nodeFactory = new NodeFactory(SecurityContext.getSuperUserInstance());

			
			// Create node with type
			node = nodeFactory.createNodeWithType(graphDb.createNode(), nodeType);
			if(node != null) {
				if ((user != null) && !(user instanceof SuperUser)) {

					node.setOwner(user);
	//
					AbstractRelationship securityRel = (AbstractRelationship) createRel.execute(user, node, RelType.SECURITY, null, true);    // avoid duplicates

					securityRel.setAllowed(Permission.values());
					logger.log(Level.FINEST, "All permissions given to user {0}", user.getStringProperty(AbstractNode.Key.name));
					node.unlockReadOnlyPropertiesOnce();
					node.setProperty(AbstractNode.Key.createdBy.name(), user.getProperty(AbstractNode.Key.uuid), false);

				}

				node.unlockReadOnlyPropertiesOnce();
				node.setProperty(AbstractNode.Key.createdDate.name(), now, false);
				node.setProperty(AbstractNode.Key.lastModifiedDate.name(), now, false);
				logger.log(Level.FINE, "Node {0} created", node.getId());

				// set type first!!
				node.setProperty(AbstractNode.Key.type.name(), nodeType);
				attrs.remove(AbstractNode.Key.type.name());

				for (Entry<String, Object> attr : attrs.entrySet()) {

					Object value = attr.getValue();
					node.setProperty(attr.getKey(), value);

				}

				attrs.clear();
			}

		}

		if (node != null) {

			// notify node of its creation
			node.onNodeCreation();

			// iterate post creation transformations
			for (Transformation<GraphObject> transformation : EntityContext.getEntityCreationTransformations(node.getClass())) {

				transformation.apply(securityContext, node);

			}

			// allow modification listener to examine creation
//                      EntityContext.getGlobalModificationListener().graphObjectCreated(securityContext, node);
		}

		return node;
	}
}
