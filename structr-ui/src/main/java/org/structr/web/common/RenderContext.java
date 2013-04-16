/**
 * Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 * This file is part of structr <http://structr.org>.
 *
 * structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.structr.web.common;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.structr.core.GraphObject;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.NodeAttribute;
import org.structr.rest.ResourceProvider;
import org.structr.web.entity.Component;
import org.structr.web.entity.Condition;
import org.structr.web.entity.dom.Page;

/**
 * Holds information about the context in which a resource is rendered,
 * like f.e. edit mode, language
 * 
 * @author Axel Morgner
 */


public class RenderContext {
	
	private static final Logger logger                   = Logger.getLogger(RenderContext.class.getName());
	
	private Map<String, GraphObject> dataObjects = new LinkedHashMap<String, GraphObject>();
	private final StringBuilder buffer           = new StringBuilder(8192);
	private Locale locale                        = Locale.getDefault();
	private boolean edit                         = false;
	private int depth                            = 0;
	private boolean inBody                       = false;
	private GraphObject detailsDataObject        = null;
	private GraphObject currentDataObject        = null;
	private Iterable<GraphObject> listSource     = null;
	private String searchClass                   = null;  
	private Condition condition                  = null; 
	private List<NodeAttribute> attrs            = null;   
	private Page page                            = null;  
	private Component component                  = null;  
	private HttpServletRequest request           = null;
	private HttpServletResponse response         = null;
	private ResourceProvider resourceProvider    = null;
	
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
	
	public void setDetailsDataObject(GraphObject detailsDataObject) {
		this.detailsDataObject = detailsDataObject;
	}
	
	public GraphObject getDetailsDataObject() {
		return detailsDataObject;
	}
	
	public void setDataObject(GraphObject currentDataObject) {
		this.currentDataObject = currentDataObject;
	}
	
	public GraphObject getDataObject() {
		return currentDataObject;
	}
	
	public void setListSource(Iterable<GraphObject> listSource) {
		this.listSource = listSource;
	}
	
	public Iterable<GraphObject> getListSource() {
		return listSource;
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
	
	public HttpServletResponse getResponse() {
		return response;
	}
	
	public void setResourceProvider(final ResourceProvider resourceProvider) {
		this.resourceProvider = resourceProvider;
	}
	
	public ResourceProvider getResourceProvider() {
		return resourceProvider;
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
	
	public GraphObject getDataNode(String key) {
		return dataObjects.get(key);
	}
	
	public void putDataObject(String key, GraphObject currentDataObject) {
		dataObjects.put(key, currentDataObject);
		setDataObject(currentDataObject);
		
	}
	
	public void clearDataObject(String key) {
		dataObjects.remove(key);
		setDataObject(null);
	}
	
	public boolean hasDataForKey(String key) {
		return dataObjects.containsKey(key);
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
