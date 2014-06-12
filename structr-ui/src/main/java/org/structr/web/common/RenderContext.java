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

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.Ownership;
import org.structr.core.Result;
import org.structr.core.app.StructrApp;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Principal;
import org.structr.core.entity.relationship.PrincipalOwnsNode;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.RelationProperty;
import org.structr.rest.ResourceProvider;
import org.structr.schema.action.ActionContext;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.web.entity.html.relation.ResourceLink;

/**
 * Holds information about the context in which a resource is rendered,
 * like f.e. edit mode, language
 *
 * @author Axel Morgner
 */


public class RenderContext extends ActionContext {

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
	 * @return
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

	// ----- interface ActionContext -----
	@Override
	public Object getReferencedProperty(final SecurityContext securityContext, final GraphObject entity, final String refKey) throws FrameworkException {

		final String DEFAULT_VALUE_SEP = "!";
		final String[] parts           = refKey.split("[\\.]+");
		String referenceKey            = parts[parts.length - 1];
		String defaultValue            = null;

		if (StringUtils.contains(referenceKey, DEFAULT_VALUE_SEP)) {

			String[] ref = StringUtils.split(referenceKey, DEFAULT_VALUE_SEP);
			referenceKey = ref[0];
			if (ref.length > 1) {

				defaultValue = ref[1];

			} else {

				defaultValue = "";
			}
		}

		Page _page = this.getPage();
		GraphObject _data = null;

		// walk through template parts
		for (int i = 0; (i < parts.length); i++) {

			String part = parts[i];

			if (_data != null) {

				Object value = _data.getProperty(StructrApp.getConfiguration().getPropertyKeyForJSONName(_data.getClass(), part));

				if (value instanceof GraphObject) {
					_data = (GraphObject) value;

					continue;

				}

				// special keyword "size"
				if (i > 0 && "size".equals(part)) {

					Object val = _data.getProperty(StructrApp.getConfiguration().getPropertyKeyForJSONName(_data.getClass(), parts[i - 1]));

					if (val instanceof List) {

						return ((List) val).size();

					}

				}

				// special keyword "link", works on deeper levels, too
				if ("link".equals(part) && _data instanceof AbstractNode) {

					ResourceLink rel = ((AbstractNode) _data).getOutgoingRelationship(ResourceLink.class);
					if (rel != null) {

						_data = rel.getTargetNode();

						break;

					}

					continue;

				}

				if (value == null) {

					// check for default value
					if (defaultValue != null && StringUtils.contains(refKey, "!")) {
						return numberOrString(defaultValue);
					}

					// Need to return null here to avoid _data sticking to the (wrong) parent object
					return null;

				}

			}

			// data objects from parent elements
			if (this.hasDataForKey(part)) {

				_data = this.getDataNode(part);

				if (parts.length == 1) {
					return _data;
				}

				continue;

			}

			// special keyword "request"
			if ("request".equals(part)) {

				final HttpServletRequest request = this.getRequest(); //securityContext.getRequest();
				if (request != null) {

					if (StringUtils.contains(refKey, "!")) {

						return numberOrString(StringUtils.defaultIfBlank(request.getParameter(referenceKey), defaultValue));

					} else {

						return numberOrString(StringUtils.defaultString(request.getParameter(referenceKey)));
					}
				}

			}

			// special keyword "now":
			if ("now".equals(part)) {

				return new Date();
			}

			// special keyword "me"
			if ("me".equals(part)) {

				Principal me = (Principal) securityContext.getUser(false);

				if (me != null) {

					_data = me;

					if (parts.length == 1) {
						return _data;
					}

					continue;
				}

			}

			// special boolean keywords
			if ("true".equals(part)) {
				return true;
			}

			if ("false".equals(part)) {
				return false;
			}

			// the following keywords work only on root level
			// so that they can be used as property keys for data objects
			if (_data == null) {

				// details data object id
				if ("id".equals(part)) {

					GraphObject detailsObject = this.getDetailsDataObject();

					if (detailsObject != null) {
						return detailsObject.getUuid();
					}

				}

				// details data object
				if ("current".equals(part)) {

					GraphObject detailsObject = this.getDetailsDataObject();

					if (detailsObject != null) {

						_data = detailsObject;

						if (parts.length == 1) {
							return _data;
						}

						continue;
					}

				}

				// special keyword "this"
				if ("this".equals(part)) {

					_data = this.getDataObject();

					if (parts.length == 1) {
						return _data;
					}

					continue;

				}

				// special keyword "element"
				if ("element".equals(part)) {

					_data = entity;

					if (parts.length == 1) {
						return _data;
					}

					continue;

				}

				// special keyword "ownerDocument", works only on root level
				if ("page".equals(part)) {

					_data = _page;

					if (parts.length == 1) {
						return _data;
					}

					continue;

				}

				// special keyword "link"
				if (entity instanceof NodeInterface && "link".equals(part)) {

					ResourceLink rel = ((NodeInterface)entity).getOutgoingRelationship(ResourceLink.class);

					if (rel != null) {
						_data = rel.getTargetNode();

						if (parts.length == 1) {
							return _data;
						}

						continue;
					}

				}

				// special keyword "parent"
				if ("parent".equals(part)) {

					_data = (DOMNode) ((DOMNode)entity).getParentNode();

					if (parts.length == 1) {
						return _data;
					}

					continue;

				}

				// special keyword "owner"
				if (entity instanceof NodeInterface && "owner".equals(part)) {

					Ownership rel = ((NodeInterface)entity).getIncomingRelationship(PrincipalOwnsNode.class);
					if (rel != null) {

						_data = rel.getSourceNode();

						if (parts.length == 1) {
							return _data;
						}
					}

					continue;

				}

				// special keyword "result_size"
				if ("result_count".equals(part) || "result_size".equals(part)) {

					Result result = this.getResult();

					if (result != null) {

						return result.getRawResultCount();

					}

				}

				// special keyword "page_size"
				if ("page_size".equals(part)) {

					Result result = this.getResult();

					if (result != null) {

						return result.getPageSize();

					}

				}

				// special keyword "page_count"
				if ("page_count".equals(part)) {

					Result result = this.getResult();

					Integer pageCount = result.getPageCount();

					if (pageCount != null) {
						return pageCount;
					} else {
						return 1;
					}

				}

				// special keyword "page_no"
				if ("page_no".equals(part)) {

					Result result = this.getResult();

					if (result != null) {

						return result.getPage();

					}

				}

			}

		}

		if (_data != null) {

			PropertyKey referenceKeyProperty = StructrApp.getConfiguration().getPropertyKeyForJSONName(_data.getClass(), referenceKey);
			//return getEditModeValue(securityContext, renderContext, _data, referenceKeyProperty, defaultValue);
			Object value = _data.getProperty(referenceKeyProperty);

			PropertyConverter converter = referenceKeyProperty.inputConverter(securityContext);

			if (value != null && converter != null && !(referenceKeyProperty instanceof RelationProperty)) {
				value = converter.revert(value);
			}

			return value != null ? value : defaultValue;

		}

		// check for default value
		if (defaultValue != null && StringUtils.contains(refKey, "!")) {
			return numberOrString(defaultValue);
		}

		return null;

	}

	@Override
	public boolean returnRawValue(final SecurityContext securityContext) {
		return ((EditMode.RAW.equals(getEditMode(securityContext.getUser(false)))));
	}

	public boolean hasTimeout(final long timeout) {
		return System.currentTimeMillis() > (renderStartTime + timeout);
	}
}
