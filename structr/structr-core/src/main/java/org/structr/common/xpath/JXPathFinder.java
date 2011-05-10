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
package org.structr.common.xpath;

import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.node.NodeFactoryCommand;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.jxpath.JXPathContext;
import org.apache.commons.jxpath.JXPathException;
import org.apache.commons.jxpath.JXPathInvalidSyntaxException;
import org.apache.commons.jxpath.Pointer;
import org.apache.commons.jxpath.ri.JXPathContextReferenceImpl;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.structr.common.NodePositionComparator;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.User;
import org.structr.core.node.GraphDatabaseCommand;
import org.structr.core.node.StructrNodeFactory;
import org.structr.core.node.XPath;

/**
 *
 * @author cmorgner
 */
public class JXPathFinder {

    private GraphDatabaseService db = null;
    private AbstractNode currentNode = null;
    private static final Logger logger = Logger.getLogger(JXPathFinder.class.getName());
    private JXPathContext context = null;

    public JXPathFinder(final AbstractNode currentNode, final User user) {
        this.currentNode = currentNode;
        //this.db = currentNode.getNode().getGraphDatabase();

        Command graphDbCommand = Services.command(GraphDatabaseCommand.class);
        this.db = (GraphDatabaseService) graphDbCommand.execute();

        if (context == null) {
            
            Node rootNode = db.getReferenceNode();

            //JXPathContextReferenceImpl.addNodePointerFactory(new NeoNodePointerFactory());
            //moved to service initialization
            context = JXPathContextReferenceImpl.newContext(rootNode);

            if (currentNode != null && !(currentNode.isRootNode())) {
                // find pointer to current node and make it the active context
                Pointer currentNodePointer = context.getPointer(currentNode.getNodeXPath(user));

                // create relative context
                context = context.getRelativeContext(currentNodePointer);
            }
        }
    }

    /**
     * Returns a single node by it's ID attribute.
     * expression.
     *
     * @param id the id of the desired node
     * @return the node with id ID or null
     * @throws JXPathExpression
     */
    public Node getNodeById(long id) throws JXPathException {
        Node ret = null;

        try {
            Node node = db.getNodeById(id);
            ret = createStructrNode(node);

        } catch (Throwable t) {

            if (t instanceof JXPathException) {
                throw new JXPathException(t);
            }

        }

        return ret;
    }

    /**
     * Returns a list of nodes as a result of the given XPath
     * expression, sorted by their position attribute.
     *
     * @param xpath the xpath expression
     * @return a list of nodes resulting from xpath, sorted by their position attribute
     * @throws JXPathException
     */
    public List<AbstractNode> findNodes(XPath xpath, StructrNodeFactory nodeFactory) throws JXPathException {
        return findNodes(xpath, new NodePositionComparator(), nodeFactory);
    }

    /**
     * Returns a list of nodes as a result of the given path,
     * sorted by their position attribute.
     *
     * @param path the path
     * @return a list of nodes resulting from xpath, sorted by their position attribute
     * @throws JXPathException
     */
    public List<AbstractNode> findNodes(String path, StructrNodeFactory nodeFactory) throws JXPathException {
        XPath xpath = new XPath();
        // converts a path into an XPath expression
        xpath.setPath(path);
        return findNodes(xpath, nodeFactory);
    }

    /**
     * Returns a sorted list of nodes as a result of the given XPath
     * expression, sorted by <code>comparator</code>.
     *
     * @param xpath the xpath expression
     * @param comparator the comparator
     * @return a list of nodes resulting from xpath, sorted by comparator
     * @throws JXPathException
     */
    public List<AbstractNode> findNodes(XPath xpath, Comparator<AbstractNode> comparator, StructrNodeFactory nodeFactory) throws JXPathException {
        List<AbstractNode> ret = null;

        long t0 = System.currentTimeMillis();
        logger.log(Level.FINE, "XPath: {0}, current node: {1}", new Object[]{xpath.getXPath(), currentNode.getName()});

        try {
            //JXPathContext context = getContext();

            // convert to set to remove duplicate entries
            Set nodeSet = new HashSet(context.selectNodes(xpath.getXPath()));

            ret = nodeFactory.createNodes(nodeSet);

        } catch (Throwable t) {

            if (t instanceof JXPathException || t instanceof JXPathInvalidSyntaxException) {
                logger.log(Level.WARNING, "Could not find node(s) by XPath: {0}", xpath.getXPath());
            }

        }


        if (ret != null && comparator != null) {
            Collections.sort(ret, comparator);
        }

        long t1 = System.currentTimeMillis();
        if (ret != null) {
            logger.log(Level.FINE, "Returning node list with {0} entries in {1} ms", new Object[]{ret.size(), t1 - t0});
        } else {
            logger.log(Level.FINE, "Returning null node list in {0} ms", new Object[]{t1 - t0});
        }
        return ret;
    }

    /**
     * Returns a node attribute as a result of the given
     * XPath expression.
     *
     * @param xpath the xpath expression
     * @return a node attribute
     * @throws JXPathException
     */
    public Object getNodeAttribute(String xpath) throws JXPathException {
        Object ret = null;

        try {
            //JXPathContext context = getContext();
            ret = context.getValue(xpath);

        } catch (Throwable t) {
            if (t instanceof JXPathException) {
                logger.log(Level.FINE, "Could not get node attribute from XPath " + xpath, t);
            }
            ret = null;
        }

        return ret;
    }

    /**
     * Creates a AbstractNode from a Neo4j node.
     *
     * @param node the node
     * @return the AbstractNode
     */
    private Node createStructrNode(Node node) {
        Command nodeFactory = Services.command(NodeFactoryCommand.class);
        return (Node) nodeFactory.execute(node);
    }
//
//    /**
//     * Create and return a new absolute JXPathContext
//     *
//     * @return a new absolute context
//     * @throws JXPathException
//     */
//    private JXPathContext getAbsoluteContext() throws JXPathException {
//        Node rootNode = db.getReferenceNode();
//
//        JXPathContextReferenceImpl.addNodePointerFactory(new NeoNodePointerFactory());
//        JXPathContext context = JXPathContextReferenceImpl.newContext(rootNode);
//
//        return context;
//    }

    /**
     * Creates and returns a new JXPathContext relative to the given node.
     *
     * If no other node is given, the context is relative to the
     * root node, thus an absolute context.
     *
     * @return a new relative context
     * @throws JXPathException
     *//*
    private JXPathContext getContext() throws JXPathException {

        return context;

    }*/
}
