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

import org.structr.core.entity.AbstractNode;

/**
 * 
 * @author amorgner
 * 
 */
public class Domain extends WebNode {

    private final static String ICON_SRC = "/images/page_world.png";

    @Override
    public String getIconSrc() {
        return ICON_SRC;
    }

    /**
     * Render view of domain node.
     */
    @Override
    public void renderView(StringBuilder out, AbstractNode startNode,
            final String editUrl, final Long editNodeId) {

        if (editNodeId != null && getId() == editNodeId.longValue()) {

            renderEditFrame(out, editUrl);

        } else {

            if (isVisible()) {

                if (this instanceof WebNode) {

                    WebNode webNode = (WebNode) this;
                    HomePage homepage = webNode.getHomePage();

                    if (homepage == null) {

                        out.append("No home page found for ").append(getName());

                    } else {
                        homepage.renderView(out, homepage, editUrl, editNodeId);

                    }

                }
            }
        }
    }

//    /**
//     * Return part of an url
//     *
//     * @return
//     */
//    @Override
//    public String getUrlPart() {
//        return getName();
//    }
}
