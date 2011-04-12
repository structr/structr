package org.structr.core.entity;

/**
 * 
 * @author amorgner
 * 
 */
public class Principal extends AbstractNode {

    private final static String ICON_SRC = "/images/user.png";

    @Override
    public String getIconSrc() {
        return ICON_SRC;
    }

    @Override
    public void renderView(StringBuilder out, final AbstractNode startNode, final String editUrl, final Long editNodeId, final User user)
    {

        if (editNodeId != null && getId() == editNodeId.longValue()) {

            renderEditFrame(out, editUrl);

        } else {

            if (isVisible(user)) {
                out.append(getName());
            }

        }
    }
}
