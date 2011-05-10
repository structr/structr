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

import org.apache.commons.jxpath.ri.QName;
import org.apache.commons.jxpath.ri.compiler.NodeTest;
import org.apache.commons.jxpath.ri.model.NodeIterator;
import org.apache.commons.jxpath.ri.model.NodePointer;
import org.neo4j.graphdb.Node;

/**
 *
 * @author cmorgner
 */
public class Neo4jAttributePointer extends NodePointer {

    private QName name = null;
    private Object attribute = null;
    private Node node = null;

    public Neo4jAttributePointer(NodePointer parent, Node node, QName name, Object attribute) {
        super(parent);

        this.node = node;
        this.attribute = attribute;
        this.name = name;
    }

    public Neo4jAttributePointer(Node node) {
        super(null);

        this.node = node;
    }

    @Override
    public NodePointer getParent() {
        return (getImmediateParentPointer());
    }

    @Override
    public NodePointer getImmediateParentPointer() {
        return (parent);
    }

    @Override
    public synchronized Object getRootNode() {
        return (node.getGraphDatabase().getReferenceNode());
    }

    @Override
    public boolean isLeaf() {
        return (true);
    }

    @Override
    public boolean isCollection() {
        return (false);
    }

    @Override
    public int getLength() {
        return (1);
    }

    @Override
    public QName getName() {
        return (name);
    }

    @Override
    public Object getBaseValue() {
        return (attribute);
    }

    @Override
    public Object getValue() {
        return (attribute);
    }

    @Override
    public Object getImmediateNode() {
        return (node);
    }

    @Override
    public void setValue(Object value) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int compareChildNodePointers(NodePointer pointer1, NodePointer pointer2) {
        return (pointer1.asPath().compareTo(pointer2.asPath()));
    }

    @Override
    public NodeIterator childIterator(NodeTest test, boolean reverse, NodePointer startWith) {
        return (null);
    }

    @Override
    public NodeIterator attributeIterator(QName name) {
        return (null);
    }
}
