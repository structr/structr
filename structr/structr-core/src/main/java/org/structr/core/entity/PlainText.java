package org.structr.core.entity;

import org.structr.core.entity.Template;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;
import org.structr.core.entity.StructrNode;
import org.structr.core.entity.User;

/**
 * 
 * @author amorgner
 * 
 */
public class PlainText extends StructrNode {

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
    public void renderView(StringBuilder out, final StructrNode startNode,
            final String editUrl, final Long editNodeId, final User user) {

        if (editNodeId != null && getId() == editNodeId.longValue()) {

            renderEditFrame(out, editUrl);

        } else {

            if (isVisible()) {
                String html = getContent();

                if (html != null && html.length() > 0) {

                    Template t = startNode.getTemplate(user);

                    //StringBuilder content = new StringBuilder(html);
                    //HttpServletRequest request = getRequest();

                    if (t != null && t.getCallingNode() != null) {

                        // then, replace $[paramKey] by request.getParameter(paramKey)
//                        if (request != null) {
//                            replaceByRequestValues(content, startNode, editUrl, editNodeId);
//                        }

                        //}

                        // then replace placeholder for property values
                        //replaceByPropertyValues(content, startNode, editUrl, editNodeId);

                        //out.append(content);

                        //StringWriter sw = new StringWriter();

                        StringWriter content = new StringWriter();

                        // process content with Freemarker
                        //replaceByFreeMarker(html, sw, startNode, editUrl, editNodeId);
                        replaceByFreeMarker(html, content, startNode, editUrl, editNodeId, user);

                        String test = content.toString();



//                        sw.flush();
//
                        StringBuilder content2 = new StringBuilder(test);


                        // finally, replace %{subnodeKey} by rendered content of subnodes with this name
                        replaceBySubnodes(content2, startNode, editUrl, editNodeId, user);
//
//                        out.append(content);

                        String test2 = content2.toString();
                        out.append(test2);


                    } else {
                        out.append(getContent());
                    }
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
    public void renderDirect(OutputStream out, final StructrNode startNode,
            final String editUrl, final Long editNodeId, final User user) {

        if (isVisible()) {

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
}
