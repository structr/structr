/*
 *  Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
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
package org.structr.web.datasource;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import org.apache.commons.collections.iterators.IteratorEnumeration;
import org.apache.commons.lang3.StringUtils;
import org.structr.common.PagingHelper;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.Result;
import org.structr.core.Value;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.NodeFactory;
import org.structr.core.property.PropertyKey;
import org.structr.rest.ResourceProvider;
import org.structr.rest.resource.Resource;
import org.structr.rest.servlet.JsonRestServlet;
import org.structr.rest.servlet.ResourceHelper;
import org.structr.web.common.GraphDataSource;
import org.structr.web.common.RenderContext;
import org.structr.web.common.UiResourceProvider;
import org.structr.web.entity.dom.DOMElement;

/**
 * List data source equivalent to a rest resource.
 *
 * TODO: This method uses code from the {@link JsonRestServlet} which should be
 * encapsulated and re-used here
 *
 */
public class RestDataSource implements GraphDataSource<List<GraphObject>> {

	private static final Logger logger = Logger.getLogger(RestDataSource.class.getName());

	@Override
	public List<GraphObject> getData(final SecurityContext securityContext, final RenderContext renderContext, AbstractNode referenceNode) throws FrameworkException {

		final String restQuery = ((DOMElement) referenceNode).getPropertyWithVariableReplacement(securityContext, renderContext, DOMElement.restQuery);
		if (restQuery == null || restQuery.isEmpty()) {
			return Collections.EMPTY_LIST;
		}

		return getData(securityContext, renderContext, restQuery);
	}

	@Override
	public List<GraphObject> getData(final SecurityContext securityContext, final RenderContext renderContext, final String restQuery) throws FrameworkException {

		Map<Pattern, Class<? extends Resource>> resourceMap = new LinkedHashMap<>();

		ResourceProvider resourceProvider = renderContext == null ? null : renderContext.getResourceProvider();
		if (resourceProvider == null) {
			try {
				resourceProvider = UiResourceProvider.class.newInstance();
			} catch (Throwable t) {
				logger.log(Level.SEVERE, "Couldn't establish a resource provider", t);
				return Collections.EMPTY_LIST;
			}
		}

		// inject resources
		resourceMap.putAll(resourceProvider.getResources());

		Value<String> propertyView = new ThreadLocalPropertyView();
		propertyView.set(securityContext, PropertyView.Ui);

		// initialize variables
		// mimic HTTP request
		HttpServletRequest request = new HttpServletRequestWrapper(renderContext == null ? securityContext.getRequest() : renderContext.getRequest()) {

			@Override
			public Enumeration<String> getParameterNames() {
				return new IteratorEnumeration(getParameterMap().keySet().iterator());
			}

			@Override
			public String getParameter(String key) {
				String[] p = getParameterMap().get(key);
				return p != null ? p[0] : null;
			}

			@Override
			public Map<String, String[]> getParameterMap() {
				String[] parts = StringUtils.split(getQueryString(), "&");
				Map<String, String[]> parameterMap = new HashMap();
				for (String p : parts) {
					String[] kv = StringUtils.split(p, "=");
					if (kv.length > 1) {
						parameterMap.put(kv[0], new String[]{kv[1]});
					}
				}
				return parameterMap;
			}

			@Override
			public String getQueryString() {
				return StringUtils.substringAfter(restQuery, "?");
			}

			@Override
			public String getPathInfo() {
				return StringUtils.substringBefore(restQuery, "?");
			}

			@Override
			public StringBuffer getRequestURL() {
				return new StringBuffer(restQuery);
			}
		};

		// update request in security context
		securityContext.setRequest(request);

		//HttpServletResponse response = renderContext.getResponse();
		Resource resource = ResourceHelper.applyViewTransformation(request, securityContext, ResourceHelper.optimizeNestedResourceChain(ResourceHelper.parsePath(securityContext, request, resourceMap, propertyView, GraphObject.id), GraphObject.id), propertyView);

		// TODO: decide if we need to rest the REST request here
		//securityContext.checkResourceAccess(request, resource.getResourceSignature(), resource.getGrant(request, response), PropertyView.Ui);
		// add sorting & paging
		String pageSizeParameter = request.getParameter(JsonRestServlet.REQUEST_PARAMETER_PAGE_SIZE);
		String pageParameter = request.getParameter(JsonRestServlet.REQUEST_PARAMETER_PAGE_NUMBER);
		String offsetId = request.getParameter(JsonRestServlet.REQUEST_PARAMETER_OFFSET_ID);
		String sortOrder = request.getParameter(JsonRestServlet.REQUEST_PARAMETER_SORT_ORDER);
		String sortKeyName = request.getParameter(JsonRestServlet.REQUEST_PARAMETER_SORT_KEY);
		boolean sortDescending = (sortOrder != null && "desc".equals(sortOrder.toLowerCase()));
		int pageSize = parseInt(pageSizeParameter, NodeFactory.DEFAULT_PAGE_SIZE);
		int page = parseInt(pageParameter, NodeFactory.DEFAULT_PAGE);
		PropertyKey sortKey = null;

		// set sort key
		if (sortKeyName != null) {

			Class<? extends GraphObject> type = resource.getEntityClass();
			if (type == null) {

				// fallback to default implementation
				// if no type can be determined
				type = AbstractNode.class;

			}
			sortKey = StructrApp.getConfiguration().getPropertyKeyForDatabaseName(type, sortKeyName);
		}

		// do action
		Result result = resource.doGet(sortKey, sortDescending, pageSize, page, offsetId);
		result.setIsCollection(resource.isCollectionResource());
		result.setIsPrimitiveArray(resource.isPrimitiveArray());

		//Integer rawResultCount = (Integer) Services.getAttribute(NodeFactory.RAW_RESULT_COUNT + Thread.currentThread().getId());
		PagingHelper.addPagingParameter(result, pageSize, page);

		List<GraphObject> res = result.getResults();

		if (renderContext != null) {
			renderContext.setResult(result);
		}

		return res != null ? res : Collections.EMPTY_LIST;

	}

	/**
	 * Tries to parse the given String to an int value, returning
	 * defaultValue on error.
	 *
	 * @param value the source String to parse
	 * @param defaultValue the default value that will be returned when parsing fails
	 * @return the parsed value or the given default value when parsing fails
	 */
	private static int parseInt(String value, int defaultValue) {

		if (value == null) {

			return defaultValue;

		}

		try {
			return Integer.parseInt(value);
		} catch (Throwable ignore) {}

		return defaultValue;
	}

	private static class ThreadLocalPropertyView extends ThreadLocal<String> implements Value<String> {

		@Override
		protected String initialValue() {
			return PropertyView.Ui;
		}

		@Override
		public void set(SecurityContext securityContext, String value) {
			set(value);
		}

		@Override
		public String get(SecurityContext securityContext) {
			return get();
		}
	}



}
