/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.web.common;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang.StringUtils;
import org.structr.core.GraphObject;
import org.structr.core.Result;
import org.structr.core.entity.Principal;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.property.PropertyKey;
import org.structr.rest.ResourceProvider;
import org.structr.web.entity.Component;
import org.structr.web.entity.dom.Page;

/**
 * Holds information about the context in which a resource is rendered,
 * like f.e. edit mode, language
 * 
 * @author Axel Morgner
 */


public class RenderContext {
	
	private static final Logger logger                   = Logger.getLogger(RenderContext.class.getName());
	
	private Map<String, GraphObject> dataObjects = new LinkedHashMap<>();
	//private final StringBuilder buffer           = new StringBuilder(8192);
	private Locale locale                        = Locale.getDefault();
	private EditMode editMode                    = EditMode.NONE;
	private int depth                            = 0;
	private boolean inBody                       = false;
	private boolean appLibRendered               = false;
	private GraphObject detailsDataObject        = null;
	private GraphObject currentDataObject        = null;
	private GraphObject sourceDataObject         = null;
	private Iterable<GraphObject> listSource     = null;
	private String searchClass                   = null;  
	private PropertyKey relatedProperty          = null;
	private List<NodeAttribute> attrs            = null;   
	private Page page                            = null;  
	private Component component                  = null;  
	private HttpServletRequest request           = null;
	private HttpServletResponse response         = null;
	private ResourceProvider resourceProvider    = null;
	private Result result                        = null;
	
	public enum EditMode {
	
		NONE, DATA, CONTENT, RAW;
		
	}
	
	public RenderContext() {
	}
	
	public RenderContext(final HttpServletRequest request, HttpServletResponse response, final EditMode editMode, final Locale locale) {
		
		this.request    = request;
		this.response   = response;
		
		this.editMode = editMode;
		this.locale = locale;
		
		
	}
	public static RenderContext getInstance(final HttpServletRequest request, HttpServletResponse response, final Locale locale) {

		String editString = StringUtils.defaultString(request.getParameter("edit"));
		
		EditMode edit;

		switch (editString) {
			
			case "1" :
				
				edit = EditMode.DATA;
				break;
			
			case "2" :
				
				edit = EditMode.CONTENT;
				break;
				
			case "3" :
				
				edit = EditMode.RAW;
				break;

			default :
				
				edit = EditMode.NONE;
			
		}
		
		return new RenderContext(request, response, edit, locale);

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
	
	public void setSourceDataObject(GraphObject sourceDataObject) {
		this.sourceDataObject = sourceDataObject;
	}
	
	public GraphObject getSourceDataObject() {
		return sourceDataObject;
	}

	public void setListSource(Iterable<GraphObject> listSource) {
		this.listSource = listSource;
	}
	
	public void setResult(Result result) {
		this.result = result;
	}
	
	public Iterable<GraphObject> getListSource() {
		return listSource;
	}
	
	public PropertyKey getRelatedProperty() {
		return relatedProperty;
	}
	
	public void setRelatedProperty(final PropertyKey relatedProperty) {
		this.relatedProperty = relatedProperty;
	}
	
	/**
	 * Return edit mode.
	 * 
	 * If no user is logged in, the edit mode is always NONE to disable
	 * editing for public sessions.
	 * 
	 * @param user
	 * @return 
	 */
	public EditMode getEditMode(final Principal user) {
		return user == null ? EditMode.NONE : editMode;
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
	
	public String getSearchClass() {
		return searchClass;
	}
	
	public PrintWriter getOutputWriter() {
		try {
			return response.getWriter();
		} catch (IOException ex) {
			logger.log(Level.SEVERE, "Could not get output writer", ex);
		}
		return null;
	}
	
	public void setInBody(final boolean inBody) {
		this.inBody = inBody;
	}
	
	public boolean inBody() {
		return inBody;
	}
	
	public void setAppLibRendered(final boolean appLibRendered) {
		this.appLibRendered = appLibRendered;
	}
	
	public boolean appLibRendered() {
		return appLibRendered;
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
	public Result getResult() {
		return result;
	}
}
