/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.node;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.entity.StructrNode;
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
        StructrNode node = null;
        StructrNode targetNode = null;
        User user = null;

        Command findNode = Services.command(FindNodeCommand.class);

        switch (parameters.length) {
            case 3:

                if (parameters[0] instanceof Long) {
                    long id = ((Long) parameters[0]).longValue();
                    node = (StructrNode) findNode.execute(user, id);

                } else if (parameters[0] instanceof StructrNode) {
                    node = (StructrNode) parameters[0];

                } else if (parameters[0] instanceof String) {
                    long id = Long.parseLong((String) parameters[0]);
                    node = (StructrNode) findNode.execute(user, id);
                }

                if (parameters[1] instanceof Long) {
                    long id = ((Long) parameters[1]).longValue();
                    targetNode = (StructrNode) findNode.execute(user, id);

                } else if (parameters[1] instanceof StructrNode) {
                    targetNode = (StructrNode) parameters[1];

                } else if (parameters[1] instanceof String) {
                    long id = Long.parseLong((String) parameters[1]);
                    targetNode = (StructrNode) findNode.execute(user, id);
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

    private StructrNode doCopyNode(StructrNode node, StructrNode targetNode, User user) {

        if (node != null) {

            Command createNode = Services.command(CreateNodeCommand.class);
            Command createRel = Services.command(CreateRelationshipCommand.class);

            StructrNode newNode = (StructrNode) createNode.execute(user);

            // copy properties
            for (String key : node.getPropertyKeys()) {

                // don't copy creation date
                if (!(key.equals(StructrNode.CREATED_DATE_KEY))) {
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
