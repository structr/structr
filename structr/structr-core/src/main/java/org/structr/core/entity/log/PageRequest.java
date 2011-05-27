/*
 *  Copyright (C) 2011 Axel Morgner, structr <structr@structr.org>
 * 
 *  This file is part of structr <http://structr.org>.
 * 
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
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

//    public static final String URI_KEY = "uri";
//    public static final String PARAMETERS_KEY = "parameters";
//    public static final String REMOTE_HOST_KEY = "remoteHost";
//    public static final String REMOTE_ADDRESS_KEY = "remoteAddress";

//    public String getUri() {
//        return getStringProperty(URI_KEY);
//    }
//
//    public void setUri(final String uri) {
//        setProperty(URI_KEY, uri);
//    }
//
//    public String getRemoteHost() {
//        return getStringProperty(REMOTE_HOST_KEY);
//    }
//
//    public void setRemoteHost(final String remoteHost) {
//        setProperty(REMOTE_HOST_KEY, remoteHost);
//    }
//
//    public String getRemoteAddress() {
//        return getStringProperty(REMOTE_ADDRESS_KEY);
//    }
//
//    public void setRemoteAddress(final String remoteAddr) {
//        setProperty(REMOTE_ADDRESS_KEY, remoteAddr);
//    }
//
//    @Override
//    public String getActivityText() {
//        String text =
//                "URI: " + getStringProperty(URI_KEY) + "\n" +
//                "Parameters: " + getStringProperty(PARAMETERS_KEY) + "\n" +
//                "Remote Host: " + getStringProperty(REMOTE_HOST_KEY) + "\n" +
//                "Remote Addr: " + getStringProperty(REMOTE_ADDRESS_KEY);
//        return text;
//    }

}
