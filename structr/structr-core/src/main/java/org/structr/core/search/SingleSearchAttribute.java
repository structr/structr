/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.search;

import org.structr.common.SearchOperator;
import org.structr.core.node.NodeAttribute;

/**
 * A parameterized node attribute extended by a boolean search operator.
 * <p>
 * Used in {@see SearchNodeCommand}.
 *
 * @author amorgner
 */
public class SingleSearchAttribute extends SearchAttribute {

    private NodeAttribute nodeAttribute;

    public SingleSearchAttribute(final String key, final Object value, final SearchOperator searchOp) {
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

    public Object getValue() {
        return nodeAttribute.getValue();
    }

}
