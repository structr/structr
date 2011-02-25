package org.structr.core.entity.web;


import java.util.*;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.User;

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
            final String editUrl, final Long editNodeId, final User user) {

        // if this page is requested to be edited, render edit frame
        if (editNodeId != null && getId() == editNodeId.longValue()) {

            renderEditFrame(out, editUrl);

            // otherwise, render subnodes in edit mode
        } else {

            List<AbstractNode> subnodes = getSortedDirectChildAndLinkNodes(user);

            out.append("<div class=\"row\">");
            // render nodes in correct order
            for (AbstractNode s : subnodes) {

                out.append("<div class=\"rowItem\">");

                if (editNodeId != null && s.getId() == editNodeId.longValue()) {
                    renderEditFrame(out, editUrl);
                } else {
                    s.renderView(out, startNode, editUrl, editNodeId, user);
                }
                out.append("</div>");
            }
            out.append("</div>");
        }
    }
}
