package org.structr.core.entity.web;

import org.structr.core.entity.PlainText;

/**
 * 
 * @author amorgner
 * 
 */
public class HtmlSource extends PlainText {

//    private final static String keyPrefix = "${";
//    private final static String keySuffix = "}";
//    private final static String requestKeyPrefix = "$[";
//    private final static String requestKeySuffix = "]";
//    private final static String subnodeKeyPrefix = "$(";
//    private final static String subnodeKeySuffix = ")";
    private final static String ICON_SRC = "/images/html.png";

    @Override
    public String getIconSrc() {
        return ICON_SRC;
    }

    /**
     * Render HTML content
     */
//    @Override
//    public void renderView(StringBuilder out, final StructrNode startNode,
//            final String editUrl, final Long editNodeId) {
//
//            super.renderView(out, startNode, editUrl, editNodeId);
//    }
}
