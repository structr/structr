/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
