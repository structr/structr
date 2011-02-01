package org.structr.core.entity.web;

import org.structr.core.entity.web.Page;

/**
 * 
 * @author amorgner
 * 
 */
public class HomePage extends Page {

    private final static String ICON_SRC = "/images/home.png";

    @Override
    public String getIconSrc() {
        return ICON_SRC;
    }
}
