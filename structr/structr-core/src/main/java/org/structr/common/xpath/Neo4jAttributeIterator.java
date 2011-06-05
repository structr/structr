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

import java.util.LinkedList;
import java.util.List;
//import java.util.logging.Level;
//import java.util.logging.Logger;
import org.apache.commons.jxpath.ri.QName;
import org.apache.commons.jxpath.ri.model.NodeIterator;
import org.apache.commons.jxpath.ri.model.NodePointer;
import org.neo4j.graphdb.Node;

/**
 * TODO: implement attribute iterator. Need to synthesize attribute nodes because
 * Neo4j stores attributes directly in nodes.
 *
 * @author cmorgner
 */
public class Neo4jAttributeIterator implements NodeIterator {

//    private static final Logger logger = Logger.getLogger(Neo4jAttributeIterator.class.getName());
    private NodePointer parent = null;
    private int position = 0;
    private Node node = null;
    private Object[] attributes = null;
    private QName name = null;

    public Neo4jAttributeIterator(NodePointer parent, QName name) {

//        long t0 = System.currentTimeMillis();

        this.parent = parent;
        this.name = name;
        this.node = (Node) parent.getNode();


        String lname = name.toString();

        if ("id".equals(name.toString())) {
            attributes = new Object[1];
            attributes[0] = node.getId();

        } else if (!("*".equals(lname))) {

            if (node.hasProperty(lname)) {
                attributes = new Object[1];
                attributes[0] = node.getProperty(lname);
            }

        } else {

            // handle wildcard
            List properties = new LinkedList();
            for (String key : node.getPropertyKeys()) {

                properties.add(node.getProperty(key));
            }

            this.attributes = properties.toArray();
        }

//        long t1 = System.currentTimeMillis();
//        String nodeName = (String) node.getProperty("name");
//        logger.log(Level.INFO, "Neo4jAttributeIterator created for attribute {0} and node {1} ({2}) in {3} ms", new Object[]{name, nodeName, node.getId(), t1 - t0});
    }

    @Override
    public int getPosition() {
        return (position);
    }

    @Override
    public boolean setPosition(int position) {
        if (position < 1 || position > attributes.length) {
            return (false);

        } else {
            this.position = position;

            return (true);
        }
    }

    @Override
    public NodePointer getNodePointer() {
        try {
            // fix one-off error (why does position start at 1??)

            return (new Neo4jAttributePointer(parent, node, name, attributes[position - 1]));

        } catch (Exception ex) {
        }

        return (null);
    }
}
