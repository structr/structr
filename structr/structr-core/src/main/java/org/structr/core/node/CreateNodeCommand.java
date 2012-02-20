/*
 *  Copyright (C) 2011 Axel Morgner, structr <structr@structr.org>
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

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;

import org.structr.common.RelType;
import org.structr.common.error.FrameworkException;
import org.structr.core.Command;
import org.structr.core.EntityContext;
import org.structr.core.Services;
import org.structr.core.Transformation;
import org.structr.core.entity.*;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.DirectedRelation.Cardinality;
import org.structr.core.entity.Group;
import org.structr.core.entity.Principal;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.entity.SuperUser;
import org.structr.core.entity.User;

//~--- JDK imports ------------------------------------------------------------

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.core.GraphObject;

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

		GraphDatabaseService graphDb   = (GraphDatabaseService) arguments.get("graphDb");
		NodeFactory nodeFactory = (NodeFactory) arguments.get("nodeFactory");
		User user                      = securityContext.getUser();
		AbstractNode node              = null;

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

			// Create node with type
			node = nodeFactory.createNode(securityContext, graphDb.createNode(), nodeType);
			logger.log(Level.FINE, "Node {0} created", node.getId());

//                      EntityContext.getGlobalModificationListener().newNode(securityContext, node.getId());
			// set type first!!
			node.setProperty(AbstractNode.Key.type.name(), nodeType);
			attrs.remove(AbstractNode.Key.type.name());

			for (Entry<String, Object> attr : attrs.entrySet()) {

				Object value = attr.getValue();

				// ignore null values at creation
				if (value != null) {
					node.setProperty(attr.getKey(), value);
				}
			}

			attrs.clear();

			if ((user != null) &&!(user instanceof SuperUser)) {

				DirectedRelation rel = new DirectedRelation(null, RelType.OWNS,
								   Direction.OUTGOING, Cardinality.OneToMany, null);

				rel.createRelationship(securityContext, user, node);

//                              createRel.execute(user, node, RelType.OWNS, true); // avoid duplicates
				logger.log(Level.FINEST, "Relationship to owner {0} added", user.getName());

				Principal principal;
				Group group = user.getGroupNode();

				if (group != null) {
					principal = group;
				} else {
					principal = user;
				}

				AbstractRelationship securityRel = (AbstractRelationship) createRel.execute(principal,
									  node, RelType.SECURITY, true);    // avoid duplicates

				securityRel.setAllowed(AbstractRelationship.Permission.values());
				logger.log(Level.FINEST, "All permissions given to {0}", principal.getName());
				node.unlockReadOnlyPropertiesOnce();
				node.setProperty(AbstractNode.Key.createdBy.name(),

//                              user.getRealName() + " (" + user.getName() + ")", false);
				user.getProperty(AbstractNode.Key.uuid), false);
			}

			node.unlockReadOnlyPropertiesOnce();
			node.setProperty(AbstractNode.Key.createdDate.name(), now, false);
			node.setProperty(AbstractNode.Key.lastModifiedDate.name(), now, false);
		}

		if (node != null) {

			// notify node of its creation
			node.onNodeCreation();

			// iterate post creation transformations
			for (Transformation<GraphObject> transformation :
				EntityContext.getEntityCreationTransformations(node.getClass())) {
				transformation.apply(securityContext, node);
			}

			// allow modification listener to examine creation
//                      EntityContext.getGlobalModificationListener().graphObjectCreated(securityContext, node);
		}

		return node;
	}
}
