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

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.kernel.Traversal;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.common.RelType;
import org.structr.core.entity.StructrRelationship;
import org.structr.core.entity.User;
import org.structr.core.notification.AddNotificationCommand;
import org.structr.core.notification.DefaultNotification;

/**
 * Deletes a node, or removes a LINK relationship respectively.
 *
 * @param node the node, a Long nodeId or a String nodeId
 * 
 * @return the deleted node's parent
 *
 * @author cmorgner
 */
public class DeleteNodeCommand extends NodeServiceCommand {

    private static final Logger logger = Logger.getLogger(DeleteNodeCommand.class.getName());

    @Override
    public Object execute(Object... parameters) {
        AbstractNode node = null;
        AbstractNode parentNode = null;
        Boolean recursive = false;
        User user = null;

        Command findNode = Services.command(FindNodeCommand.class);

        switch (parameters.length) {

            case 2:  // first parameter: node, second parameter: user

                if (parameters[0] instanceof Long) {
                    long id = ((Long) parameters[0]).longValue();
                    node = (AbstractNode) findNode.execute(user, id);

                } else if (parameters[0] instanceof AbstractNode) {
                    node = ((AbstractNode) parameters[0]);

                } else if (parameters[0] instanceof String) {
                    long id = Long.parseLong((String) parameters[0]);
                    node = (AbstractNode) findNode.execute(user, id);
                }

                if (parameters[1] instanceof User) {
                    user = (User) parameters[1];
                }

                break;

            case 3: // first parameter: node, second parameter: new parent node, third parameter: user

                if (parameters[0] instanceof Long) {
                    long id = ((Long) parameters[0]).longValue();
                    node = (AbstractNode) findNode.execute(user, id);

                } else if (parameters[0] instanceof AbstractNode) {
                    node = ((AbstractNode) parameters[0]);

                } else if (parameters[0] instanceof String) {
                    long id = Long.parseLong((String) parameters[0]);
                    node = (AbstractNode) findNode.execute(user, id);
                }

                if (parameters[1] instanceof Long) {
                    long id = ((Long) parameters[1]).longValue();
                    parentNode = (AbstractNode) findNode.execute(user, id);

                } else if (parameters[1] instanceof AbstractNode) {
                    parentNode = (AbstractNode) parameters[1];

                } else if (parameters[1] instanceof String) {
                    long id = Long.parseLong((String) parameters[1]);
                    parentNode = (AbstractNode) findNode.execute(user, id);
                }

                if (parameters[2] instanceof User) {
                    user = (User) parameters[2];
                }

                break;

            case 4: // first parameter: node, second parameter: new parent node, third parameter: recursive, forth parameter: user

                if (parameters[0] instanceof Long) {
                    long id = ((Long) parameters[0]).longValue();
                    node = (AbstractNode) findNode.execute(user, id);

                } else if (parameters[0] instanceof AbstractNode) {
                    node = ((AbstractNode) parameters[0]);

                } else if (parameters[0] instanceof String) {
                    long id = Long.parseLong((String) parameters[0]);
                    node = (AbstractNode) findNode.execute(user, id);
                }

                if (parameters[1] instanceof Long) {
                    long id = ((Long) parameters[1]).longValue();
                    parentNode = (AbstractNode) findNode.execute(user, id);

                } else if (parameters[1] instanceof AbstractNode) {
                    parentNode = (AbstractNode) parameters[1];

                } else if (parameters[1] instanceof String) {
                    long id = Long.parseLong((String) parameters[1]);
                    parentNode = (AbstractNode) findNode.execute(user, id);
                }

                if (parameters[2] instanceof String) {
                    recursive = Boolean.parseBoolean((String) parameters[2]);
                } else if (parameters[2] instanceof Boolean) {
                    recursive = (Boolean) parameters[2];
                }

                if (parameters[3] instanceof User) {
                    user = (User) parameters[3];
                }

                break;

            default:
                break;

        }

        if (node == null) {
            setExitCode(Command.exitCode.FAILURE);
            setErrorMessage("Could not delete node null");
            logger.log(Level.WARNING, getErrorMessage());
            return null;
        }


        if (node.getId() == 0) {
            setExitCode(Command.exitCode.FAILURE);
            setErrorMessage("Deleting the root node is not allowed.");
            logger.log(Level.WARNING, getErrorMessage());
            return null;
        }

        return doDeleteNode(user, node, parentNode, recursive);
    }

    private AbstractNode doDeleteNode(final User user, final AbstractNode structrNode, final AbstractNode parentNode, final Boolean recursive) {

        GraphDatabaseService graphDb = (GraphDatabaseService) arguments.get("graphDb");


        final Node node = graphDb.getNodeById(structrNode.getId());


        final Command transactionCommand = Services.command(TransactionCommand.class);
        AbstractNode newParentNode = (AbstractNode) transactionCommand.execute(new StructrTransaction() {

            @Override
            public Object execute() throws Throwable {

                AbstractNode newParentNode = null;

                Command findNode = Services.command(FindNodeCommand.class);
                if (parentNode == null) {

                    if (recursive) {

                        Relationship parentRel = node.getSingleRelationship(RelType.HAS_CHILD, Direction.INCOMING);
                        if (parentRel != null) {
                            newParentNode = (AbstractNode) findNode.execute(user, parentRel.getStartNode().getId());
                            parentRel.delete();
                        }

                        //newParentNode = structrNode.getParentNode(user);

                        // delete HAS_CHILD relationship to parent node

                        // iterate over all child nodes
                        for (Path p : Traversal.description().breadthFirst().relationships(RelType.HAS_CHILD, Direction.OUTGOING).traverse(node)) {

//                        Relationship r = p.lastRelationship();
                            Node n = p.endNode();

                            // delete any outgoing relationships
                            for (Relationship l : n.getRelationships(Direction.OUTGOING)) {
                                try {
                                    l.delete();
                                } catch (IllegalStateException ise) {
                                    // relationship was already deleted
                                    logger.log(Level.WARNING, "Relationship {0} already deleted in this transaction", l.getId());
                                }
                            }

                            // delete any incoming relationships
                            for (Relationship l : n.getRelationships(Direction.INCOMING)) {
                                try {
                                    l.delete();
                                } catch (IllegalStateException ise) {
                                    // relationship was already deleted
                                    logger.log(Level.WARNING, "Relationship {0} already deleted in this transaction", l.getId());
                                }
                            }

                            // delete the HAS_CHILD relationship of this child node
                            //if (r != null) r.delete();

                            // deletion callback, must not prevent node deletion!
                            try {
                                ((AbstractNode) findNode.execute(user, n.getId())).onNodeDeletion();

                            } catch (Throwable t) {
                                logger.log(Level.WARNING, "Exception while calling onDeletion callback: {0}", t);
                            }

                            // delete the child node itself
                            n.delete();
                        }

                    } else {

                        // check if node has subnodes
                        if (structrNode.hasChildren()) {
                            setExitCode(Command.exitCode.FAILURE);
                            setErrorMessage("Could not delete node " + node.getId() + " because it has still child nodes");
                            logger.log(Level.WARNING, getErrorMessage());
                            return null;
                        }

                        Relationship parentRel = node.getSingleRelationship(RelType.HAS_CHILD, Direction.INCOMING);

                        // it is possible that a node has no parent (= incoming child) relationship, e.g. thumbnails
                        if (parentRel != null) {
                            //newParentNode = (AbstractNode) findNode.execute(user, parentRel.getStartNode().getId());
                            newParentNode = structrNode.getParentNode();
                            parentRel.delete();
                        }
                        // delete other incoming relationships
                        List<StructrRelationship> incomingRels = structrNode.getIncomingRelationships();
                        for (StructrRelationship r : incomingRels) {
                            r.getRelationship().delete();
                        }

                        // deletion callback, must not prevent node deletion!
                        try {
                            structrNode.onNodeDeletion();

                        } catch (Throwable t) {
                            logger.log(Level.WARNING, "Exception while calling onDeletion callback: {0}", t);
                        }

                        // delete node
                        node.delete();
                    }

                } else {

                    for (Relationship r : node.getRelationships(RelType.LINK, Direction.INCOMING)) {

                        AbstractNode p = (AbstractNode) findNode.execute(user, r.getStartNode().getId());
                        if (p.equals(parentNode)) {
                            r.delete();
                        }
                    }

                    newParentNode = parentNode;

                    return newParentNode;

                }

                return newParentNode;
            }
        });

        //setExitCode(Command.exitCode.SUCCESS);
        return newParentNode;
    }
}
