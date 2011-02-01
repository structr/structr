package org.structr.core.entity;

/**
 * 
 * @author amorgner
 * 
 */
public class Category extends StructrNode {

    private final static String ICON_SRC = "/images/tag_green.png";

    @Override
    public String getIconSrc() {
        return ICON_SRC;
    }
}
