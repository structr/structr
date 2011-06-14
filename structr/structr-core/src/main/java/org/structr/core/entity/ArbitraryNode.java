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
import java.util.List;
import org.structr.common.StructrOutputStream;

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
    public void renderNode(StructrOutputStream out, AbstractNode startNode, String editUrl, Long editNodeId) {

        // if this page is requested to be edited, render edit frame
        if (editNodeId != null && getId() == editNodeId.longValue()) {

            renderEditFrame(out, editUrl);

        // otherwise, render subnodes in edit mode
        } else {

            if (hasTemplate()) {

                template.setCallingNode(this);
                template.renderNode(out, startNode, editUrl, editNodeId);
            } else {

                List<AbstractNode> subnodes = getSortedDirectChildAndLinkNodes();

                // render subnodes in correct order
                for (AbstractNode s : subnodes) {
                    s.renderNode(	null,startNode, editUrl, editNodeId);
                }
            }
        }
    }

    @Override
    public void onNodeCreation() {
    }

    @Override
    public void onNodeInstantiation() {
    }
}
