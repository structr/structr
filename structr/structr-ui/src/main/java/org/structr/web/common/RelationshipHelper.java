/*
 *  Copyright (C) 2010-2012 Axel Morgner, structr <structr@structr.org>
 *
 *  This file is part of structr <http://structr.org>.
 *
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */



package org.structr.web.common;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.RelationshipType;

import org.structr.common.GraphObjectComparator;
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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang.StringUtils;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Axel Morgner
 */
public class RelationshipHelper {

	public static void copyRelationships(SecurityContext securityContext, AbstractNode origNode, AbstractNode cloneNode, RelType relType, String componentId)
		throws FrameworkException {

		copyIncomingRelationships(securityContext, origNode, cloneNode, relType, componentId);
		copyOutgoingRelationships(securityContext, origNode, cloneNode, relType, componentId);
	}

	public static void copyIncomingRelationships(SecurityContext securityContext, AbstractNode origNode, AbstractNode cloneNode, RelType relType, String componentId)
		throws FrameworkException {

		if (cloneNode == null) {

			return;

		}

		Command createRel = Services.command(securityContext, CreateRelationshipCommand.class);

		for (AbstractRelationship in : origNode.getIncomingRelationships()) {

			AbstractNode startNode = in.getStartNode();

			if (startNode == null) {

				continue;

			}

			RelationshipType origRelType = in.getRelType();

			if (!(relType.name().equals(origRelType.name()))) {

				continue;

			}

			Map<String, Object> props = in.getProperties();
			props.remove(AbstractRelationship.Key.uuid.name());
			
			AbstractRelationship newInRel = (AbstractRelationship) createRel.execute(startNode, cloneNode, relType, props, false);

			// only set componentId if set and avoid setting the component id of the clone node itself
			if ((componentId != null) &&!(cloneNode.getStringProperty(AbstractNode.Key.uuid).equals(componentId))) {

				newInRel.setProperty(Component.Key.componentId, componentId);

			}

			setPositions(cloneNode, newInRel);
			
		}
	}

	private static void setPositions(final AbstractNode cloneNode, AbstractRelationship rel) throws FrameworkException {
		
			Set<String> paths = (Set<String>) cloneNode.getProperty(Component.UiKey.paths);
			
			for (String path : paths) {
				
				String pageId = path.substring(0, 32);
				Long position	= Long.parseLong(StringUtils.substringAfterLast(path, "_"));
				
				rel.setProperty(pageId, position + 1);
				
			}
		
	}
	
	public static void copyOutgoingRelationships(SecurityContext securityContext, AbstractNode sourceNode, AbstractNode cloneNode, RelType relType, String componentId)
		throws FrameworkException {

		Command createRel = Services.command(securityContext, CreateRelationshipCommand.class);

		for (AbstractRelationship out : sourceNode.getOutgoingRelationships()) {

			AbstractNode endNode         = out.getEndNode();
			RelationshipType origRelType = out.getRelType();

			if (!relType.name().equals(origRelType.name())) {

				continue;

			}
			
			Map<String, Object> props = out.getProperties();
			props.remove(AbstractRelationship.Key.uuid.name());

			AbstractRelationship newOutRel = (AbstractRelationship) createRel.execute(cloneNode, endNode, relType, props, false);

			if (componentId != null) {

				newOutRel.setProperty(Component.Key.componentId, componentId);

			}

			setPositions(cloneNode, newOutRel);

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

	public static void moveIncomingRelationships(SecurityContext securityContext, AbstractNode origNode, AbstractNode cloneNode, final RelType relType, String pageId, String componentId,
		long position)
		throws FrameworkException {

		copyIncomingRelationships(securityContext, origNode, cloneNode, relType, componentId);
		removeIncomingRelationships(securityContext, origNode, relType);
	}

	public static void moveOutgoingRelationships(SecurityContext securityContext, AbstractNode origNode, AbstractNode cloneNode, final RelType relType, String pageId, String componentId,
		long position)
		throws FrameworkException {

		copyOutgoingRelationships(securityContext, origNode, cloneNode, relType, componentId);
		removeOutgoingRelationships(securityContext, origNode, relType);
	}

	public static void tagOutgoingRelsWithPageId(final AbstractNode startNode, final AbstractNode node, final String originalPageId, final String pageId) throws FrameworkException {

		for (AbstractRelationship rel : node.getRelationships(RelType.CONTAINS, Direction.OUTGOING)) {

			Long position = rel.getLongProperty(originalPageId);

			if (position != null) {

				rel.setProperty(pageId, position);

			}

			tagOutgoingRelsWithPageId(startNode, rel.getEndNode(), originalPageId, pageId);

		}
	}

	public static void untagOutgoingRelsFromPageId(final AbstractNode startNode, final AbstractNode node, final String startPageId, final String pageId) throws FrameworkException {

		for (AbstractRelationship rel : node.getRelationships(RelType.CONTAINS, Direction.OUTGOING)) {

			Long position = rel.getLongProperty(startPageId);

			if (position != null) {

				rel.removeProperty(pageId);

			}

			untagOutgoingRelsFromPageId(startNode, rel.getEndNode(), startPageId, pageId);

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

	public static void reorderRels(final List<AbstractRelationship> rels, final String pageId) throws FrameworkException {

		if (pageId == null) {

			return;

		}

		long i = 0;

		Collections.sort(rels, new GraphObjectComparator(pageId, GraphObjectComparator.ASCENDING));

		for (AbstractRelationship rel : rels) {

			try {

				rel.setProperty(pageId, i);

				i++;

			} catch (IllegalStateException ise) {

				// Silently ignore this exception and continue, omitting deleted rels
				continue;
			}

		}
	}

	//~--- get methods ----------------------------------------------------

	public static Set<String> getChildrenInPage(final AbstractNode parentNode, final String pageId) {

		Set<String> nodesWithChildren        = new HashSet<String>();
		List<AbstractRelationship> childRels = parentNode.getOutgoingRelationships(RelType.CONTAINS);

		for (AbstractRelationship childRel : childRels) {

			String parentId = parentNode.getUuid();

			if ((pageId == null) || (parentNode instanceof Group) || (parentNode instanceof Folder)) {

				nodesWithChildren.add(parentId);

			}

			Long childPos = null;

			if (childRel.getLongProperty(pageId) != null) {

				childPos = childRel.getLongProperty(pageId);

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

	public static boolean hasChildren(final AbstractNode node, final String pageId) {

		List<AbstractRelationship> childRels = node.getOutgoingRelationships(RelType.CONTAINS);

		if ((node instanceof Group) || (node instanceof Folder)) {

			return !childRels.isEmpty();

		}

		for (AbstractRelationship childRel : childRels) {

			Long childPos = null;

			if (childRel.getLongProperty(pageId) != null) {

				childPos = childRel.getLongProperty(pageId);

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
