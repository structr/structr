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
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.User;

/**
 *
 * @author axel
 */
public class WebNode extends AbstractNode {

    private static final Logger logger = Logger.getLogger(AbstractNode.class.getName());

    protected final static String SESSION_BLOCKED = "sessionBlocked";
    protected final static String USERNAME_KEY = "username";

    /**
     * Traverse over all child nodes to find a home page
     */
    public HomePage getHomePage(final User user) {

        List<AbstractNode> childNodes = getAllChildren(HomePage.class.getSimpleName(), user);

        for (AbstractNode node : childNodes) {

            if (node instanceof HomePage) {
                return ((HomePage) node);
            }

        }
        logger.log(Level.FINE, "No home page found for node {0}", this.getId());
        return null;
    }


    /**
     * Render a node-specific view as html
     */
    @Override
    public void renderView(StringBuilder out, final AbstractNode startNode,
            final String editUrl, final Long editNodeId, final User user) {

        if (editNodeId != null && getId() == editNodeId.longValue()) {

            renderEditFrame(out, editUrl);

        } else {

            if (isVisible(user)) {

                if (hasTemplate(user)) {
//                    template.setRequest(getRequest());
                    template.setCallingNode(this);
                    template.renderView(out, startNode, editUrl, editNodeId, user);
                } else {
                    out.append(getName());
                }
            }

        }
    }

    @Override
    public String getIconSrc() {
        return "/images/folder.png";
    }

    @Override
    public void onNodeCreation()
    {
    }

    @Override
    public void onNodeInstantiation()
    {
    }
}
