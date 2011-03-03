/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.search;

import org.structr.core.node.NodeAttribute;

/**
 * A parameterized node attribute extended by a boolean search operator.
 * <p>
 * Used in {@see SearchNodeCommand}.
 *
 * @author amorgner
 */
public class BooleanSearchAttribute extends SearchAttribute {

    private NodeAttribute nodeAttribute;

    public BooleanSearchAttribute(final String key, final Boolean value, final SearchOperator searchOp) {
        nodeAttribute = new NodeAttribute(key, value);
        setSearchOperator(searchOp);
    }

    @Override
    public Object getAttribute() {
        return nodeAttribute;
    }

    @Override
    public void setAttribute(Object attribute) {
        this.nodeAttribute = (NodeAttribute) attribute;
    }

    public String getKey() {
        return nodeAttribute.getKey();
    }

    public Boolean getValue() {
        return (Boolean) nodeAttribute.getValue();
    }

}
