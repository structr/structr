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

import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.common.RelType;
import org.structr.core.entity.User;

/**
 * Copies a node.
 *
 * @param node          the node, a Long nodeId or a String nodeId
 * @param targetNode the new parent node, a Long nodeId or a String nodeId
 * 
 * @author amorgner
 */
public class CopyNodeCommand extends NodeServiceCommand {

    private static final Logger logger = Logger.getLogger(CopyNodeCommand.class.getName());

    @Override
    public Object execute(Object... parameters) {
        AbstractNode node = null;
        AbstractNode targetNode = null;
        User user = null;

        Command findNode = Services.command(securityContext, FindNodeCommand.class);

        switch (parameters.length) {
            case 3:

                if (parameters[0] instanceof Long) {
                    long id = ((Long) parameters[0]).longValue();
                    node = (AbstractNode) findNode.execute(user, id);

                } else if (parameters[0] instanceof AbstractNode) {
                    node = (AbstractNode) parameters[0];

                } else if (parameters[0] instanceof String) {
                    long id = Long.parseLong((String) parameters[0]);
                    node = (AbstractNode) findNode.execute(user, id);
                }

                if (parameters[1] instanceof Long) {
                    long id = ((Long) parameters[1]).longValue();
                    targetNode = (AbstractNode) findNode.execute(user, id);

                } else if (parameters[1] instanceof AbstractNode) {
                    targetNode = (AbstractNode) parameters[1];

                } else if (parameters[1] instanceof String) {
                    long id = Long.parseLong((String) parameters[1]);
                    targetNode = (AbstractNode) findNode.execute(user, id);
                }

                if (parameters[2] instanceof User) {
                    user = (User) parameters[2];
                }

                break;

            default:
                break;

        }

        return (doCopyNode(node, targetNode, user));
    }

    private AbstractNode doCopyNode(AbstractNode node, AbstractNode targetNode, User user) {

        if (node != null) {

            Command createNode = Services.command(securityContext, CreateNodeCommand.class);
            Command createRel = Services.command(securityContext, CreateRelationshipCommand.class);

            AbstractNode newNode = (AbstractNode) createNode.execute(user);

            // copy properties
            for (String key : node.getPropertyKeys()) {

                // don't copy creation date
                if (!(key.equals(AbstractNode.Key.createdDate.name()))) {
                    newNode.setProperty(key, node.getProperty(key));
                }
            }

            // create relationship between target node and copied node
            //targetNode.getNode().createRelationshipTo(newNode, RelType.HAS_CHILD);
            createRel.execute(targetNode, newNode, RelType.HAS_CHILD);

        } else {
            logger.log(Level.WARNING, "Copy node to node {0} failed!", targetNode.getId());
        }

        return (null);
    }
}
