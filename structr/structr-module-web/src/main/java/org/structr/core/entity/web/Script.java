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
package org.structr.core.entity.web;

import bsh.Interpreter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.structr.common.CurrentRequest;
import org.structr.common.RenderMode;
import org.structr.core.NodeRenderer;
import org.structr.core.entity.AbstractNode;
import org.structr.renderer.ScriptRenderer;

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

    @Override
    public void initializeRenderers(Map<RenderMode, NodeRenderer> renderers)
    {
	    renderers.put(RenderMode.Default, new ScriptRenderer());
    }
}
