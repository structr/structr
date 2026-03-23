/*
 * Copyright (C) 2010-2026 Structr GmbH
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
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.api.util.Iterables;
import org.structr.common.RequestParameters;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.datasources.Channel;
import org.structr.core.entity.DataAdapter;
import org.structr.core.entity.Principal;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.script.Scripting;
import org.structr.core.traits.StructrTraits;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;
import org.structr.web.entity.LinkSource;
import org.structr.web.entity.dom.DOMElement;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.web.entity.event.ActionMapping;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Map.Entry;

/**
 * Holds information about the context in which a resource is rendered, like
 * f.e. edit mode
 *
 *
 */
public class RenderContext extends ActionContext {

	private final Map<String, GraphObject> dataObjects = new HashMap<>();
	private final Stack<SecurityContext> scStack       = new Stack<>();
	private final Map<String, Object> theme            = new LinkedHashMap<>();
	private DOMNode currentComponent                   = null;
	private Channel currentDataSource                  = null;
	private DataAdapter currentAdapter                 = null;
	private String currentReloadBehaviour              = null;
	private EditMode editMode                          = EditMode.NONE;
	private AsyncBuffer buffer                         = null;
	private int depth                                  = 0;
	private boolean inBody                             = false;
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

		NONE, WIDGET, CONTENT, RAW, DEPLOYMENT, PREVIEW
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
		this.theme.putAll(other.theme);

		this.editMode                   = other.editMode;
		this.inBody                     = other.inBody;
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

		final String editString = StringUtils.defaultString(request.getParameter(RequestParameters.EditMode.getName()));

		return new RenderContext(securityContext, request, response, editMode(editString));

	}

	public void setDetailsDataObject(final GraphObject detailsDataObject) {
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

		switch (editString) {

			case "1": return EditMode.WIDGET;
			case "2": return EditMode.CONTENT;
			case "3": return EditMode.RAW;
			case "4": return EditMode.DEPLOYMENT;
			case "5": return EditMode.PREVIEW;
			default: return EditMode.NONE;
		}
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

	public GraphObject getDataNode(final String key) {
		return dataObjects.get(key);
	}

	public void putDataObject(final String key, final GraphObject currentDataObject) {
		dataObjects.put(key, currentDataObject);
		setDataObject(currentDataObject);

	}

	public void clearDataObject(final String key) {
		dataObjects.remove(key);
		setDataObject(null);
	}

	public boolean hasDataForKey(final String key) {
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
	public Object evaluate(final GraphObject entity, final String key, final Object data, final String defaultValue, final int depth, final GraphObject contextObject, final int row, final int column) throws FrameworkException {

		// data key can only be used as the very first token
		if (depth == 0 && hasDataForKey(key)) {

			return getDataNode(key);
		}

		// evaluate non-ui specific context
		final Object value = super.evaluate(entity, key, data, defaultValue, depth, contextObject, row, column);
		if (value == null) {

			if (data != null) {

				switch (key) {

					// link has two different meanings
					case "link":

						if (data instanceof NodeInterface node && node.is(StructrTraits.LINK_SOURCE)) {

							final LinkSource linkSource = node.as(LinkSource.class);
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

					case "theme":
						return this.theme;

					case "template":

						if (entity.is(StructrTraits.DOM_NODE)) {
							return entity.as(DOMNode.class).getClosestTemplate(getPage());
						}
						break;

					case "component":

						if (currentComponent != null) {
							return currentComponent;
						}
						return resolveClosestComponent(entity);

					case "dataSource":

						// provide access to the current data source in render templates
						// that are included via renderEach or renderFields
						if (currentDataSource != null) {
							return currentDataSource;
						}

						final DOMNode forDataSource = resolveClosestComponent(entity);
						if (forDataSource != null) {

							return forDataSource.getComponentConfiguration().getDataSource();
						}
						break;


					case "adapter":

						// provide access to the current adapter in render templates
						// that are included via renderEach or renderFields
						if (currentAdapter != null) {
							return currentAdapter;
						}

						final DOMNode forAdapter = resolveClosestComponent(entity);
						if (forAdapter != null) {

							return forAdapter.getComponentConfiguration().getDataAdapter();
						}

						LoggerFactory.getLogger(RenderContext.class).warn("{} with UUID {} has no data adapter in its context or any of its parents.", entity.getType(), entity.getUuid());
						break;

					case "page":
						Page page = getPage();
						if (page == null && entity.is(StructrTraits.DOM_NODE)) {
							page = entity.as(DOMNode.class).getOwnerDocument();
						}
						return page;

					case "parent":

						if (entity.is(StructrTraits.DOM_NODE)) {
							return entity.as(DOMNode.class).getParent();
						}
						break;

					case "children":

						if (entity.is(StructrTraits.DOM_NODE)) {

							return Iterables.toList(entity.as(DOMNode.class).getChildren());

						}
						break;

					// link has two different meanings
					case "link":

						if (entity.is(StructrTraits.LINK_SOURCE)) {

							final LinkSource linkSource = entity.as(LinkSource.class);

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

		if (caller instanceof NodeInterface n && (n.is(StructrTraits.TEMPLATE) || n.is(StructrTraits.CONTENT))) {

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

		final Map<String, Object> renderState = new HashMap<>();

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

			return output.toString(StandardCharsets.UTF_8).trim();
		}

		return null;
	}

	public void initializeFromEncodedRenderState(final String encoded) {

		final ByteArrayInputStream input = new ByteArrayInputStream(encoded.getBytes(StandardCharsets.UTF_8));
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

	public Map<String, Object> getTheme() {
		return theme;
	}

	public void setCurrentAdapter(final DataAdapter newDataSource) {
		this.currentAdapter = newDataSource;
	}

	public DataAdapter getCurrentAdapter() {
		return currentAdapter;
	}

	public void setCurrentReloadBehaviour(final String reloadBehaviour) {
		this.currentReloadBehaviour = reloadBehaviour;
	}

	public String getCurrentReloadBehaviour() {
		return currentReloadBehaviour;
	}

	public void setCurrentDataSource(final Channel newDataSource) {
		this.currentDataSource = newDataSource;
	}

	public Channel getCurrentDataSource() {
		return currentDataSource;
	}

	public void setPossibleCurrentComponent(final Object candidate) {

		if (candidate == null && candidate instanceof GraphObject g && g.is(StructrTraits.DOM_NODE)) {
			this.currentComponent = g.as(DOMNode.class).getClosestComponent();
		}
	}

	public void setCurrentComponent(final DOMNode currentComponent) {
		this.currentComponent = currentComponent;
	}

	public DOMNode getCurrentComponent() {
		return currentComponent;
	}

	public String getChannelValue(final String name) {

		if (name != null) {

			switch (name) {

				case "current":
					if (detailsDataObject != null) {

						return detailsDataObject.getUuid();
					}
					break;

				default:
					final HttpServletRequest request = getSecurityContext().getRequest();
					if (request != null) {
						return request.getParameter(name);
					}
			}
		}

		return null;
	}

	// ----- private methods -----
	private void readConfigParameters () {
		indentHtml = Settings.HtmlIndentation.getValue();
	}

	private DOMNode resolveClosestComponent(final GraphObject entity) {

		if (entity.is(StructrTraits.DOM_NODE)) {

			return entity.as(DOMNode.class).getClosestComponent();
		}

		// ActionMapping can have a closest data source via DOMElements
		if (entity.is(StructrTraits.ACTION_MAPPING)) {

			final ActionMapping actionMapping = entity.as(ActionMapping.class);

			for (final DOMElement element : actionMapping.getTriggerElements()) {

				final DOMNode component = element.as(DOMNode.class).getClosestComponent();
				if (component != null) {

					return component;
				}
			}
		}

		return null;
	}
}
