package org.structr.core.entity.web;

import bsh.Interpreter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.apache.commons.io.IOUtils;
import org.structr.common.CurrentRequest;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.User;
//import org.structr.common.xpath.JXPathFinder;

/**
 * 
 * @author cmorgner
 * 
 */
public class Script extends AbstractNode {

    private final static String ICON_SRC = "/images/script.png";

    @Override
    public String getIconSrc() {
        return ICON_SRC;
    }
    private final static String TEXT_KEY = "script";
    private final static String CONTENT_TYPE_KEY = "contentType";
    private final static String SIZE_KEY = "size";

    public String getText() {
        return (String) getProperty(TEXT_KEY);
    }

    @Override
    public String getContentType() {
        return (String) getProperty(CONTENT_TYPE_KEY);
    }

    public String getSize() {
        return (String) getProperty(SIZE_KEY);
    }

    public void setText(final String text) {
        setProperty(TEXT_KEY, text);
    }

    public void setMimeType(final String mimeType) {
        setProperty(CONTENT_TYPE_KEY, mimeType);
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
                out.append(evaluate());
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

                StringReader in = new StringReader(getText());

                // just copy to output stream
                IOUtils.copy(in, out);

            } catch (IOException e) {
                System.out.println("Error while rendering " + getText() + ": " + e.getMessage());
            }
        }
    }

    @Override
    public String evaluate() {
        StringBuilder ret = new StringBuilder();

        try {
//            JXPathFinder nodeFinder = new JXPathFinder(this);
            HttpServletRequest request = CurrentRequest.getRequest();
            HttpSession session = CurrentRequest.getSession();

            Interpreter interpreter = new Interpreter();
            interpreter.set("_buffer", ret);
            interpreter.set("_session", session);
            interpreter.set("_request", request);
//            interpreter.set("_finder", nodeFinder);

            String contents = getText();
            interpreter.eval(contents);

        } catch (Throwable t) {
            StringWriter stringWriter = new StringWriter();
            PrintWriter writer = new PrintWriter(stringWriter);
            t.printStackTrace(writer);

            ret.append(stringWriter.getBuffer().toString());
        }

        return (ret.toString());
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
