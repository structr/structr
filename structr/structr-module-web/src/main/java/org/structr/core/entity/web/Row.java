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


import java.util.*;
import org.structr.core.entity.AbstractNode;

/**
 * 
 * @author amorgner
 * 
 */
public class Row extends HtmlText {

    private final static String ICON_SRC = "/images/layout_row.png";

    @Override
    public final String getIconSrc() {
        return ICON_SRC;
    }

    /**
     * Render edit view
     *
     * @param out
     * @param startNode
     * @param editUrl
     * @param editNodeId
     */
    @Override
    public void renderView(StringBuilder out, final AbstractNode startNode,
            final String editUrl, final Long editNodeId) {

        // if this page is requested to be edited, render edit frame
        if (editNodeId != null && getId() == editNodeId.longValue()) {

            renderEditFrame(out, editUrl);

            // otherwise, render subnodes in edit mode
        } else {

            List<AbstractNode> subnodes = getSortedDirectChildAndLinkNodes();

            out.append("<div class=\"row\">");
            // render nodes in correct order
            for (AbstractNode s : subnodes) {

                out.append("<div class=\"rowItem\">");

                if (editNodeId != null && s.getId() == editNodeId.longValue()) {
                    renderEditFrame(out, editUrl);
                } else {
                    s.renderView(out, startNode, editUrl, editNodeId);
                }
                out.append("</div>");
            }
            out.append("</div>");
        }
    }
}
