package org.structr.core.entity.web;

import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.User;

/**
 * A Menu contains menu items which will be rendered recursively.
 *
 * @author amorgner
 * 
 */
public class Menu extends MenuItem {

    private final static String ICON_SRC = "/images/tree.png";

    @Override
    public String getIconSrc() {
        return ICON_SRC;
    }
    public final static String MAX_DEPTH_KEY = "maxDepth";

    public int getMaxDepth() {
        return getIntProperty(MAX_DEPTH_KEY);
    }

    public void setMaxDepth(final int maxDepth) {
        setProperty(MAX_DEPTH_KEY, maxDepth);
    }

    /**
     * Render a menu
     */
    @Override
    public void renderView(StringBuilder out, final AbstractNode startNode,
            final String editUrl, final Long editNodeId, final User user) {

        if (editNodeId != null && getId() == editNodeId.longValue()) {

            renderEditFrame(out, editUrl);

        } else {

            if (isVisible()) {

                renderMenuItems(out, startNode, this, 0, 0, 0, getMaxDepth(), user);

            }
        }
    }




}
