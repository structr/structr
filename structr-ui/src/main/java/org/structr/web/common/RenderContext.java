/**
 * Copyright (C) 2010-2016 Structr GmbH
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
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.web.common;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Stack;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.Result;
import org.structr.core.Services;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Principal;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;
import org.structr.rest.ResourceProvider;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.web.entity.html.relation.ResourceLink;

/**
 * Holds information about the context in which a resource is rendered, like
 * f.e. edit mode
 *
 *
 */
public class RenderContext extends ActionContext {

	private final Map<String, GraphObject> dataObjects = new LinkedHashMap<>();
	private final Stack<SecurityContext> scStack       = new Stack<>();
	private EditMode editMode                          = EditMode.NONE;
	private AsyncBuffer buffer                         = new AsyncBuffer();
	private int depth                                  = 0;
	private boolean inBody                             = false;
	private boolean appLibRendered                     = false;
	private GraphObject detailsDataObject              = null;
	private GraphObject currentDataObject              = null;
	private GraphObject sourceDataObject               = null;
	private Iterable<GraphObject> listSource           = null;
	private PropertyKey relatedProperty                = null;
	private Page page                                  = null;
	private HttpServletRequest request                 = null;
	private HttpServletResponse response               = null;
	private ResourceProvider resourceProvider          = null;
	private Result result                              = null;
	private boolean anyChildNodeCreatesNewLine         = false;
	private boolean indentHtml                         = true;

	public enum EditMode {

		NONE, WIDGET, CONTENT, RAW, DEPLOYMENT;

	}

	public RenderContext(final SecurityContext securityContext) {
		super(securityContext);

		readConfigParameters();
	}

	/**
	 * Create a copy of this render context with a clean buffer.
	 *
	 * @param other The render context to copy from
	 */
	public RenderContext(final RenderContext other) {

		super(other);

		this.dataObjects.putAll(other.dataObjects);
		this.editMode = other.editMode;
		this.inBody = other.inBody;
		this.appLibRendered = other.appLibRendered;
		this.detailsDataObject = other.detailsDataObject;
		this.currentDataObject = other.currentDataObject;
		this.sourceDataObject = other.sourceDataObject;
		this.listSource = other.listSource;
		this.relatedProperty = other.relatedProperty;
		this.page = other.page;
		this.request = other.request;
		this.response = other.response;
		this.resourceProvider = other.resourceProvider;
		this.result = other.result;
		this.anyChildNodeCreatesNewLine = other.anyChildNodeCreatesNewLine;
		this.locale = other.locale;
		this.indentHtml = other.indentHtml;

	}

	public RenderContext(final SecurityContext securityContext, final HttpServletRequest request, HttpServletResponse response, final EditMode editMode) {

		super(securityContext);

		this.request = request;
		this.response = response;

		this.editMode = editMode;

		readConfigParameters();

	}

	public static RenderContext getInstance(final SecurityContext securityContext, final HttpServletRequest request, HttpServletResponse response) {

		final String editString = StringUtils.defaultString(request.getParameter("edit"));

		return new RenderContext(securityContext, request, response, editMode(editString));

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
	 * Pushes the current security context on the stack of security
	 * contexts and installs the given security context until a call
	 * to {@link popSecurityContext} is made.
	 *
	 * @param securityContext
	 */
	public void pushSecurityContext(final SecurityContext securityContext) {

		scStack.push(this.securityContext);
		this.securityContext = securityContext;
	}

	public void popSecurityContext() {

		if (!scStack.isEmpty()) {
			this.securityContext = scStack.pop();
		}
	}

	/**
	 * Return edit mode.
	 *
	 * If no user is logged in, the edit mode is always NONE to disable
	 * editing for public sessions.
	 *
	 * @param user
	 * @return edit mode
	 */
	public EditMode getEditMode(final Principal user) {
		return user == null ? EditMode.NONE : editMode;
	}

	public void setEditMode(final EditMode edit) {
		this.editMode = edit;
	}

	public static EditMode editMode(final String editString) {

		EditMode edit;

		switch (editString) {

			case "1":

				edit = EditMode.WIDGET;
				break;

			case "2":

				edit = EditMode.CONTENT;
				break;

			case "3":

				edit = EditMode.RAW;
				break;

			case "4":

				edit = EditMode.DEPLOYMENT;
				break;

			default:

				edit = EditMode.NONE;

		}

		return edit;

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

	public void setBuffer(final AsyncBuffer buffer) {
		this.buffer = buffer;
	}

	public AsyncBuffer getBuffer() {
		return buffer;
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

	public Result getResult() {
		return result;
	}

	public void setAnyChildNodeCreatesNewLine(final boolean anyChildNodeCreatesNewLine) {
		this.anyChildNodeCreatesNewLine = anyChildNodeCreatesNewLine;
	}

	public boolean getAnyChildNodeCreatesNewLine() {
		return anyChildNodeCreatesNewLine;
	}

	@Override
	public boolean returnRawValue() {
		final EditMode editMode = getEditMode(securityContext.getUser(false));
		return ((EditMode.RAW.equals(editMode) || EditMode.WIDGET.equals(editMode)));
	}

	@Override
	public Object evaluate(final GraphObject entity, final String key, final Object data, final String defaultValue) throws FrameworkException {

		if (hasDataForKey(key)) {
			return getDataNode(key);
		}

		// evaluate non-ui specific context
		final Object value = super.evaluate(entity, key, data, defaultValue);
		if (value == null) {

			if (data != null) {

				switch (key) {

					// link has two different meanings
					case "link":

						if (data instanceof AbstractNode) {

							final ResourceLink rel = ((AbstractNode)data).getOutgoingRelationship(ResourceLink.class);
							if (rel != null) {

								return rel.getTargetNode();
							}

						}
				}

			} else {

				// "data-less" keywords to start the evaluation chain
				switch (key) {

					case "id":

						GraphObject detailsObject = this.getDetailsDataObject();
						if (detailsObject != null) {

							return detailsObject.getUuid();

						} else if (defaultValue != null) {

							return Function.numberOrString(defaultValue);
						}
						break;

					case "current":
						return getDetailsDataObject();

					case "template":

						if (entity instanceof DOMNode) {
							return ((DOMNode) entity).getClosestTemplate(getPage());
						}
						break;

					case "page":
						return getPage();

					case "parent":

						if (entity instanceof DOMNode) {
							return ((DOMNode) entity).getParentNode();
						}
						break;

					case "children":

						if (entity instanceof DOMNode) {

							return ((DOMNode) entity).getChildNodes();

						}
						break;

					// link has two different meanings
					case "link":

						if (entity instanceof NodeInterface) {

							final ResourceLink rel = ((NodeInterface)entity).getOutgoingRelationship(ResourceLink.class);
							if (rel != null) {

								return rel.getTargetNode();
							}
						}
						break;

					case "result_count":
					case "result_size":

						final Result sizeResult = this.getResult();
						if (sizeResult != null) {

							return sizeResult.getRawResultCount();
						}
						break;

					case "page_size":

						final Result pageSizeResult = this.getResult();
						if (pageSizeResult != null) {

							return pageSizeResult.getPageSize();

						}
						break;

					case "page_count":

						final Result pageCountResult = this.getResult();
						if (pageCountResult != null) {

							Integer pageCount = result.getPageCount();
							if (pageCount != null) {

								return pageCount;

							} else {

								return 1;
							}
						}
						break;


					case "page_no":

						final Result pageNoResult = this.getResult();
						if (pageNoResult != null) {

							return pageNoResult.getPage();
						}
						break;

				}
			}

		}

		return value;
	}

	private void readConfigParameters () {

		try {
			indentHtml = Boolean.parseBoolean(StructrApp.getConfigurationValue(Services.HTML_INDENTATION, "true"));

		} catch(Throwable t) {}

	}

	public boolean shouldIndentHtml() {
		return indentHtml;
	}
}
