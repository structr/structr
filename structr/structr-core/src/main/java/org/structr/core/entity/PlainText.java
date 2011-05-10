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
package org.structr.core.entity;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

/**
 * 
 * @author amorgner
 * 
 */
public class PlainText extends AbstractNode {

    private final static Logger logger = Logger.getLogger(PlainText.class.getName());
    private final static String ICON_SRC = "/images/page_white_text.png";

    @Override
    public String getIconSrc() {
        return ICON_SRC;
    }
    public final static String CONTENT_KEY = "content";
    public final static String CONTENT_TYPE_KEY = "contentType";
    public final static String SIZE_KEY = "size";

    public String getContent() {
        return (String) getProperty(CONTENT_KEY);
    }

    public void setContent(final String content) {
        setProperty(CONTENT_KEY, content);
    }

    @Override
    public String getContentType() {
        return (String) getProperty(CONTENT_TYPE_KEY);
    }

    public void setContentType(final String contentType) {
        setProperty(CONTENT_TYPE_KEY, contentType);
    }

    public String getSize() {
        return (String) getProperty(SIZE_KEY);
    }

    public void setSize(final String size) {
        setProperty(SIZE_KEY, size);
    }

    @Override
    public void renderView(StringBuilder out, final AbstractNode startNode,
            final String editUrl, final Long editNodeId, final User user) {

        if (editNodeId != null && getId() == editNodeId.longValue()) {

            renderEditFrame(out, editUrl);

        } else {

            if (isVisible(user)) {
                String html = getContent();

                if (StringUtils.isNotBlank(html)) {

                    StringWriter content = new StringWriter();

                    // process content with Freemarker
                    replaceByFreeMarker(html, content, startNode, editUrl, editNodeId, user);

                    StringBuilder content2 = new StringBuilder(content.toString());

                    // finally, replace %{subnodeKey} by rendered content of subnodes with this name
                    replaceBySubnodes(content2, startNode, editUrl, editNodeId, user);
                    out.append(content2.toString());

                }
            }
        }
    }

    /**
     * Stream content directly to output.
     *
     * @param out
     */
    @Override
    public void renderDirect(OutputStream out, final AbstractNode startNode,
            final String editUrl, final Long editNodeId, final User user) {

        if (isVisible(user)) {

            try {

                StringBuilder sb = new StringBuilder();
                renderView(sb, startNode, editUrl, editNodeId, user);

                // write to output stream
                IOUtils.write(sb.toString(), out);

            } catch (IOException e) {
                logger.log(Level.WARNING, "Error while rendering {0}: {1}", new String[]{getContent(), e.getMessage()});
            }
        }
    }

    @Override
    public void onNodeCreation()
    {
    }

    @Override
    public void onNodeInstantiation()
    {
    }
}
