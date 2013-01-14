/*
 *  Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 *  This file is part of structr <http://structr.org>.
 *
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */


package org.structr.web.common;

import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.NodeAttribute;
import org.structr.web.entity.Component;
import org.structr.web.entity.Condition;
import org.structr.web.entity.Page;

/**
 * Holds information about the context in which a resource is rendered,
 * like f.e. edit mode, language
 * 
 * @author Axel Morgner
 */


public class RenderContext {
	
	private static final Logger logger                   = Logger.getLogger(RenderContext.class.getName());
	
	private boolean edit = false;
	private int depth = 0;
	private boolean inBody = false;
	private String searchClass;
	private Condition condition;
	private final StringBuilder buffer = new StringBuilder(8192);
	
	private List<NodeAttribute> attrs;
	private AbstractNode viewComponent;
	private Page page;
	private Component component;
	
	
	private Locale locale = Locale.getDefault();
	private HttpServletRequest request    = null;
	private HttpServletResponse response  = null;
	
	public RenderContext() {
	}
	
	public RenderContext(final HttpServletRequest request, HttpServletResponse response, final boolean edit, final Locale locale) {
		
		this.request    = request;
		this.response   = response;
		
		this.edit = edit;
		this.locale = locale;
		
		
	}
	public static RenderContext getInstance(final HttpServletRequest request, HttpServletResponse response, final Locale locale) {

		return new RenderContext(request, response, request.getParameter("edit") != null, locale);

	}
	
	public boolean getEdit() {
		return edit;
	}
	
	public Locale getLocale() {
		return locale;
	}
	
	public String getISO3Country() {
		return getLocale().getISO3Country();
	}
	
	public String getISO3Language() {
		return getLocale().getISO3Language();
	}
	
	public HttpServletRequest getRequest() {
		return request;
	}
	
	public void increaseDepth() {
		this.depth++;
	}
	
	public void decreaseDepth() {
		this.depth--;
	}
	
	public void setDepth(final int depth) {
		this.depth = depth;
	}
	
	public int getDepth() {
		return depth;
	}
	
	public Condition getCondition() {
		return condition;
	}
	
	public String getSearchClass() {
		return searchClass;
	}
	
	public StringBuilder getBuffer() {
		return buffer;
	}
	
	public void setInBody(final boolean inBody) {
		this.inBody = inBody;
	}
	
	public boolean inBody() {
		return inBody;
	}
	
	public List<NodeAttribute> getAttrs() {
		return attrs;
	}
	
	public AbstractNode getViewComponent() {
		return viewComponent;
	}
	
	public void setPage(final Page page) {
		this.page = page;
	}
	
	public Page getPage() {
		return page;
	}
	
	public String getPageId() {
		return (page != null ? page.getUuid() : null);
	}
	
	public Component getComponent() {
		return component;
	}
	
	public String getComponentId() {
		return (component != null ? component.getUuid() : null);
	}
}
