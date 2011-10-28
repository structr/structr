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

import java.util.Collection;
import org.neo4j.graphdb.GraphDatabaseService;

import org.structr.common.RelType;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Group;
import org.structr.core.entity.Principal;
import org.structr.core.entity.StructrRelationship;
import org.structr.core.entity.SuperUser;
import org.structr.core.entity.User;

//~--- JDK imports ------------------------------------------------------------

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.ErrorBuffer;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author cmorgner
 */
public class CreateNodeCommand extends NodeServiceCommand {

	private static final Logger logger = Logger.getLogger(CreateNodeCommand.class.getName());

	//~--- methods --------------------------------------------------------

	@Override
	public Object execute(Object... parameters) {

		GraphDatabaseService graphDb   = (GraphDatabaseService) arguments.get("graphDb");
		StructrNodeFactory nodeFactory = (StructrNodeFactory) arguments.get("nodeFactory");

//              IndexService index = (LuceneFulltextIndexService) arguments.get("index");
		User user         = securityContext.getUser();
		AbstractNode node = null;

		// Default is update index when creating a new node,
		// so the node is found immediately
		boolean updateIndex = true;

		// TODO: let the AbstractNode create itself, including all necessary db properties
		// example: a HtmlSource has to be created with mimeType=text/html
		if (graphDb != null) {

			Date now                  = new Date();
			Command createRel         = Services.command(securityContext, CreateRelationshipCommand.class);
			Map<String, Object> attrs = new HashMap<String, Object>();

			// initialize node from parameters...
			for (Object o : parameters) {

				if(o instanceof Map) {

					Map<String, Object> map = (Map<String, Object>)o;
					attrs.putAll(map);

				} else if(o instanceof Collection) {

					Collection<NodeAttribute> c = (Collection)o;
					for(NodeAttribute attr : c) {
						attrs.put(attr.getKey(), attr.getValue());
					}

				} else if(o instanceof NodeAttribute) {

					NodeAttribute attr = (NodeAttribute)o;
					attrs.put(attr.getKey(), attr.getValue());

				} else if(o instanceof Boolean) {
					updateIndex = (Boolean)o;
				}
			}

			// Determine node type
			Object typeObject = attrs.get(AbstractNode.Key.type.name());
			String nodeType = typeObject != null ? typeObject.toString() : "GenericNode";

			// Create node with type
			node = nodeFactory.createNode(securityContext, graphDb.createNode(), nodeType);
			logger.log(Level.FINE, "Node {0} created", node.getId());

			ErrorBuffer errorBuffer = new ErrorBuffer();
			for(Entry<String, Object> attr : attrs.entrySet()) {

				try {
					node.setProperty(attr.getKey(), attr.getValue());

				} catch(Throwable t) {
					errorBuffer.add(t.getMessage());
				}
			}

			if(errorBuffer.hasError()) {
				throw new IllegalArgumentException(errorBuffer.toString());
			}

			attrs.clear();

			if ((user != null) &&!(user instanceof SuperUser)) {

				createRel.execute(user, node, RelType.OWNS);
				logger.log(Level.FINEST, "Relationship to owner {0} added", user.getName());

				Principal principal;
				Group group = user.getGroupNode();

				if (group != null) {
					principal = group;
				} else {
					principal = user;
				}

				StructrRelationship securityRel = (StructrRelationship) createRel.execute(principal,
									  node, RelType.SECURITY);

				securityRel.setAllowed(StructrRelationship.Permission.values());
				logger.log(Level.FINEST, "All permissions given to {0}", principal.getName());
				node.setProperty(AbstractNode.Key.createdBy.name(),
						 user.getRealName() + " (" + user.getName() + ")", false);
			}

			node.setProperty(AbstractNode.Key.createdDate.name(), now, false);
			node.setProperty(AbstractNode.Key.lastModifiedDate.name(), now, false);

			if (updateIndex) {

				// index the database node we just created
				Services.command(securityContext, IndexNodeCommand.class).execute(node);
				logger.log(Level.FINE, "Node {0} indexed.", node.getId());
			}
		}

		if (node != null) {

			// notify node of its creation
			node.onNodeCreation();
		}

		return node;
	}
}
