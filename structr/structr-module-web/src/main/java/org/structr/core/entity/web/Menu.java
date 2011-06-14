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

import org.structr.common.StructrOutputStream;
import org.structr.core.entity.AbstractNode;

/**
 * A Menu contains menu items which will be rendered recursively.
 *
 * @author amorgner
 * 
 */
public class Menu extends MenuItem {

    private final static String ICON_SRC = "/images/tree.png";
    private final static int maxDepthDefault = 3;

    @Override
    public String getIconSrc() {
        return ICON_SRC;
    }
    public final static String MAX_DEPTH_KEY = "maxDepth";

    public int getMaxDepth() {
        Object depth = getProperty(MAX_DEPTH_KEY);
        
        // The first time, set default max depth
        if (depth == null) {
            setMaxDepth(maxDepthDefault);
            return maxDepthDefault;
        } else {
            return getIntProperty(MAX_DEPTH_KEY);
        }
    }

    public void setMaxDepth(final int maxDepth) {
        setProperty(MAX_DEPTH_KEY, maxDepth);
    }

    /**
     * Render a menu
     */
    @Override
    public void renderNode(final StructrOutputStream out, final AbstractNode startNode,
            final String editUrl, final Long editNodeId) {

        if (editNodeId != null && getId() == editNodeId.longValue()) {

            renderEditFrame(out, editUrl);

        } else {

            if (isVisible()) {

                renderMenuItems(out, startNode, this, 0, 0, 0, getMaxDepth());

            }
        }
    }
}
