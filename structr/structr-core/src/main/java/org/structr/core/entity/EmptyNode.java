package org.structr.core.entity;

/**
 * 
 * @author amorgner
 * 
 */
public class EmptyNode extends AbstractNode {

    private final static String ICON_SRC = "/images/error.png";

    @Override
    public String getIconSrc() {
        return ICON_SRC;
    }
}
