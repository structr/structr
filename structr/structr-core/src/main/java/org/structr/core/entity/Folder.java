package org.structr.core.entity;

/**
 * 
 * @author amorgner
 * 
 */
public class Folder extends AbstractNode {

    private final static String ICON_SRC = "/images/folder.png";

    @Override
    public String getIconSrc() {
        return ICON_SRC;
    }
}
