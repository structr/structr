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
package org.structr.core.node.search;

import java.util.LinkedList;
import java.util.List;
import org.structr.core.entity.AbstractNode;

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
    private List<AbstractNode> result = new LinkedList<AbstractNode>();

    public void setSearchOperator(final SearchOperator searchOp) {
        this.searchOp = searchOp;
    }

    public SearchOperator getSearchOperator() {
        return searchOp;
    }

    public void setResult(final List<AbstractNode> result) {
        this.result = result;
    }

    public List<AbstractNode> getResult() {
        return result;
    }

    public void addToResult(final AbstractNode node) {
        result.add(node);
    }

    public abstract Object getAttribute();

    public abstract void setAttribute(Object attribute);
    
}
