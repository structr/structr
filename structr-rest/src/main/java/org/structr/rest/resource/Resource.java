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
package org.structr.rest.resource;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.structr.common.CaseHelper;
import org.structr.common.GraphObjectComparator;
import org.structr.common.Permission;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.Result;
import org.structr.core.Services;
import org.structr.core.Value;
import org.structr.core.app.App;
import org.structr.core.app.Query;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.graph.NodeFactory;
import org.structr.core.graph.search.SearchCommand;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.rest.RestMethodResult;
import org.structr.rest.exception.IllegalPathException;
import org.structr.rest.exception.NoResultsException;
import org.structr.rest.exception.NotAllowedException;
import org.structr.rest.servlet.JsonRestServlet;
import org.structr.schema.ConfigurationProvider;

//~--- classes ----------------------------------------------------------------
/**
 * Base class for all resource constraints. Constraints can be combined with succeeding constraints to avoid unneccesary evaluation.
 *
 *
 * @author Christian Morgner
 */
public abstract class Resource {

	private static final Logger logger = Logger.getLogger(Resource.class.getName());

	protected SecurityContext securityContext = null;
	protected PropertyKey idProperty          = null;

	public abstract Resource tryCombineWith(Resource next) throws FrameworkException;

	/**
	 * Check and configure this instance with the given values. Please note that you need to set the security context of your class in this method.
	 *
	 * @param part the uri part that matched this resource
	 * @param securityContext the security context of the current request
	 * @param request the current request
	 * @return whether this resource accepts the given uri part
	 * @throws FrameworkException
	 */
	public abstract boolean checkAndConfigure(String part, SecurityContext securityContext, HttpServletRequest request) throws FrameworkException;
	public abstract String getUriPart();
	public abstract Class<? extends GraphObject> getEntityClass();
	public abstract String getResourceSignature();
	public abstract boolean isCollectionResource() throws FrameworkException;

	public abstract Result doGet(PropertyKey sortKey, boolean sortDescending, int pageSize, int page, String offsetId) throws FrameworkException;
	public abstract RestMethodResult doPost(final Map<String, Object> propertySet) throws FrameworkException;


	public RestMethodResult doHead() throws FrameworkException {
		Thread.dumpStack();
		throw new IllegalStateException("Resource.doHead() called, this should not happen.");
	}

	public RestMethodResult doOptions() throws FrameworkException {
		return new RestMethodResult(HttpServletResponse.SC_OK);
	}

	public RestMethodResult doDelete() throws FrameworkException {

		Iterable<? extends GraphObject> results = null;

		// catch 204, DELETE must return 200 if resource is empty
		try {
			results = doGet(null, false, NodeFactory.DEFAULT_PAGE_SIZE, NodeFactory.DEFAULT_PAGE, null).getResults();
		} catch (final NoResultsException nre) {
			results = null;
		}

		if (results != null) {

			final Iterable<? extends GraphObject> finalResults = results;
			final App app                                      = StructrApp.getInstance(securityContext);

			for (final GraphObject obj : finalResults) {

				if (obj instanceof AbstractRelationship) {

					app.delete((AbstractRelationship)obj);

				} else if (obj instanceof AbstractNode) {

					if (!securityContext.isAllowed((AbstractNode)obj, Permission.delete)) {

						logger.log(Level.WARNING, "Could not delete {0} because {1} has no delete permission", new Object[]{obj, securityContext.getUser(true)});
						throw new NotAllowedException();

					}

					// delete cascading
					app.delete((AbstractNode)obj);
				}

			}
		}

		return new RestMethodResult(HttpServletResponse.SC_OK);
	}

	public RestMethodResult doPut(final Map<String, Object> propertySet) throws FrameworkException {

		final Result<GraphObject> result = doGet(null, false, NodeFactory.DEFAULT_PAGE_SIZE, NodeFactory.DEFAULT_PAGE, null);
		final List<GraphObject> results  = result.getResults();
		final App app                    = StructrApp.getInstance(securityContext);

		if (results != null && !results.isEmpty()) {

			final Class type = results.get(0).getClass();

			PropertyMap properties = PropertyMap.inputTypeToJavaType(securityContext, type, propertySet);

			for (final GraphObject obj : results) {

				for (final Entry<PropertyKey, Object> attr : properties.entrySet()) {

					obj.setProperty(attr.getKey(), attr.getValue());

				}

			}

			return new RestMethodResult(HttpServletResponse.SC_OK);

		}

		throw new IllegalPathException();
	}

	/**
	 *
	 * @param propertyView
	 */
	public void configurePropertyView(final Value<String> propertyView) {
	}

	public void configureIdProperty(PropertyKey idProperty) {
		this.idProperty = idProperty;
	}

	public void postProcessResultSet(final Result result) {
	}

	public boolean isPrimitiveArray() {
		return false;
	}

	public void setSecurityContext(final SecurityContext securityContext) {
		this.securityContext = securityContext;
	}

	/**
	 * Override this method in your resource implementation and return false
	 * to prevent the creation of an encosing transaction context in your
	 * doPost() method. Default: true.
	 *
	 * @return whether to create transaction around the doPost() method
	 */
	public boolean createPostTransaction() {
		return true;
	}

	// ----- protected methods -----
	protected PropertyKey findPropertyKey(final TypedIdResource typedIdResource, final TypeResource typeResource) {

		Class sourceNodeType = typedIdResource.getTypeResource().getEntityClass();
		String rawName = typeResource.getRawType();
		PropertyKey key = StructrApp.getConfiguration().getPropertyKeyForJSONName(sourceNodeType, rawName, false);

		if (key == null) {

			// try to convert raw name into lower-case variable name
			key = StructrApp.getConfiguration().getPropertyKeyForJSONName(sourceNodeType, CaseHelper.toLowerCamelCase(rawName), false);
		}

		return key;
	}

	protected String buildLocationHeader(final GraphObject newObject) {

		final StringBuilder uriBuilder = securityContext.getBaseURI();

		uriBuilder.append(getUriPart());
		uriBuilder.append("/");

		if (newObject != null) {

			// use configured id property
			if (idProperty == null) {

				uriBuilder.append(newObject.getUuid());

			} else {

				uriBuilder.append(newObject.getProperty(idProperty));

			}
		}

		return uriBuilder.toString();
	}

	protected void applyDefaultSorting(List<? extends GraphObject> list, PropertyKey sortKey, boolean sortDescending) {

		if (!list.isEmpty()) {

			String finalSortOrder = sortDescending ? "desc" : "asc";

			if (sortKey == null) {

				// Apply default sorting, if defined
				final GraphObject obj = list.get(0);

				final PropertyKey defaultSort = obj.getDefaultSortKey();

				if (defaultSort != null) {

					sortKey = defaultSort;
					finalSortOrder = obj.getDefaultSortOrder();
				}
			}

			if (sortKey != null) {
				Collections.sort(list, new GraphObjectComparator(sortKey, finalSortOrder));
			}
		}
	}

	protected void extractDistanceSearch(final HttpServletRequest request, final Query query) {

		if (request != null) {

			final String distance = request.getParameter(SearchCommand.DISTANCE_SEARCH_KEYWORD);

			if (!request.getParameterMap().isEmpty() && StringUtils.isNotBlank(distance)) {

				final Double dist = Double.parseDouble(distance);
				final String location = request.getParameter(SearchCommand.LOCATION_SEARCH_KEYWORD);

				String street = request.getParameter(SearchCommand.STREET_SEARCH_KEYWORD);
				String house = request.getParameter(SearchCommand.HOUSE_SEARCH_KEYWORD);
				String postalCode = request.getParameter(SearchCommand.POSTAL_CODE_SEARCH_KEYWORD);
				String city = request.getParameter(SearchCommand.CITY_SEARCH_KEYWORD);
				String state = request.getParameter(SearchCommand.STATE_SEARCH_KEYWORD);
				String country = request.getParameter(SearchCommand.COUNTRY_SEARCH_KEYWORD);

				// if location, use city and street, else use all fields that are there!
				if (location != null) {

					String[] parts = location.split("[,]+");
					switch (parts.length) {

						case 3:
							country = parts[2];	// no break here intentionally

						case 2:
							city = parts[1];	// no break here intentionally

						case 1:
							street = parts[0];
							break;

						default:
							break;
					}
				}

				query.location(street, house, postalCode, city, state, country, dist);
			}
		}
	}

	protected void extractSearchableAttributes(final SecurityContext securityContext, final Class type, final HttpServletRequest request, final Query query) throws FrameworkException {

		if (type != null && request != null && !request.getParameterMap().isEmpty()) {

			final ConfigurationProvider conf   = Services.getInstance().getConfigurationProvider();
			final List<PropertyKey> searchKeys = new LinkedList<>();

			for (final String name : request.getParameterMap().keySet()) {

				final PropertyKey key = conf.getPropertyKeyForJSONName(type, getFirstPartOfString(name));
				if (key != null) {

					if (key.isSearchable()) {

						// add to list of searchable keys
						searchKeys.add(key);

					} else if (!JsonRestServlet.commonRequestParameters.contains(name)) {

						throw new FrameworkException(400, "Search key " + name + " is not indexed.");
					}

				} else if (!JsonRestServlet.commonRequestParameters.contains(name)) {

					// exclude common request parameters here (should not throw exception)
					throw new FrameworkException(400, "Invalid search key " + name);
				}
			}

			// sort list of search keys according to their desired order
			// so that querying search attributes can use other attributes
			// to refine their partial results.
			Collections.sort(searchKeys, new PropertyKeyProcessingOrderComparator());

			for (final PropertyKey key : searchKeys) {

				// hand list of search attributes over to key
				key.extractSearchableAttribute(securityContext, request, query);
			}
		}
	}

	protected static int parseInteger(final Object source) {

		try {
			return Integer.parseInt(source.toString());
		} catch (final Throwable t) {
		}

		return -1;
	}

	// ----- private methods -----
	/**
	 * Returns the first part of the given source string when it contains a "."
	 *
	 * @param parameter
	 * @return
	 */
	private String getFirstPartOfString(final String source) {

		final int pos = source.indexOf(".");
		if (pos > -1) {

			return source.substring(0, pos);
		}

		return source;
	}

	// ----- nested classes -----
	private static class PropertyKeyProcessingOrderComparator implements Comparator<PropertyKey> {

		@Override
		public int compare(final PropertyKey key1, final PropertyKey key2) {
			return Integer.valueOf(key1.getProcessingOrderPosition()).compareTo(Integer.valueOf(key2.getProcessingOrderPosition()));
		}
	}
}
