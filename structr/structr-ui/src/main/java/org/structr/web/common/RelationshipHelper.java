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

import org.neo4j.graphdb.RelationshipType;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.node.*;
import org.structr.web.entity.Component;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Axel Morgner
 */
public class RelationshipHelper {

	public static void copyRelationships(SecurityContext securityContext, AbstractNode origNode, AbstractNode cloneNode, String resourceId, String componentId, long position)
		throws FrameworkException {

		copyIncomingRelationships(securityContext, origNode, cloneNode, resourceId, componentId, position);
		copyOutgoingRelationships(securityContext, origNode, cloneNode, resourceId, componentId, position);
	}

	public static void copyIncomingRelationships(SecurityContext securityContext, AbstractNode origNode, AbstractNode cloneNode, String resourceId, String componentId, long position)
		throws FrameworkException {

		Command createRel = Services.command(securityContext, CreateRelationshipCommand.class);

		for (AbstractRelationship in : origNode.getIncomingRelationships()) {

			AbstractNode startNode        = in.getStartNode();
			RelationshipType relType      = in.getRelType();
			AbstractRelationship newInRel = (AbstractRelationship) createRel.execute(startNode, cloneNode, relType);

			newInRel.setProperty("type", in.getStringProperty("type"));

			// only set componentId if set
			if (componentId != null) {

				newInRel.setProperty(Component.Key.componentId, componentId);

			}

			if (resourceId != null) {

				newInRel.setProperty(Component.Key.resourceId, resourceId);
				newInRel.setProperty(resourceId, position);

			}

		}
	}

	public static void copyOutgoingRelationships(SecurityContext securityContext, AbstractNode sourceNode, AbstractNode targetNode, String resourceId, String componentId, long position)
		throws FrameworkException {

		Command createRel = Services.command(securityContext, CreateRelationshipCommand.class);

		for (AbstractRelationship out : sourceNode.getOutgoingRelationships()) {

			AbstractNode endNode           = out.getEndNode();
			RelationshipType relType       = out.getRelType();
			AbstractRelationship newOutRel = (AbstractRelationship) createRel.execute(targetNode, endNode, relType);

			newOutRel.setProperty("type", out.getStringProperty("type"));

			if (componentId != null) {

				newOutRel.setProperty(Component.Key.componentId, componentId);

			}

			if (resourceId != null) {

				newOutRel.setProperty(Component.Key.resourceId, resourceId);
				newOutRel.setProperty(resourceId, position);

			}

		}
	}

	public static void removeOutgoingRelationships(SecurityContext securityContext, final AbstractNode node) throws FrameworkException {

		final Command delRel           = Services.command(securityContext, DeleteRelationshipCommand.class);
		StructrTransaction transaction = new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {

				for (AbstractRelationship out : node.getOutgoingRelationships()) {

					delRel.execute(out);

				}

				return null;
			}
		};

		Services.command(securityContext, TransactionCommand.class).execute(transaction);
	}

	public static void removeIncomingRelationships(SecurityContext securityContext, final AbstractNode node) throws FrameworkException {

		final Command delRel           = Services.command(securityContext, DeleteRelationshipCommand.class);
		StructrTransaction transaction = new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {

				for (AbstractRelationship in : node.getIncomingRelationships()) {

					delRel.execute(in);

				}

				return null;
			}
		};

		Services.command(securityContext, TransactionCommand.class).execute(transaction);
	}

	public static void moveIncomingRelationships(SecurityContext securityContext, AbstractNode origNode, AbstractNode cloneNode, String resourceId, String componentId, long position)
		throws FrameworkException {

		copyIncomingRelationships(securityContext, origNode, cloneNode, resourceId, componentId, position);
		removeIncomingRelationships(securityContext, origNode);

	}

	public static void moveOutgoingRelationships(SecurityContext securityContext, AbstractNode origNode, AbstractNode cloneNode, String resourceId, String componentId, long position)
		throws FrameworkException {

		copyOutgoingRelationships(securityContext, origNode, cloneNode, resourceId, componentId, position);
		removeOutgoingRelationships(securityContext, origNode);

	}
}
