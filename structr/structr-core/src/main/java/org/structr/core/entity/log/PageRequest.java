/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.entity.log;

/**
 * To log page requests
 * 
 * @author axel
 */
public class PageRequest extends Activity {

    private final static String ICON_SRC = "/images/page.png";

    @Override
    public String getIconSrc() {
        return ICON_SRC;
    }

    public static final String URI_KEY = "uri";
    public static final String REMOTE_HOST_KEY = "remoteHost";
    public static final String REMOTE_ADDRESS_KEY = "remoteAddress";

    public String getUri() {
        return getStringProperty(URI_KEY);
    }

    public void setUri(final String uri) {
        setProperty(URI_KEY, uri);
    }

    public String getRemoteHost() {
        return getStringProperty(REMOTE_HOST_KEY);
    }

    public void setRemoteHost(final String remoteHost) {
        setProperty(REMOTE_HOST_KEY, remoteHost);
    }

    public String getRemoteAddress() {
        return getStringProperty(REMOTE_ADDRESS_KEY);
    }

    public void setRemoteAddress(final String remoteAddr) {
        setProperty(REMOTE_ADDRESS_KEY, remoteAddr);
    }

    @Override
    public String getActivityText() {
        String text =
                "URI: " + getUri() + "\n" +
                "Remote Host: " + getRemoteHost() + "\n" +
                "Remote Addr: " + getRemoteAddress();
        return text;
    }

}
