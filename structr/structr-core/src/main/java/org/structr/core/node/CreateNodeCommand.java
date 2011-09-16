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

import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

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
		AbstractNode node = null;
		User user         = null;

		// Default is update index when creating a new node,
		// so the node is found immediately
		boolean updateIndex = true;

		// TODO: let the AbstractNode create itself, including all necessary db properties
		// example: a HtmlSource has to be created with mimeType=text/html
		if (graphDb != null) {

			Date now                  = new Date();
			Command createRel         = Services.command(CreateRelationshipCommand.class);
			List<NodeAttribute> attrs = new LinkedList<NodeAttribute>();

			// initialize node from parameters...
			for (Object o : parameters) {

				if (o instanceof List) {
					attrs.addAll((List) o);
				} else if (o instanceof NodeAttribute) {

					NodeAttribute attr = (NodeAttribute) o;

					attrs.add(attr);

				} else if (o instanceof User) {
					user = (User) o;
				} else if (o instanceof Boolean) {
					updateIndex = (Boolean) o;
				}
			}

			// Determine node type
			String nodeType = null;

			for (NodeAttribute attr : attrs) {

				if (AbstractNode.TYPE_KEY.equals(attr.getKey())) {
					nodeType = (String) attr.getValue();
				}
			}

			// Create node with type
			node = nodeFactory.createNode(graphDb.createNode(), nodeType);
			logger.log(Level.FINE, "Node {0} created", node.getId());

			for (NodeAttribute attr : attrs) {

				// Don't update index now
				node.setProperty(attr.getKey(), attr.getValue(), false);
				logger.log(Level.FINEST, "Set node attribute {0} to {1}", new Object[] { attr.getKey(),
					attr.getValue() });
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

				securityRel.setAllowed(Arrays.asList(StructrRelationship.ALL_PERMISSIONS));
				logger.log(Level.FINEST, "All permissions given to {0}", principal.getName());
				node.setProperty(AbstractNode.CREATED_BY_KEY,
						 user.getRealName() + " (" + user.getName() + ")", false);
			}

			node.setProperty(AbstractNode.CREATED_DATE_KEY, now, false);
			node.setProperty(AbstractNode.LAST_MODIFIED_DATE_KEY, now, false);

			if (updateIndex) {

				// index the database node we just created
				Services.command(IndexNodeCommand.class).execute(node);
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
