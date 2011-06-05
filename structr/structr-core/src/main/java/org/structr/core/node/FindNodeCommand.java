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
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.NotFoundException;
import org.neo4j.kernel.Traversal;
import org.structr.common.RelType;
import org.structr.common.TreeHelper;
import org.structr.core.UnsupportedArgumentError;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.User;
//import org.structr.common.xpath.JXPathFinder;

/**
 * Searches for a node in the database and returns the result.
 *
 * <p>
 * Note that there are two possible types of results:
 * <ul>
 * <li>a single node as a result of
 *	<ul>
 *		<li>a single Long argument which specifies the id of the desired node</li<
 *		<li>a single String that contains a numerical value that specifies the
 *			id of the desired node</li>
 *              <li>a single database Node argument
 *	</ul>
 * </li>
 * <li>a List of Nodes as a result of
 *	<ul>
 *		<li>a single String that specifies the <b>absolute</b> path to the desired
 *			node(s) in the node tree</li>
 *		<li>a single String that specifies a valid XPath expression
 *		<li>a AbstractNode and a String argument, where the AbstractNode specifies the start
 *			node of a relative XPath expression, and the String is interpreted
 *			as the XPath to the desired node(s).
 *                      If the String argument is omitted, the result contains all
 *                      subnodes of the start node recursively.
 *	</ul>
 * </li>
 * </ul>
 *
 * @author cmorgner
 */
public class FindNodeCommand extends NodeServiceCommand {

    private static final Logger logger = Logger.getLogger(FindNodeCommand.class.getName());

    @Override
    public Object execute(Object... parameters) {

        GraphDatabaseService graphDb = (GraphDatabaseService) arguments.get("graphDb");
        StructrNodeFactory nodeFactory = (StructrNodeFactory) arguments.get("nodeFactory");

        User user = null;

        if (graphDb != null) {
            switch (parameters.length) {
                case 0:
                    throw new UnsupportedArgumentError("No arguments supplied");

                case 2:
                    if (parameters[0] instanceof User) {
                        user = (User) parameters[0];
                    }
                    return (handleSingleArgument(graphDb, nodeFactory, user, parameters[1]));

                default:
                    if (parameters[0] instanceof User) {
                        user = (User) parameters[0];
                    }
                    return (handleMultipleArguments(graphDb, nodeFactory, user, parameters));
            }
        }

        return (null);
    }

    // <editor-fold defaultstate="collapsed" desc="private methods">
    private Object handleSingleArgument(GraphDatabaseService graphDb, StructrNodeFactory nodeFactory, User user, Object argument) {

        Object result = null;

        if (argument instanceof Node) {
            result = nodeFactory.createNode((Node) argument);

        } else if (argument instanceof Long) {
            // single long value: find node by id
            long id = ((Long) argument).longValue();

            Node node = null;
            try {
                node = graphDb.getNodeById(id);
                result = nodeFactory.createNode(node);
            } catch (NotFoundException nfe) {
                logger.log(Level.WARNING, "Node with id {0} not found in database!", id);
            }

        } else if (argument instanceof String) {
            // single string value, try to parse to long
            try {
                long id = Long.parseLong((String) argument);

                Node node = graphDb.getNodeById(id);
                result = nodeFactory.createNode(node);

            } catch (Exception ex) {
                // failed :(
                logger.log(Level.WARNING, "Node with id {0} not found in database! Reason: {1}", new Object[]{argument, ex.getMessage()});

                String path = (String) argument;
                Node rootNode = graphDb.getReferenceNode();

                if (path.endsWith("*")) {
                    result = TreeHelper.getNodesByPath(nodeFactory.createNode(rootNode), path, true);
                } else {
                    result = TreeHelper.getNodeByPath(nodeFactory.createNode(rootNode), path, true);
                }


            }

        } else if (argument instanceof XPath) {

            // single xpath attribute: find node by absolute XPath
            Node rootNode = graphDb.getReferenceNode();
            XPath xpath = (XPath) argument;



//            JXPathFinder nodeFinder = new JXPathFinder(nodeFactory.createNode(rootNode), user);
//            if (xpath.getXPath().startsWith("count(")) {
//
//                // getNodeAttribute calls getValue(xpath) -> supports counting nodes with count()
//                result = nodeFinder.getNodeAttribute(xpath.getXPath());
//
//                // round value by converting it to long
//                result = longValue(result);
//
//            } else {

            String path = xpath.getXPath();

            if (path.endsWith("*")) {
                result = TreeHelper.getNodesByPath(nodeFactory.createNode(rootNode), path, true);
            } else {
                result = TreeHelper.getNodeByPath(nodeFactory.createNode(rootNode), path, true);
            }

            //result = nodeFinder.findNodes(xpath, nodeFactory);
//            }

        } else if (argument instanceof ReferenceNode) {

            // return reference node
            Node node = graphDb.getReferenceNode();
            result = nodeFactory.createNode(node);

        } else if (argument instanceof NodeAttribute) {
            // single node attribute: find node by attribute..
            throw new UnsupportedOperationException("Not supported yet, use SearchNodeCommand instead!");

        } else if (argument instanceof AbstractNode) {

            AbstractNode startNode = (AbstractNode) argument;
            Node s = null;
            if (startNode == null) {
                s = graphDb.getReferenceNode();

            } else {
                s = graphDb.getNodeById(startNode.getId());
            }

            // complete node tree
            result = nodeFactory.createNodes(Traversal.description().breadthFirst().relationships(RelType.HAS_CHILD, Direction.OUTGOING).traverse(s).nodes());

        }

        return result;
    }

    private Object handleMultipleArguments(GraphDatabaseService graphDb, StructrNodeFactory nodeFactory, User user, Object[] parameters) {
        // at this point, we're sure there are at least 2 elements in the array
        // (so, no check here :))

        Object result = null;

        // omit first parameter (is user)
        if (parameters[1] instanceof AbstractNode && (parameters[2] instanceof XPath || parameters[2] instanceof String)) {
            // relative xpath expression
            AbstractNode currentNode = (AbstractNode) parameters[1];

            String path;
            if (parameters[2] instanceof XPath) {
                path = ((XPath) parameters[2]).getXPath();
            } else {
                path = (String) parameters[2];
            }

            try {
                long id = Long.parseLong(path);

                Node node = graphDb.getNodeById(id);
                return nodeFactory.createNode(node);

            } catch (Exception ex) {
                // string is not an ID
                logger.log(Level.FINE, "Node with id {0} not found in database! Reason: {1}", new Object[]{path, ex.getMessage()});
            }
            
            if (path.startsWith("/")) {
                currentNode = nodeFactory.createNode(graphDb.getReferenceNode());
            }

            if (path.endsWith("*")) {
                result = TreeHelper.getNodesByPath(currentNode, path, true);
            } else {
                result = TreeHelper.getNodeByPath(currentNode, path, true);
            }
//
//
//            JXPathFinder nodeFinder = new JXPathFinder(currentNode, user);
//            if (xpath.getXPath().startsWith("count(")) {
//
//                // getNodeAttribute calls getValue(xpath) -> supports counting nodes with count()
//                ret = nodeFinder.getNodeAttribute(xpath.getXPath());
//
//                // round value by converting it to long
//                ret = longValue(ret);
//
//            } else {
//
//                ret = nodeFinder.findNodes(xpath, nodeFactory);
//            }

        }

        return result;
    }

    /**
     * Round value by converting it to long
     * 
     * @param value a (@see Number)
     * @return
     */
    private long longValue(final Object value) {
        // return rounded value
        if (value != null && value instanceof Number) {
            return ((Number) value).longValue();
        } else {
            return -1L;
        }
    }
    // </editor-fold>
}
