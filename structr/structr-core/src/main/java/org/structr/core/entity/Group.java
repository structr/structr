package org.structr.core.entity;

/**
 * 
 * @author amorgner
 * 
 */
public class Group extends Principal {

    private final static String ICON_SRC = "/images/group.png";

    @Override
    public String getIconSrc() {
        return ICON_SRC;
    }
}
