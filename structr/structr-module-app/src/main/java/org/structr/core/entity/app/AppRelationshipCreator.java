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



package org.structr.core.entity.app;

import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;

import org.structr.common.PropertyKey;
import org.structr.common.PropertyView;
import org.structr.common.StructrOutputStream;
import org.structr.core.EntityContext;
import org.structr.core.NodeSource;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.StructrRelationship;
import org.structr.core.node.StructrTransaction;
import org.structr.core.node.TransactionCommand;

//~--- JDK imports ------------------------------------------------------------

import java.util.LinkedHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.List;
import java.util.Map;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Christian Morgner
 */
public class AppRelationshipCreator extends ActionNode {

	private static final Logger logger = Logger.getLogger(AppRelationshipCreator.class.getName());

	//~--- static initializers --------------------------------------------

	static {

		EntityContext.registerPropertySet(AppRelationshipCreator.class,
						  PropertyView.All,
						  Key.values());
	}

	//~--- constant enums -------------------------------------------------

	public enum Key implements PropertyKey{ targetRelType; }

	//~--- methods --------------------------------------------------------

	@Override
	public boolean doAction(final StructrOutputStream out, final AbstractNode startNode, final String editUrl,
				final Long editNodeId) {

		String relType            = getStringProperty(Key.targetRelType.name());
		AbstractNode relStartNode = getNodeFromNamedSource(out.getRequest(),
			"startNode");
		AbstractNode relEndNode   = getNodeFromNamedSource(out.getRequest(),
			"endNode");

		if (relType == null) {

			logger.log(Level.WARNING,
				   "AppRelationshipCreator needs {0} property",
				   Key.targetRelType.name());

			return (false);
		}

		if (relStartNode == null) {

			logger.log(Level.WARNING,
				   "AppRelationshipCreator needs startNode");

			return (false);
		}

		if (relEndNode == null) {

			logger.log(Level.WARNING,
				   "AppRelationshipCreator needs endNode");

			return (false);
		}

		if (relStartNode.getId() == relEndNode.getId()) {

			logger.log(Level.WARNING,
				   "AppRelationshipCreator can not operate on a single node (start == end!)");

			return (false);
		}

		logger.log(Level.INFO,
			   "All checks passed, creating relationship {0}",
			   relType);

		final Node fromNode               = relStartNode.getNode();
		final Node toNode                 = relEndNode.getNode();
		final RelationshipType newRelType = DynamicRelationshipType.withName(relType);

		Services.command(securityContext,
				 TransactionCommand.class).execute(new StructrTransaction() {

			@Override
			public Object execute() throws Throwable {

				try {

					fromNode.createRelationshipTo(toNode,
								      newRelType);

				} catch (Throwable t) {

					logger.log(Level.WARNING,
						   "Error creating relationship: {0}",
						   t);
				}

				return (null);
			}

		});

		/*
		 * <<<<<<< HEAD
		 * Services.command(securityContext, CreateRelationshipCommand.class).execute(relStartNode, relEndNode, relType);
		 * =======
		 * Services.command(CreateRelationshipCommand.class).execute(relStartNode, relEndNode, relType);
		 * >>>>>>> 0f55394c125ecab035924262c7b0c1fb27248885
		 */
		return (true);
	}

	//~--- get methods ----------------------------------------------------

	@Override
	public Map<String, Slot> getSlots() {

		Map<String, Slot> ret = new LinkedHashMap<String, Slot>();

		/*
		 * ret.put("startNode", new StringSlot());
		 * ret.put("endNode", new StringSlot());
		 */
		return (ret);
	}

	@Override
	public String getIconSrc() {
		return ("/images/brick_link.png");
	}

	// ----- private methods -----
	private AbstractNode getNodeFromNamedSource(HttpServletRequest request, String name) {

		List<StructrRelationship> rels = getIncomingDataRelationships();
		AbstractNode ret               = null;

		for (StructrRelationship rel : rels) {

			AbstractNode node = rel.getStartNode();

			if (node instanceof NodeSource) {

				if (rel.getRelationship().hasProperty(ActionNode.Key.targetSlotName.name())) {

					String targetSlot = (String) rel.getRelationship().getProperty(
								ActionNode.Key.targetSlotName.name());

					if (name.equals(targetSlot)) {

						NodeSource source = (NodeSource) node;

						ret = source.loadNode(request);

						break;
					}
				}
			}
		}

		return (ret);
	}
}
