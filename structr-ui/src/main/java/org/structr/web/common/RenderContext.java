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

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.Result;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Principal;
import org.structr.core.graph.NodeInterface;
import org.structr.core.parser.Functions;
import org.structr.core.property.PropertyKey;
import org.structr.rest.ResourceProvider;
import org.structr.schema.action.ActionContext;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.web.entity.dom.Template;
import org.structr.web.entity.html.relation.ResourceLink;

/**
 * Holds information about the context in which a resource is rendered, like
 * f.e. edit mode, language
 *
 * @author Axel Morgner
 */
public class RenderContext extends ActionContext {

	private static final Logger logger = Logger.getLogger(RenderContext.class.getName());

	private final Map<String, GraphObject> dataObjects = new LinkedHashMap<>();
	private final long renderStartTime                 = System.currentTimeMillis();
	private Locale locale                              = Locale.getDefault();
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

	public enum EditMode {

		NONE, WIDGET, CONTENT, RAW;

	}

	public RenderContext() {
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

	}

	public RenderContext(final HttpServletRequest request, HttpServletResponse response, final EditMode editMode, final Locale locale) {

		this.request = request;
		this.response = response;

		this.editMode = editMode;
		this.locale = locale;

	}

	public static RenderContext getInstance(final HttpServletRequest request, HttpServletResponse response, final Locale locale) {

		String editString = StringUtils.defaultString(request.getParameter("edit"));

		return new RenderContext(request, response, editMode(editString), locale);

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

			default:

				edit = EditMode.NONE;

		}

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
	public boolean returnRawValue(final SecurityContext securityContext) {
		EditMode editMode = getEditMode(securityContext.getUser(false));
		return ((EditMode.RAW.equals(editMode) || EditMode.WIDGET.equals(editMode)));
	}

	public boolean hasTimeout(final long timeout) {
		return System.currentTimeMillis() > (renderStartTime + timeout);
	}

	@Override
	public Object evaluate(final SecurityContext securityContext, final GraphObject entity, final String key, final Object data, final String defaultValue) throws FrameworkException {

		if (hasDataForKey(key)) {
			return getDataNode(key);
		}

		// evaluate non-ui specific context
		Object value = super.evaluate(securityContext, entity, key, data, defaultValue);
		if (value == null) {

			if (data != null) {

				switch (key) {

					// link has two different meanings
					case "link":

						if (data instanceof AbstractNode) {

							ResourceLink rel = ((AbstractNode)data).getOutgoingRelationship(ResourceLink.class);
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

							return Functions.numberOrString(defaultValue);
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

							final Template template = ((DOMNode) entity).getClosestTemplate(getPage());
							if (template != null) {

								return template.getChildNodes();
							}
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

					case "host":
						return securityContext.getRequest().getServerName();

					case "port":
						return securityContext.getRequest().getServerPort();

					case "path_info":
						return securityContext.getRequest().getPathInfo();

					case "result_count":
					case "result_size":

						Result sizeResult = this.getResult();
						if (sizeResult != null) {

							return sizeResult.getRawResultCount();
						}
						break;

					case "page_size":

						Result pageSizeResult = this.getResult();
						if (pageSizeResult != null) {

							return pageSizeResult.getPageSize();

						}
						break;

					case "page_count":

						Result pageCountResult = this.getResult();
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

						Result pageNoResult = this.getResult();
						if (pageNoResult != null) {

							return pageNoResult.getPage();
						}
						break;

				}
			}

		}

		return value;
	}
}
