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
import org.apache.commons.lang.StringUtils;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.Index;
import org.structr.core.entity.AbstractNode;

/**
 * Command for indexing a node's property
 *
 * @author axel
 */
public class IndexNodeCommand extends NodeServiceCommand {

    private static final Logger logger = Logger.getLogger(IndexNodeCommand.class.getName());
    private Index<Node> index;

    @Override
    public Object execute(Object... parameters) {

        GraphDatabaseService graphDb = (GraphDatabaseService) arguments.get("graphDb");
        index = (Index<Node>) arguments.get("index");

        if (graphDb != null) {
            long id = 0;
            Node node = null;
            AbstractNode structrNode = null;

            String key = null;

            switch (parameters.length) {

                case 1:

                    // index all properties of this node
                    if (parameters[0] instanceof Long) {
                        id = ((Long) parameters[0]).longValue();
                    } else if (parameters[0] instanceof String) {
                        id = Long.parseLong((String) parameters[0]);
                    } else if (parameters[0] instanceof AbstractNode) {
                        structrNode = (AbstractNode) parameters[0];
                        id = structrNode.getId();
                    }

                    node = graphDb.getNodeById(id);

                    indexNode(node);

                    break;

                case 2:

                    // index a certain property

                    if (parameters[0] instanceof Long) {
                        id = ((Long) parameters[0]).longValue();
                    } else if (parameters[0] instanceof String) {
                        id = Long.parseLong((String) parameters[0]);
                    } else if (parameters[0] instanceof AbstractNode) {
                        structrNode = (AbstractNode) parameters[0];
                        id = structrNode.getId();
                    }

                    node = graphDb.getNodeById(id);

                    if (parameters[1] instanceof String) {
                        key = (String) parameters[1];
                    }

                    indexProperty(node, key);

                    break;


                default:

                    logger.log(Level.SEVERE, "Wrong number of parameters for the index property command: {0}", parameters);
                    return null;

            }
        }

        return null;

    }

    private void indexNode(final Node node) {

        for (String key : node.getPropertyKeys()) {
            indexProperty(node, key);
        }

    }

    private void indexProperty(final Node node, final String key) {


        if (key == null) {
            logger.log(Level.SEVERE, "Node {0} has null key", new Object[]{node.getId()});
            return;
        }

        boolean emptyKey = StringUtils.isEmpty((String) key);

        if (emptyKey) {
            logger.log(Level.SEVERE, "Node {0} has empty, not-null key, removing property", new Object[]{node.getId()});
            node.removeProperty(key);
            return;
        }

        if (!(node.hasProperty(key))) {
            logger.log(Level.FINE, "Node {0} has no key {1}, ignoring", new Object[]{node.getId(), key});
            return;
        }

        Object value = node.getProperty(key);

        boolean emptyValue = (value instanceof String && StringUtils.isEmpty((String) value));

        if (value == null) {

            logger.log(Level.SEVERE, "Node {0} has null value for key {1}, removing property", new Object[]{node.getId(), key});
            node.removeProperty(key);

        } else if (emptyValue) {

            logger.log(Level.WARNING, "Node {0} has empty, non-null value for key {1}", new Object[]{node.getId(), key});

        } else {

            index.remove(node, key, value);
            logger.log(Level.FINE, "Node {0}: Key {1}, Value {2} removed from index", new Object[]{node.getId(), key, value});

            index.add(node, key, value);
            logger.log(Level.FINE, "Node {0}: Key {1}, Value {2} added to index", new Object[]{node.getId(), key, value});
            
        }

    }
}
