/*
 * Copyright (C) 2010-2021 Structr GmbH
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
import org.structr.api.config.Settings;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.entity.Principal;
import org.structr.core.property.PropertyKey;
import org.structr.core.script.Scripting;
import org.structr.rest.ResourceProvider;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;
import org.structr.web.entity.LinkSource;
import org.structr.web.entity.dom.Content;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.web.entity.dom.Template;

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
	private AsyncBuffer buffer                         = null;
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
	private boolean anyChildNodeCreatesNewLine         = false;
	private boolean indentHtml                         = true;

	public enum EditMode {

		NONE, WIDGET, CONTENT, RAW, DEPLOYMENT, SHAPES, SHAPES_MINIATURES;

	}

	public RenderContext(final SecurityContext securityContext) {

		super(securityContext);

		this.buffer = new AsyncBuffer();

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
		this.editMode                   = other.editMode;
		this.inBody                     = other.inBody;
		this.appLibRendered             = other.appLibRendered;
		this.detailsDataObject          = other.detailsDataObject;
		this.currentDataObject          = other.currentDataObject;
		this.sourceDataObject           = other.sourceDataObject;
		this.listSource                 = other.listSource;
		this.relatedProperty            = other.relatedProperty;
		this.page                       = other.page;
		this.request                    = other.request;
		this.response                   = other.response;
		this.resourceProvider           = other.resourceProvider;
		this.anyChildNodeCreatesNewLine = other.anyChildNodeCreatesNewLine;
		this.locale                     = other.locale;
		this.indentHtml                 = other.indentHtml;
		this.buffer                     = other.buffer;

	}

	public RenderContext(final SecurityContext securityContext, final HttpServletRequest request, HttpServletResponse response, final EditMode editMode) {

		super(securityContext);

		this.buffer   = new AsyncBuffer();
		this.request  = request;
		this.response = response;
		this.editMode = editMode;

		readConfigParameters();

		// force indentation for deployment mode
		if (EditMode.DEPLOYMENT.equals(this.editMode)) {
			this.indentHtml = true;
		}

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
		return (user == null || Boolean.FALSE.equals(user.isAdmin())) ? EditMode.NONE : editMode;
	}

	public static EditMode getValidatedEditMode(final Principal user, final String editModeString) {
		return (user == null || Boolean.FALSE.equals(user.isAdmin())) ? EditMode.NONE : editMode(editModeString);
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

			case "5":

				edit = EditMode.SHAPES;
				break;

			case "6":

				edit = EditMode.SHAPES_MINIATURES;
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

	public void setAnyChildNodeCreatesNewLine(final boolean anyChildNodeCreatesNewLine) {
		this.anyChildNodeCreatesNewLine = anyChildNodeCreatesNewLine;
	}

	public boolean getAnyChildNodeCreatesNewLine() {
		return anyChildNodeCreatesNewLine;
	}

	public boolean shouldIndentHtml() {
		return indentHtml;
	}

	public String getRequestParameter(final String name) {

		if (request != null) {

			return request.getParameter(name);
		}

		return null;
	}

	@Override
	public boolean returnRawValue() {
		final EditMode editMode = getEditMode(securityContext.getUser(false));
		return ((EditMode.RAW.equals(editMode) || EditMode.WIDGET.equals(editMode)));
	}

	@Override
	public Object evaluate(final GraphObject entity, final String key, final Object data, final String defaultValue, final int depth) throws FrameworkException {

		// data key can only be used as the very first token
		if (depth == 0 && hasDataForKey(key)) {
			return getDataNode(key);
		}

		// evaluate non-ui specific context
		final Object value = super.evaluate(entity, key, data, defaultValue, depth);
		if (value == null) {

			if (data != null) {

				switch (key) {

					// link has two different meanings
					case "link":

						if (data instanceof LinkSource) {

							final LinkSource linkSource = (LinkSource)data;
							return linkSource.getLinkable();
						}
						break;
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

						if (entity instanceof LinkSource) {

							final LinkSource linkSource = (LinkSource)entity;
							return linkSource.getLinkable();
						}
						break;
				}
			}
		}

		return value;
	}

	@Override
	public boolean isRenderContext() {
		return true;
	}

	@Override
	public void print(final Object[] objects, final Object caller) {

		if ((caller instanceof Template) || (caller instanceof Content)) {

			for (final Object obj : objects) {

				if (obj != null) {

					this.buffer.append(Scripting.formatToDefaultDateOrString(obj));
				}
			}

		} else {

			super.print(objects, null);
		}
	}

	// ----- private methods -----
	private void readConfigParameters () {
		indentHtml = Settings.HtmlIndentation.getValue();
	}
}
