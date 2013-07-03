/**
 * Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 * This file is part of structr <http://structr.org>.
 *
 * structr is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * structr is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with structr. If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.rest.resource;

//~--- JDK imports ------------------------------------------------------------
import org.structr.core.graph.search.SearchAttribute;
import org.structr.core.graph.search.Search;
import org.structr.core.graph.search.DistanceSearchAttribute;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.search.BooleanClause.Occur;
import org.structr.common.CaseHelper;
import org.structr.common.GraphObjectComparator;
import org.structr.common.Permission;
import org.structr.common.PropertyView;
import org.structr.core.property.PropertyKey;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.property.PropertyMap;
import org.structr.core.EntityContext;
import org.structr.core.GraphObject;
import org.structr.core.Result;
import org.structr.core.Services;
import org.structr.core.Value;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.graph.DeleteNodeCommand;
import org.structr.core.graph.DeleteRelationshipCommand;
import org.structr.core.graph.NodeFactory;
import org.structr.core.graph.StructrTransaction;
import org.structr.core.graph.TransactionCommand;
import org.structr.rest.RestMethodResult;
import org.structr.rest.exception.IllegalPathException;
import org.structr.rest.exception.NoResultsException;
import org.structr.rest.exception.NotAllowedException;
import org.structr.rest.servlet.JsonRestServlet;

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

	//~--- methods --------------------------------------------------------
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

	public abstract Result doGet(PropertyKey sortKey, boolean sortDescending, int pageSize, int page, String offsetId) throws FrameworkException;

	public abstract RestMethodResult doPost(final Map<String, Object> propertySet) throws FrameworkException;

	public abstract RestMethodResult doHead() throws FrameworkException;

	public abstract RestMethodResult doOptions() throws FrameworkException;

	public abstract Resource tryCombineWith(Resource next) throws FrameworkException;

	// ----- methods -----
	public RestMethodResult doDelete() throws FrameworkException {

		final DeleteNodeCommand deleteNode = Services.command(securityContext, DeleteNodeCommand.class);
		final DeleteRelationshipCommand deleteRel = Services.command(securityContext, DeleteRelationshipCommand.class);
		Iterable<? extends GraphObject> results = null;

		// catch 204, DELETE must return 200 if resource is empty
		try {
			results = doGet(null, false, NodeFactory.DEFAULT_PAGE_SIZE, NodeFactory.DEFAULT_PAGE, null).getResults();
		} catch (final NoResultsException nre) {
			results = null;
		}

		if (results != null) {

			final Iterable<? extends GraphObject> finalResults = results;

			Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {
				@Override
				public Object execute() throws FrameworkException {

					for (final GraphObject obj : finalResults) {

						if (obj instanceof AbstractRelationship) {

							deleteRel.execute((AbstractRelationship)obj);

						} else if (obj instanceof AbstractNode) {

							if (!securityContext.isAllowed((AbstractNode)obj, Permission.delete)) {

								logger.log(Level.WARNING, "Could not delete {0} because {1} has no delete permission", new Object[]{obj, securityContext.getUser(true)});
								throw new NotAllowedException();

							}

							// delete cascading
							deleteNode.execute((AbstractNode)obj, true);
						}

					}

					return null;
				}
			});

		}

		return new RestMethodResult(HttpServletResponse.SC_OK);
	}

	public RestMethodResult doPut(final Map<String, Object> propertySet) throws FrameworkException {

		final Result<GraphObject> result = doGet(null, false, NodeFactory.DEFAULT_PAGE_SIZE, NodeFactory.DEFAULT_PAGE, null);
		final List<GraphObject> results = result.getResults();

		if (results != null && !results.isEmpty()) {

			final Class type = results.get(0).getClass();

			final StructrTransaction transaction = new StructrTransaction() {
				@Override
				public Object execute() throws FrameworkException {

					PropertyMap properties = PropertyMap.inputTypeToJavaType(securityContext, type, propertySet);

					for (final GraphObject obj : results) {

						for (final Entry<PropertyKey, Object> attr : properties.entrySet()) {

							obj.setProperty(attr.getKey(), attr.getValue());

						}

					}

					return null;
				}
			};

			// modify results in a single transaction
			Services.command(securityContext, TransactionCommand.class).execute(transaction);

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

	// ----- protected methods -----
	protected PropertyKey findPropertyKey(final TypedIdResource typedIdResource, final TypeResource typeResource) {

		Class sourceNodeType = typedIdResource.getTypeResource().getEntityClass();
		String rawName = typeResource.getRawType();
		PropertyKey key = EntityContext.getPropertyKeyForJSONName(sourceNodeType, rawName, false);

		if (key == null) {

			// try to convert raw name into lower-case variable name
			key = EntityContext.getPropertyKeyForJSONName(sourceNodeType, CaseHelper.toLowerCamelCase(rawName));
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

				uriBuilder.append(newObject.getId());

			} else {

				uriBuilder.append(newObject.getProperty(idProperty));

			}
		}

		return uriBuilder.toString();
	}

	protected void applyDefaultSorting(List<? extends GraphObject> list, PropertyKey sortKey, boolean sortDescending) {

		if (!list.isEmpty()) {

			PropertyKey finalSortKey = sortKey;
			String finalSortOrder = sortDescending ? "desc" : "asc";

			if (finalSortKey == null) {

				// Apply default sorting, if defined
				final GraphObject obj = list.get(0);

				final PropertyKey defaultSort = obj.getDefaultSortKey();

				if (defaultSort != null) {

					finalSortKey = defaultSort;
					finalSortOrder = obj.getDefaultSortOrder();
				}
			}

			if (finalSortKey != null) {
				Collections.sort(list, new GraphObjectComparator(finalSortKey, finalSortOrder));
			}
		}
	}

	// ----- private methods -----
	private static int parseInteger(final Object source) {

		try {
			return Integer.parseInt(source.toString());
		} catch (final Throwable t) {
		}

		return -1;
	}

	//~--- get methods ----------------------------------------------------
	public abstract String getUriPart();

	public abstract Class<? extends GraphObject> getEntityClass();

	public abstract String getResourceSignature();

	protected DistanceSearchAttribute getDistanceSearch(final HttpServletRequest request, final Set<String> validAttrs) {

		if (request != null) {

			final String distance = request.getParameter(Search.DISTANCE_SEARCH_KEYWORD);

			if (!request.getParameterMap().isEmpty() && StringUtils.isNotBlank(distance)) {

				final Double dist = Double.parseDouble(distance);
				final String location = request.getParameter(Search.LOCATION_SEARCH_KEYWORD);

				String street = request.getParameter(Search.STREET_SEARCH_KEYWORD);
				String house = request.getParameter(Search.HOUSE_SEARCH_KEYWORD);
				String postalCode = request.getParameter(Search.POSTAL_CODE_SEARCH_KEYWORD);
				String city = request.getParameter(Search.CITY_SEARCH_KEYWORD);
				String state = request.getParameter(Search.STATE_SEARCH_KEYWORD);
				String country = request.getParameter(Search.COUNTRY_SEARCH_KEYWORD);

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

				return new DistanceSearchAttribute(street, house, postalCode, city, state, country, dist, Occur.MUST);
			}
		}

		return null;
	}

	protected List<SearchAttribute> extractSearchableAttributes(final SecurityContext securityContext, final Class type, final HttpServletRequest request) throws FrameworkException {

		List<SearchAttribute> searchAttributes = new LinkedList<SearchAttribute>();
		
		if (type != null && request != null && !request.getParameterMap().isEmpty()) {

			 boolean looseSearch = parseInteger(request.getParameter(JsonRestServlet.REQUEST_PARAMETER_LOOSE_SEARCH)) == 1;

			for (final PropertyKey key : EntityContext.getPropertySet(type, PropertyView.All)) {

				if (key.isSearchableProperty()) {
					
					searchAttributes.addAll(key.extractSearchableAttribute(securityContext, request, looseSearch));
				}
			}
		}

		return searchAttributes;
	}

	public abstract boolean isCollectionResource() throws FrameworkException;

	public boolean isPrimitiveArray() {
		return false;
	}

	//~--- set methods ----------------------------------------------------
	public void setSecurityContext(final SecurityContext securityContext) {
		this.securityContext = securityContext;
	}
}
