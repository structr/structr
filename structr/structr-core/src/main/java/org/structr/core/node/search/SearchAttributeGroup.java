/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.node.search;

import java.util.LinkedList;
import java.util.List;

/**
 * Represents a group of search operators, to be used for queries
 * with multiple search attributes grouped by parenthesis.
 *
 * @author axel
 */
public class SearchAttributeGroup extends SearchAttribute {

    private List<SearchAttribute> searchItems = new LinkedList<SearchAttribute>();

    public SearchAttributeGroup(final SearchOperator searchOp) {
        setSearchOperator(searchOp);
    }

    public final void setSearchAttributes(final List<SearchAttribute> searchItems) {
        this.searchItems = searchItems;
    }

    public List<SearchAttribute> getSearchAttributes() {
        return searchItems;
    }

    public void add(final SearchAttribute searchAttribute) {
        searchItems.add(searchAttribute);
    }

    @Override
    public Object getAttribute() {
        return searchItems;
    }

    @Override
    public void setAttribute(Object attribute) {
        this.searchItems = (List<SearchAttribute>) attribute;
    }

}
