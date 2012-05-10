/*
 *  Copyright (C) 2012 Axel Morgner
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



package org.structr.web.common;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.RelationshipType;

import org.structr.common.RelType;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.entity.Folder;
import org.structr.core.node.*;
import org.structr.web.entity.Component;
import org.structr.web.entity.Group;

//~--- JDK imports ------------------------------------------------------------

import java.util.HashSet;
import java.util.List;
import java.util.Set;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Axel Morgner
 */
public class RelationshipHelper {

	public static void copyRelationships(SecurityContext securityContext, AbstractNode origNode, AbstractNode cloneNode, RelType relType, String resourceId, String componentId, long position)
		throws FrameworkException {

		copyIncomingRelationships(securityContext, origNode, cloneNode, relType, resourceId, componentId, position);
		copyOutgoingRelationships(securityContext, origNode, cloneNode, relType, resourceId, componentId, position);
	}

	public static void copyIncomingRelationships(SecurityContext securityContext, AbstractNode origNode, AbstractNode cloneNode, RelType relType, String resourceId, String componentId,
		long position)
		throws FrameworkException {

		Command createRel = Services.command(securityContext, CreateRelationshipCommand.class);

		for (AbstractRelationship in : origNode.getIncomingRelationships()) {

			AbstractNode startNode       = in.getStartNode();
			RelationshipType origRelType = in.getRelType();

			if (!(relType.name().equals(origRelType.name()))) {

				continue;

			}

			AbstractRelationship newInRel = (AbstractRelationship) createRel.execute(startNode, cloneNode, relType, in.getProperties());

			// only set componentId if set and avoid setting the component id of the clone node itself
			if ((componentId != null) &&!(cloneNode.getStringProperty(AbstractNode.Key.uuid).equals(componentId))) {

				newInRel.setProperty(Component.Key.componentId, componentId);

			}

			if (resourceId != null) {

				newInRel.setProperty(Component.Key.resourceId, resourceId);
				newInRel.setProperty(resourceId, position);

			}

		}
	}

	public static void copyOutgoingRelationships(SecurityContext securityContext, AbstractNode sourceNode, AbstractNode cloneNode, RelType relType, String resourceId, String componentId,
		long position)
		throws FrameworkException {

		Command createRel = Services.command(securityContext, CreateRelationshipCommand.class);

		for (AbstractRelationship out : sourceNode.getOutgoingRelationships()) {

			AbstractNode endNode         = out.getEndNode();
			RelationshipType origRelType = out.getRelType();

			if (!relType.name().equals(origRelType.name())) {

				continue;

			}

			AbstractRelationship newOutRel = (AbstractRelationship) createRel.execute(cloneNode, endNode, relType, out.getProperties());

			if (componentId != null) {

				newOutRel.setProperty(Component.Key.componentId, componentId);

			}

			if (resourceId != null) {

				newOutRel.setProperty(Component.Key.resourceId, resourceId);
				newOutRel.setProperty(resourceId, position);

			}

		}
	}

	public static void removeOutgoingRelationships(SecurityContext securityContext, final AbstractNode node, final RelType relType) throws FrameworkException {

		final Command delRel           = Services.command(securityContext, DeleteRelationshipCommand.class);
		StructrTransaction transaction = new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {

				for (AbstractRelationship out : node.getOutgoingRelationships()) {

					RelationshipType origRelType = out.getRelType();

					if (!relType.name().equals(origRelType.name())) {

						continue;

					}

					delRel.execute(out);

				}

				return null;
			}
		};

		Services.command(securityContext, TransactionCommand.class).execute(transaction);
	}

	public static void removeIncomingRelationships(SecurityContext securityContext, final AbstractNode node, final RelType relType) throws FrameworkException {

		final Command delRel           = Services.command(securityContext, DeleteRelationshipCommand.class);
		StructrTransaction transaction = new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {

				for (AbstractRelationship in : node.getIncomingRelationships()) {

					RelationshipType origRelType = in.getRelType();

					if (!relType.name().equals(origRelType.name())) {

						continue;

					}

					delRel.execute(in);

				}

				return null;
			}
		};

		Services.command(securityContext, TransactionCommand.class).execute(transaction);
	}

	public static void moveIncomingRelationships(SecurityContext securityContext, AbstractNode origNode, AbstractNode cloneNode, final RelType relType, String resourceId, String componentId,
		long position)
		throws FrameworkException {

		copyIncomingRelationships(securityContext, origNode, cloneNode, relType, resourceId, componentId, position);
		removeIncomingRelationships(securityContext, origNode, relType);
	}

	public static void moveOutgoingRelationships(SecurityContext securityContext, AbstractNode origNode, AbstractNode cloneNode, final RelType relType, String resourceId, String componentId,
		long position)
		throws FrameworkException {

		copyOutgoingRelationships(securityContext, origNode, cloneNode, relType, resourceId, componentId, position);
		removeOutgoingRelationships(securityContext, origNode, relType);
	}

	public static void tagOutgoingRelsWithResourceId(final AbstractNode startNode, final AbstractNode node, final String originalResourceId, final String resourceId) throws FrameworkException {

		for (AbstractRelationship rel : node.getRelationships(RelType.CONTAINS, Direction.OUTGOING)) {

			Long position = rel.getLongProperty(originalResourceId);

			if (position != null) {

				rel.setProperty(resourceId, position);

			}

			tagOutgoingRelsWithResourceId(startNode, rel.getEndNode(), originalResourceId, resourceId);

		}
	}

	public static void tagOutgoingRelsWithComponentId(final AbstractNode startNode, final AbstractNode node, final String componentId) throws FrameworkException {

		for (AbstractRelationship rel : node.getRelationships(RelType.CONTAINS, Direction.OUTGOING)) {

			if (!(startNode.equals(node))) {

				rel.setProperty("componentId", componentId);

				if (node.getType().equals(Component.class.getSimpleName())) {

					return;

				}

			}

			tagOutgoingRelsWithComponentId(startNode, rel.getEndNode(), componentId);

		}
	}

	//~--- get methods ----------------------------------------------------

	public static Set<String> getChildrenInResource(final AbstractNode parentNode, final String resourceId) {

		Set<String> nodesWithChildren        = new HashSet<String>();
		List<AbstractRelationship> childRels = parentNode.getOutgoingRelationships(RelType.CONTAINS);

		for (AbstractRelationship childRel : childRels) {

			String parentId = parentNode.getUuid();

			if (resourceId == null || (parentNode instanceof Group) || (parentNode instanceof Folder)) {

				nodesWithChildren.add(parentId);

			}

			Long childPos = null;

			if (childRel.getLongProperty(resourceId) != null) {

				childPos = childRel.getLongProperty(resourceId);

			} else {

				// Try "*"
				childPos = childRel.getLongProperty("*");
			}

			if (childPos != null) {

				nodesWithChildren.add(parentId);

			}

		}

		return nodesWithChildren;
	}

	public static boolean hasChildren(final AbstractNode node, final String resourceId) {

		List<AbstractRelationship> childRels = node.getOutgoingRelationships(RelType.CONTAINS);

		if ((node instanceof Group) || (node instanceof Folder)) {

			return !childRels.isEmpty();

		}

		for (AbstractRelationship childRel : childRels) {

			Long childPos = null;

			if (childRel.getLongProperty(resourceId) != null) {

				childPos = childRel.getLongProperty(resourceId);

			} else {

				// Try "*"
				childPos = childRel.getLongProperty("*");
			}

			if (childPos != null) {

				return true;

			}

		}

		return false;
	}
}
