package org.structr.core.entity;

/**
 * 
 * @author amorgner
 * 
 */
public class Link extends StructrNode {

    private final static String ICON_SRC = "/images/linked.png";

    @Override
    public String getIconSrc() {
        return iconSrc != null ? iconSrc : ICON_SRC;
    }
    private StructrNode structrNode;
    private String iconSrc;

    /**
     * Special constructor for wrapper node
     *
     * @param node
     */
    public void init(StructrNode node) {
        super.init(node.dbNode);
        setStructrNode(node);
    }

    /**
     * Get structr node
     *
     * @return
     */
    public StructrNode getStructrNode() {
        return structrNode;
    }

    /**
     * Set structr node
     */
    public void setStructrNode(final StructrNode structrNode) {
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
    public void renderView(StringBuilder out, final StructrNode startNode,
            final String editUrl, final Long editNodeId, final User user) {

        StructrNode node = getStructrNode();

        node.setTemplate(getTemplate(user));
        node.setRequest(getRequest());

        // if this page is requested to be edited, render edit frame
        if (editNodeId != null && getId() == editNodeId.longValue()) {

            node.renderView(out, startNode, editUrl, editNodeId, user);

        } else {
            if (isVisible()) {
                getStructrNode().renderView(out, startNode, editUrl, editNodeId, user);
            }
        }
    }

    public void setIconSrc(String iconSrc) {
        this.iconSrc = iconSrc;
    }
}
