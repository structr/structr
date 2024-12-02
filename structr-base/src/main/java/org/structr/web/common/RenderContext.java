/*
 * Copyright (C) 2010-2024 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.web.common;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.codec.binary.Base64InputStream;
import org.apache.commons.codec.binary.Base64OutputStream;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.structr.api.config.Settings;
import org.structr.common.RequestKeywords;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.PrincipalInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.script.Scripting;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.EvaluationHints;
import org.structr.schema.action.Function;
import org.structr.web.entity.LinkSource;
import org.structr.web.entity.dom.Content;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.web.entity.dom.Template;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;

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
	private boolean anyChildNodeCreatesNewLine         = false;
	private boolean indentHtml                         = true;
	private boolean isPartialRendering                 = false;
	private String templateRootId                      = null;
	private String templateId                          = null;

	public enum EditMode {

		NONE, WIDGET, CONTENT, RAW, DEPLOYMENT, SHAPES, SHAPES_MINIATURES
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
		this.anyChildNodeCreatesNewLine = other.anyChildNodeCreatesNewLine;
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

		final String editString = StringUtils.defaultString(request.getParameter(RequestKeywords.EditMode.keyword()));

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
	 * to {@link RenderContext#popSecurityContext()} is made.
	 *
	 * @param securityContext
	 */
	public void pushSecurityContext(final SecurityContext securityContext) {

		scStack.push(this.getSecurityContext());
		this.setSecurityContext(securityContext);
	}

	public void popSecurityContext() {

		if (!scStack.isEmpty()) {
			this.setSecurityContext(scStack.pop());
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
	public EditMode getEditMode(final PrincipalInterface user) {
		return (user == null || Boolean.FALSE.equals(user.isAdmin())) ? EditMode.NONE : editMode;
	}

	public static EditMode getValidatedEditMode(final PrincipalInterface user, final String editModeString) {
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

	public void setIsPartialRendering(final boolean isPartialRendering) {
		this.isPartialRendering = isPartialRendering;
	}

	public boolean isPartialRendering() {
		return isPartialRendering;
	}

	public void setTemplateRootId(final String uuid) {
		this.templateRootId = uuid;
	}

	public void setTemplateId(final String uuid) {
		this.templateId = uuid;
	}

	public boolean isTemplateRoot(final String uuid) {

		if (uuid == null) {
			return false;
		}

		return uuid.equals(this.templateRootId);
	}

	public String getTemplateId() {
		return this.templateId;
	}

	public Map<String, GraphObject> getDataObjectsMap() {
		return dataObjects;
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

		final SecurityContext securityContext = getSecurityContext();
		final EditMode editMode               = getEditMode(securityContext.getUser(false));

		return EditMode.RAW.equals(editMode) || EditMode.WIDGET.equals(editMode);
	}

	@Override
	public Object evaluate(final GraphObject entity, final String key, final Object data, final String defaultValue, final int depth, final EvaluationHints hints, final int row, final int column) throws FrameworkException {

		// report usage for toplevel keys only
		if (data == null) {

			// report key as used to identify unresolved keys later
			hints.reportUsedKey(key, row, column);
		}

		// data key can only be used as the very first token
		if (depth == 0 && hasDataForKey(key)) {

			hints.reportExistingKey(key);
			return getDataNode(key);
		}

		// evaluate non-ui specific context
		final Object value = super.evaluate(entity, key, data, defaultValue, depth, hints, row, column);
		if (value == null) {

			if (data != null) {

				switch (key) {

					// link has two different meanings
					case "link":

						if (data instanceof LinkSource) {

							hints.reportExistingKey(key);
							final LinkSource linkSource = (LinkSource)data;
							return linkSource.getLinkable();
						}
						break;
				}

			} else {

				// "data-less" keywords to start the evaluation chain
				switch (key) {

					case "id":

						hints.reportExistingKey(key);
						GraphObject detailsObject = this.getDetailsDataObject();
						if (detailsObject != null) {

							return detailsObject.getUuid();

						} else if (defaultValue != null) {

							return Function.numberOrString(defaultValue);
						}
						break;

					case "current":
						hints.reportExistingKey(key);
						return getDetailsDataObject();

					case "template":

						if (entity instanceof DOMNode) {
							hints.reportExistingKey(key);
							return ((DOMNode) entity).getClosestTemplate(getPage());
						}
						break;

					case "page":
						hints.reportExistingKey(key);
						Page page = getPage();
						if (page == null && entity instanceof DOMNode) {
							page = ((DOMNode) entity).getOwnerDocument();
						}
						return page;

					case "parent":

						if (entity instanceof DOMNode) {
							hints.reportExistingKey(key);
							return ((DOMNode) entity).getParentNode();
						}
						break;

					case "children":

						if (entity instanceof DOMNode) {

							hints.reportExistingKey(key);
							return ((DOMNode) entity).getChildNodes();

						}
						break;

					// link has two different meanings
					case "link":

						if (entity instanceof LinkSource) {

							hints.reportExistingKey(key);
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

	public String getEncodedRenderState() {

		final Map<String, Object> renderState = new LinkedHashMap<>();

		for (final String dataKey : dataObjects.keySet()) {

			final GraphObject value = dataObjects.get(dataKey);
			if (value != null) {

				renderState.put(dataKey, value.getUuid());
			}
		}

		if (!renderState.isEmpty()) {

			final ByteArrayOutputStream output = new ByteArrayOutputStream();
			final Gson gson                    = new GsonBuilder().create();

			try (final OutputStreamWriter writer = new OutputStreamWriter(new Base64OutputStream(output, true, -1, null))) {

				gson.toJson(renderState, writer);

			} catch (IOException ioex) {
				ioex.printStackTrace();
			}

			return output.toString(Charset.forName("utf-8")).trim();
		}

		return null;
	}

	public void initializeFromEncodedRenderState(final String encoded) {

		final ByteArrayInputStream input = new ByteArrayInputStream(encoded.getBytes(Charset.forName("utf-8")));
		final App app                    = StructrApp.getInstance(getSecurityContext());
		final Gson gson                  = new GsonBuilder().create();

		try (final JsonReader reader = new JsonReader(new InputStreamReader(new Base64InputStream(input, false)))) {

			final Map<String, Object> state = gson.fromJson(reader, Map.class);

			for (final Entry<String, Object> entry : state.entrySet()) {

				final Object value = entry.getValue();
				if (value != null) {

					dataObjects.put(entry.getKey(), app.getNodeById(value.toString()));
				}
			}

		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	// ----- private methods -----
	private void readConfigParameters () {
		indentHtml = Settings.HtmlIndentation.getValue();
	}
}
