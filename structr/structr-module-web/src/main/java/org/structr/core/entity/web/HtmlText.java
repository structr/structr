package org.structr.core.entity.web;

/**
 * 
 * @author amorgner
 * 
 */
public class HtmlText extends HtmlSource {

    private final static String ICON_SRC = "/images/html_orange.png";

    @Override
    public String getIconSrc() {
        return ICON_SRC;
    }
}
