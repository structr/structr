/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.common.xpath;

/**
 *
 * Encode a path expression to work with JXPath
 *
 * @author amorgner
 */
public class XPathEncoder {

    public static String encode(String path) {

        String encodedPath = path
                .replaceAll(" ", "0x20")
                .replaceAll("\\[", "0x5B")
                .replaceAll("]", "0x5D")
                .replaceAll(":", "0x3A")
                .replaceAll("\\$", "0x24")
                .replaceAll("%", "0x25")
                .replaceAll("<", "0x3C")
                .replaceAll(">", "0x3E")
                .replaceAll("\\(", "0x28")
                .replaceAll("\\)", "0x29")
                .replaceAll("@", "0x40")
                .replaceAll("'", "0x27")
                .replaceAll("\\+", "0x2B")
                .replaceAll("-", "0x2D")
                .replaceAll("=", "0x3D")
                .replaceAll(",", "0x2C")
                .replaceAll("#", "0x23")
                .replaceAll("\\*", "0x2A")
                .replaceAll("!", "0x21")
                .replaceAll("\\.", "0x2E");

        return encodedPath;
    }
}
