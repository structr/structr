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
package org.structr.core.entity;

import java.util.Collections;

/**
 * 
 * @author amorgner
 * 
 */
public class ArbitraryNode extends AbstractNode {

    private final static String ICON_SRC = "/images/error.png";
    public final static String ICON_SRC_KEY = "iconSrc";

    @Override
    public String getIconSrc() {

        NodeType typeNode = getTypeNode();
        
        Object iconSrc = null;
        if (typeNode != null) {
            iconSrc = typeNode.getProperty(ICON_SRC_KEY);
        }

        if (iconSrc != null && iconSrc instanceof String) {
            return (String) iconSrc;
        } else {
            return ICON_SRC;
        }
    }

    @Override
    public Iterable<String> getPropertyKeys() {
        
        NodeType typeNode = getTypeNode();
        
        if (typeNode != null) {
            return typeNode.getPropertyKeys();
        }
        return Collections.EMPTY_LIST;
    }

    @Override
    public void renderView(StringBuilder out, AbstractNode startNode, String editUrl, Long editNodeId) {
        out.append(getName());
    }

    @Override
    public void onNodeCreation() {
    }

    @Override
    public void onNodeInstantiation() {
    }
}
