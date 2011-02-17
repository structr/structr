package org.structr.core.entity;

/**
 * 
 * @author amorgner
 * 
 */
public class CsvFile extends File {

    private final static String ICON_SRC = "/images/page_white_excel.png";

    @Override
    public String getIconSrc() {
        return ICON_SRC;
    }
}
