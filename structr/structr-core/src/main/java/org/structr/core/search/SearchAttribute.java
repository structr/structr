/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.search;

/**
 * Wrapper representing a part of a search query.
 *
 * All parts of a search query must have a search operator and a payload.
 *
 * The payload can be either a node attribute oder a group of serach attributes.
 *
 * @author axel
 */
public abstract class SearchAttribute {

    public static final String WILDCARD = "*";

    private SearchOperator searchOp = null;

    public void setSearchOperator(final SearchOperator searchOp) {
        this.searchOp = searchOp;
    }

    public SearchOperator getSearchOperator() {
        return searchOp;
    }

    public abstract Object getAttribute();

    public abstract void setAttribute(Object attribute);
    
}
