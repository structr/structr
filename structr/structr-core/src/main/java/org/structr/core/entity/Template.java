package org.structr.core.entity;

/**
 * A Template is basically a HtmlSource node.
 * 
 * The difference is that it uses the calling node's parameters
 * for replacement of its placeholders.
 *
 * @author amorgner
 * 
 */
public class Template extends PlainText {

    private final static String ICON_SRC = "/images/page_white_gear.png";
    private StructrNode callingNode;

    @Override
    public String getIconSrc() {
        return ICON_SRC;
    }

    public void setCallingNode(final StructrNode callingNode) {
        this.callingNode = callingNode;
    }

    public StructrNode getCallingNode() {
        return callingNode;
    }

}
