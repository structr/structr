package org.structr.core.entity.web;

import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.User;

/**
 * 
 * @author amorgner
 * 
 */
public class Site extends WebNode {

    private final static String ICON_SRC = "/images/sitemap_color.png";

    @Override
    public String getIconSrc() {
        return ICON_SRC;
    }

    /**
     * Render view of site node.
     *
     */
    @Override
    public void renderView(StringBuilder out, AbstractNode startNode,
            final String editUrl, final Long editNodeId, final User user) {


        if (editNodeId != null && getId() == editNodeId.longValue()) {

            renderEditFrame(out, editUrl);

        } else {

            if (isVisible(user)) {

                if (this instanceof WebNode) {

                    WebNode webNode = (WebNode) this;
                    HomePage homepage = webNode.getHomePage(user);

                    if (homepage == null) {
                        out.append("No home page found for ").append(getName());
                    } else {
                        homepage.renderView(out, homepage, editUrl, editNodeId, user);
                    }

                }
            }

        }
    }

    /**
     * Return part of an url
     *
     * @return
     */
    @Override
    public String getUrlPart() {
        return "http://" + getName();
    }
}
