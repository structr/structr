/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.node;

import org.structr.common.SearchOperator;

/**
 * A parameterized node attribute extended by a boolean search operator.
 * <p>
 * Used in {@see SearchNodeCommand}.
 *
 * @author amorgner
 */
public class SearchAttribute extends NodeAttribute {

    public static final String WILDCARD = "*";
    public static final String NOT_NULL = "[0 TO Z]";
    public static final String NULL = "NOT([* TO *])";
    
    private SearchOperator searchOp = null;

    public SearchAttribute(final String key, final Object value, final SearchOperator searchOp) {
        super(key, value);
        this.searchOp = searchOp;
    }

    public SearchOperator getSearchOperator() {
        return searchOp;
    }

    public void setSearchOperator(final SearchOperator searchOp) {
        this.searchOp = searchOp;
    }
}
