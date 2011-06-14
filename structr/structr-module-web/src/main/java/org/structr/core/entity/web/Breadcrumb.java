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
package org.structr.core.entity.web;

import java.util.List;
import org.structr.common.StructrOutputStream;
import org.structr.core.entity.AbstractNode;

/**
 * Render a breadcrumb navigation element
 *
 * @author amorgner
 * 
 */
public class Breadcrumb extends WebNode {

    private final static String ICON_SRC = "/images/control_equalizer.png";

    @Override
    public String getIconSrc() {
        return ICON_SRC;
    }

    /**
     * Render a breadcrumb
     */
    @Override
    public void renderNode(final StructrOutputStream out, final AbstractNode startNode,
            final String editUrl, final Long editNodeId) {

        if (editNodeId != null && getId() == editNodeId.longValue()) {

            renderEditFrame(out, editUrl);

        } else {

            if (isVisible()) {

                renderBreadcrumbItems(out, startNode, this);

            }
        }
    }

    /**
     * Render breadcrumb items
     *
     * @param out
     * @param startNode
     */
    private void renderBreadcrumbItems(final StructrOutputStream out, final AbstractNode startNode, final AbstractNode currentNode) {

        List<AbstractNode> ancestors = startNode.getAncestorNodes();

        out.append("<ul>");

        String cssClass = "";

        int currentPos = 0;

        for (AbstractNode breadcrumbItem : ancestors) {

            if (currentPos == 0) {
                cssClass = " first";
            }
            
            if (currentPos == ancestors.size() - 1) {
                cssClass = " last";
            }

            if (breadcrumbItem.equals(startNode)) {
                cssClass += " current";
            }

            if (breadcrumbItem.isVisible()) {

                String relativeNodePath = breadcrumbItem.getNodePath(startNode).replace("&", "%26");

                if (!(cssClass.isEmpty())) {
                    cssClass = " class=\"" + cssClass + "\"";
                }

                out.append("<li").append(cssClass).append(">");
                out.append("<span>" + "<a href=\"").append(relativeNodePath).append("\">");
                out.append(breadcrumbItem.getName());
                out.append("</a>").append("</span>\n");
                out.append("</li>");

            }
        }

        out.append("</ul>");

    }
}
