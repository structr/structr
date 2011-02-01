package org.structr.core.entity;

/**
 * 
 * @author amorgner
 * 
 */
public class Principal extends StructrNode {

    private final static String ICON_SRC = "/images/user.png";

    @Override
    public String getIconSrc() {
        return ICON_SRC;
    }
}
