package org.structr.core.entity;

/**
 * 
 * @author amorgner
 * 
 */
public class Link extends AbstractNode {

    private final static String ICON_SRC = "/images/linked.png";

    @Override
    public String getIconSrc() {
        return iconSrc != null ? iconSrc : ICON_SRC;
    }
    private AbstractNode structrNode;
    private String iconSrc;

    /**
     * Special constructor for wrapper node
     *
     * @param node
     */
    public void init(AbstractNode node) {
        super.init(node.dbNode);
        setStructrNode(node);
    }

    /**
     * Get structr node
     *
     * @return
     */
    public AbstractNode getStructrNode() {
        return structrNode;
    }

    /**
     * Set structr node
     */
    public void setStructrNode(final AbstractNode structrNode) {
        this.structrNode = structrNode;
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

        AbstractNode node = getStructrNode();

        node.setTemplate(getTemplate(user));
//        node.setRequest(getRequest());

        // if this page is requested to be edited, render edit frame
        if (editNodeId != null && getId() == editNodeId.longValue()) {

            node.renderView(out, startNode, editUrl, editNodeId, user);

        } else {
            if (isVisible(user)) {
                getStructrNode().renderView(out, startNode, editUrl, editNodeId, user);
            }
        }
    }

    public void setIconSrc(String iconSrc) {
        this.iconSrc = iconSrc;
    }
}
