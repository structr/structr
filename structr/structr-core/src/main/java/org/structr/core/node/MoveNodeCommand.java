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
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.structr.core.entity.AbstractNode;
import org.structr.common.RelType;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.entity.SuperUser;

/**
 * Moves a node.
 *
 * @param node          the node, a Long nodeId or a String nodeId
 * @param newParentNode the new parent node, a Long nodeId or a String nodeId
 * 
 * @return the delete node's parent
 *
 * @author cmorgner
 */
public class MoveNodeCommand extends NodeServiceCommand {

    @Override
    public Object execute(Object... parameters) {

        Command findNode = Services.command(FindNodeCommand.class);

        AbstractNode node = null;
        AbstractNode newParentNode = null;
        long id = 0;
        long newParentId = 0;
        boolean isLink = false;

        switch (parameters.length) {

            case 2:

                if (parameters[0] instanceof Long) {
                    id = ((Long) parameters[0]).longValue();

                } else if (parameters[0] instanceof AbstractNode) {
                    id = ((AbstractNode) parameters[0]).getId();

                } else if (parameters[0] instanceof String) {
                    id = Long.parseLong((String) parameters[0]);
                }
                node = (AbstractNode) findNode.execute(new SuperUser(), id);

                if (parameters[1] instanceof Long) {
                    newParentId = ((Long) parameters[1]).longValue();

                } else if (parameters[1] instanceof AbstractNode) {
                    newParentId = ((AbstractNode) parameters[1]).getId();

                } else if (parameters[1] instanceof String) {
                    newParentId = Long.parseLong((String) parameters[1]);

                }
                newParentNode = (AbstractNode) findNode.execute(new SuperUser(), newParentId);

                break;

            case 3:

                if (parameters[0] instanceof Long) {
                    id = ((Long) parameters[0]).longValue();

                } else if (parameters[0] instanceof AbstractNode) {
                    id = ((AbstractNode) parameters[0]).getId();

                } else if (parameters[0] instanceof String) {
                    id = Long.parseLong((String) parameters[0]);
                }
                node = (AbstractNode) findNode.execute(new SuperUser(), id);

                if (parameters[1] instanceof Long) {
                    newParentId = ((Long) parameters[1]).longValue();

                } else if (parameters[1] instanceof AbstractNode) {
                    newParentId = ((AbstractNode) parameters[1]).getId();

                } else if (parameters[1] instanceof String) {
                    newParentId = Long.parseLong((String) parameters[1]);

                }
                newParentNode = (AbstractNode) findNode.execute(new SuperUser(), newParentId);

                if (parameters[2] instanceof Boolean) {
                    isLink = (Boolean) parameters[2];
                }
                break;

            default:
                break;

        }

        doMoveNode(node, newParentNode, isLink);

        return null;
    }

    private void doMoveNode(final AbstractNode node, final AbstractNode newParentNode, final boolean isLink) {

        if (node != null) {

            RelationshipType relType = RelType.HAS_CHILD;

            // Special treatment for link nodes
            if (isLink) {
                relType = RelType.LINK;
            }

            // delete parent relationship
            Relationship parentRel = node.getNode().getSingleRelationship(relType, Direction.INCOMING);
            if (parentRel != null) {
                parentRel.delete();
            }

            // create relationship between new parent node and node
            newParentNode.getNode().createRelationshipTo(node.getNode(), relType);

        }

    }
}
