/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.node.search;

import org.structr.core.node.NodeAttribute;

/**
 * Represents an attribute for textual search.
 *
 * <p>
 * Used in {@see SearchNodeCommand}.
 *
 * @author amorgner
 */
public class TextualSearchAttribute extends SearchAttribute {

    private NodeAttribute nodeAttribute;

    public TextualSearchAttribute(final String key, final String value, final SearchOperator searchOp) {
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

    public String getValue() {
        return (String) nodeAttribute.getValue();
    }

}
