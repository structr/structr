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
package org.structr.common;

import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author Christian Morgner
 */
public final class StructrOutputStream extends OutputStream {

    private static final Logger logger = Logger.getLogger(StructrOutputStream.class.getName());
    private SecurityContext securityContext = null;
    private HttpServletResponse response = null;
    private StringBuilder stringBuilder = null;
    private HttpServletRequest request = null;
    private boolean contentTypeSet = false;
    private String encoding = "utf-8";

    /**
     * Creates a new StructrOutputStream with an internally allocated
     * initial buffer of 1024 bytes. Using this constructor puts this
     * StructrOutputStream in buffer mode.
     */
    public StructrOutputStream(SecurityContext securityContext) {
	this.securityContext = securityContext;
        stringBuilder = new StringBuilder(1024);
    }

    /**
     * Creates a new StructrOutputStream with the given StringBuilder
     * wrapped inside. Using this constructor puts this
     * StructrOutputStream in buffer mode.
     *
     * @param toWrtap the StringBuilder to wrap
     */
    public StructrOutputStream(SecurityContext securityContext, StringBuilder toWrap) {
	this.securityContext = securityContext;
        stringBuilder = toWrap;
    }

    /**
     * Creates a new StructrOutputStream with the given OutputStream
     * wrapped inside. Using this constructor puts this StructrOutputStream
     * in direct mode.
     *
     * @param outputStream the output stream to wrap
     */
    public StructrOutputStream(HttpServletRequest request, HttpServletResponse response, SecurityContext securityContext) {
	this.request = request;
        this.response = response;
	this. securityContext = securityContext;
    }

    public StructrOutputStream append(Object o) {
        if (o != null) {
            if (response != null) {
                try {
                    response.getOutputStream().write(o.toString().getBytes(encoding));

                } catch (Throwable t) {
                    logger.log(Level.WARNING, "Exception while appending object to output stream: {0}", t);
                }

            } else if (stringBuilder != null) {
                stringBuilder.append(o);
            }
        }

        return (this);
    }

    @Override
    public String toString() {
        if (stringBuilder == null) {
            throw new IllegalStateException("StructrOutputStream used as StringBuilder but not initialized as such!");
        }

        return (stringBuilder.toString());
    }

    @Override
    public void write(int i) throws IOException {
        if (response != null) {
            response.getOutputStream().write(i);
        }
    }

    @Override
    public void write(byte[] data) throws IOException {
        if (response != null) {
            response.getOutputStream().write(data);
        }
    }

    @Override
    public void write(byte[] data, int offset, int length) throws IOException {
        if (response != null) {
            response.getOutputStream().write(data, offset, length);
        }
    }

    /**
     * @param contentType the contentType to set
     */
    public void setContentType(String contentType) {
        // content type must be set BEFORE the output stream for this
        // response is created.
        if (response != null && !contentTypeSet) {
            if (contentType != null) {
                response.setContentType(contentType);

            } else {
                // FIXME: this is a workaround for PlainText /
                // Template nodes returning content type "null"!
                response.setContentType("text/html");
            }

            response.setCharacterEncoding(encoding);

            contentTypeSet = true;
        }
    }

    /**
     * @return the encoding 
     */
    public String getEncoding() {
        return encoding;
    }

    /**
     * @param encoding the encoding to set
     */
    public void setEncoding(final String encoding) {
        this.encoding = encoding;
    }

//    public void setSecurityContext(SecurityContext context) {
//	this.securityContext = context;
//    }

    public SecurityContext getSecurityContext() {
	return this.securityContext;
    }

//    public void setRequest(HttpServletRequest request) {
//	this.request = request;
//    }

    public HttpServletRequest getRequest() {
	return this.request;
    }

    public HttpServletResponse getResponse() {
	return this.response;
    }
}
