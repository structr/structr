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

import org.structr.common.CurrentRequest;
import org.structr.common.PropertyKey;
import org.structr.common.PropertyView;
import org.structr.common.RenderMode;
import org.structr.core.EntityContext;
import org.structr.core.NodeRenderer;
import org.structr.core.entity.AbstractNode;
import org.structr.renderer.ScriptRenderer;

//~--- JDK imports ------------------------------------------------------------

import java.io.PrintWriter;
import java.io.StringWriter;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author cmorgner
 *
 */
public class Script extends AbstractNode {

	static {

		EntityContext.registerPropertySet(Script.class,
						  PropertyView.All,
						  Key.values());
	}

	//~--- constant enums -------------------------------------------------

	public enum Key implements PropertyKey{ script, contentType, text, size; }

	//~--- methods --------------------------------------------------------

	@Override
	public String evaluate() {

		StringBuilder ret = new StringBuilder();

		try {

//                      JXPathFinder nodeFinder = new JXPathFinder(this);
			HttpServletRequest request = CurrentRequest.getRequest();
			HttpSession session        = CurrentRequest.getSession();
			Interpreter interpreter    = new Interpreter();

			interpreter.set("_buffer",
					ret);
			interpreter.set("_session",
					session);
			interpreter.set("_request",
					request);

//                      interpreter.set("_finder", nodeFinder);

			String contents = getText();

			interpreter.eval(contents);
		} catch (Throwable t) {

			StringWriter stringWriter = new StringWriter();
			PrintWriter writer        = new PrintWriter(stringWriter);

			t.printStackTrace(writer);
			ret.append(stringWriter.getBuffer().toString());
		}

		return (ret.toString());
	}

	@Override
	public void initializeRenderers(Map<RenderMode, NodeRenderer> renderers) {

		renderers.put(RenderMode.Default,
			      new ScriptRenderer());
	}

	//~--- get methods ----------------------------------------------------

	@Override
	public String getIconSrc() {
		return "/images/script.png";
	}

	public String getText() {
		return (String) getProperty(Key.text.name());
	}

	@Override
	public String getContentType() {
		return (String) getProperty(Key.contentType.name());
	}

	public String getSize() {
		return (String) getProperty(Key.size.name());
	}

	//~--- set methods ----------------------------------------------------

	public void setText(final String text) {

		setProperty(Key.text.name(),
			    text);
	}

	public void setMimeType(final String mimeType) {

		setProperty(Key.contentType.name(),
			    mimeType);
	}

	public void setSize(final String size) {

		setProperty(Key.size.name(),
			    size);
	}
}
