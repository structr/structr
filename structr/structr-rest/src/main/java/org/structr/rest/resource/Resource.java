/*
 *  Copyright (C) 2010-2012 Axel Morgner, structr <structr@structr.org>
 *
 *  This file is part of structr <http://structr.org>.
 *
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.structr.rest.resource;

//~--- JDK imports ------------------------------------------------------------
import org.structr.core.graph.search.SearchOperator;
import org.structr.core.graph.search.SearchNodeCommand;
import org.structr.core.graph.search.RangeSearchAttribute;
import org.structr.core.graph.search.SearchAttribute;
import org.structr.core.graph.search.Search;
import org.structr.core.graph.search.DistanceSearchAttribute;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.structr.common.GraphObjectComparator;
import org.structr.common.Permission;
import org.structr.core.property.PropertyKey;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.InvalidSearchField;
import org.structr.core.property.PropertyMap;
import org.structr.core.property.StringProperty;
import org.structr.core.EntityContext;
import org.structr.core.GraphObject;
import org.structr.core.Result;
import org.structr.core.Services;
import org.structr.core.Value;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.entity.ResourceAccess;
import org.structr.core.graph.DeleteNodeCommand;
import org.structr.core.graph.DeleteRelationshipCommand;
import org.structr.core.graph.NodeFactory;
import org.structr.core.graph.NodeService;
import org.structr.core.graph.StructrTransaction;
import org.structr.core.graph.TransactionCommand;
import org.structr.rest.RestMethodResult;
import org.structr.rest.exception.IllegalPathException;
import org.structr.rest.exception.NoResultsException;
import org.structr.rest.exception.NotAllowedException;
import org.structr.rest.servlet.JsonRestServlet;

//~--- classes ----------------------------------------------------------------

/**
 * Base class for all resource constraints. Constraints can be
 * combined with succeeding constraints to avoid unneccesary
 * evaluation.
 *
 *
 * @author Christian Morgner
 */
public abstract class Resource {

	private static final Logger logger                 = Logger.getLogger(Resource.class.getName());
	private static final Set<String> NON_SEARCH_FIELDS = new LinkedHashSet<String>();
	private static final Pattern rangeQueryPattern     = Pattern.compile("\\[(.+) TO (.+)\\]");

	//~--- static initializers --------------------------------------------

	static {

		// create static Set with non-searchable request parameters
		// to identify search requests quickly
		NON_SEARCH_FIELDS.add(JsonRestServlet.REQUEST_PARAMETER_LOOSE_SEARCH);
		NON_SEARCH_FIELDS.add(JsonRestServlet.REQUEST_PARAMETER_PAGE_NUMBER);
		NON_SEARCH_FIELDS.add(JsonRestServlet.REQUEST_PARAMETER_PAGE_SIZE);
		NON_SEARCH_FIELDS.add(JsonRestServlet.REQUEST_PARAMETER_OFFSET_ID);
		NON_SEARCH_FIELDS.add(JsonRestServlet.REQUEST_PARAMETER_SORT_KEY);
		NON_SEARCH_FIELDS.add(JsonRestServlet.REQUEST_PARAMETER_SORT_ORDER);
	}

	//~--- fields ---------------------------------------------------------

	protected PropertyKey idProperty          = null;
	protected SecurityContext securityContext = null;

	//~--- methods --------------------------------------------------------

	/**
	 * Check and configure this instance with the given values. Please note that you need to
	 * set the security context of your class in this method.
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

		final DeleteNodeCommand deleteNode        = Services.command(securityContext, DeleteNodeCommand.class);
		final DeleteRelationshipCommand deleteRel = Services.command(securityContext, DeleteRelationshipCommand.class);
		Iterable<? extends GraphObject> results   = null;

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

							if (!securityContext.isAllowed((AbstractNode) obj, Permission.delete)) {

								throw new NotAllowedException();

							}

//                                                      // 2: delete relationships
//                                                      if (obj instanceof AbstractNode) {
//
//                                                              List<AbstractRelationship> rels = ((AbstractNode) obj).getRelationships();
//
//                                                              for (AbstractRelationship rel : rels) {
//
//                                                                      deleteRel.execute(rel);
//
//                                                              }
//
//                                                      }
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
			final PropertyMap properties = PropertyMap.inputTypeToJavaType(securityContext, type, propertySet);
			
			final StructrTransaction transaction = new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {

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
	public void configurePropertyView(final Value<String> propertyView) {}

	public void configureIdProperty(PropertyKey idProperty) {
		this.idProperty = idProperty;
	}

	public void postProcessResultSet(final Result result) {}

	// ----- protected methods -----
	protected PropertyKey findPropertyKey(final TypedIdResource typedIdResource, final TypeResource typeResource) {

		Class sourceNodeType = typedIdResource.getTypeResource().getEntityClass();
		
		return EntityContext.getPropertyKeyForJSONName(sourceNodeType, typeResource.getRawType());
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
			String finalSortOrder    = sortDescending ? "desc" : "asc";
			
			if (finalSortKey == null) {

				// Apply default sorting, if defined
				final GraphObject obj = list.get(0);

				final PropertyKey defaultSort = obj.getDefaultSortKey();

				if (defaultSort != null) {

					finalSortKey   = defaultSort;
					finalSortOrder = obj.getDefaultSortOrder();
				}
			}

			if (finalSortKey != null) {

				Collections.sort(list, new GraphObjectComparator(finalSortKey, finalSortOrder));
			}
		}
	}

	protected ResourceAccess findGrant() throws FrameworkException {

		final SearchNodeCommand search               = Services.command(SecurityContext.getSuperUserInstance(), SearchNodeCommand.class);
		final String uriPart                         = EntityContext.normalizeEntityName(this.getResourceSignature());
		final List<SearchAttribute> searchAttributes = new LinkedList<SearchAttribute>();
		ResourceAccess grant                         = null;

		searchAttributes.add(Search.andExactType(ResourceAccess.class.getSimpleName()));
		searchAttributes.add(Search.andExactProperty(ResourceAccess.signature, uriPart));

		final Result result = search.execute(searchAttributes);

		if (result.isEmpty()) {

			logger.log(Level.FINE, "No resource access object found for {0}", uriPart);

		} else {

			final AbstractNode node = (AbstractNode) result.get(0);

			if (node instanceof ResourceAccess) {

				grant = (ResourceAccess) node;

			} else {

				logger.log(Level.SEVERE, "Grant for URI {0} has wrong type!", new Object[] { uriPart, node.getClass().getName() });

			}

			if (result.size() > 1) {

				logger.log(Level.SEVERE, "Found {0} grants for URI {1}!", new Object[] { result.size(), uriPart });

			}

		}

		return grant;
	}

	// ----- private methods -----
	private static int parseInteger(final Object source) {

		try {
			return Integer.parseInt(source.toString());
		} catch (final Throwable t) {}

		return -1;
	}

	private static void checkForIllegalSearchKeys(final HttpServletRequest request, Class type, final Set<PropertyKey> searchableProperties) throws FrameworkException {

		final ErrorBuffer errorBuffer = new ErrorBuffer();

		// try to identify invalid search properties and throw an exception
		for (final Enumeration<String> e = request.getParameterNames(); e.hasMoreElements(); ) {

			final String requestParameterName  = e.nextElement();
			
			final PropertyKey requestParameterKey = EntityContext.getPropertyKeyForJSONName(type, requestParameterName);

			if (!searchableProperties.contains(requestParameterKey) && !NON_SEARCH_FIELDS.contains(requestParameterName)) {

				errorBuffer.add("base", new InvalidSearchField(requestParameterKey));

			}

		}

		if (errorBuffer.hasError()) {

			throw new FrameworkException(422, errorBuffer);

		}
	}

	public static void registerNonSearchField(final String parameterName) {
		NON_SEARCH_FIELDS.add(parameterName);
	}

	//~--- get methods ----------------------------------------------------

	public abstract String getUriPart();

	public abstract Class<? extends GraphObject> getEntityClass();

	public abstract String getResourceSignature();

	public ResourceAccess getGrant() throws FrameworkException {
		return findGrant();
	}

	protected DistanceSearchAttribute getDistanceSearch(final HttpServletRequest request) {

		final String distance = request.getParameter(Search.DISTANCE_SEARCH_KEYWORD);

		if (request != null &&!request.getParameterMap().isEmpty() && StringUtils.isNotBlank(distance)) {

			final Double dist				= Double.parseDouble(distance);
			final StringBuilder searchKey	= new StringBuilder();
			final Enumeration names			= request.getParameterNames();

			while (names.hasMoreElements()) {

				final String name = (String) names.nextElement();

				if (!name.equals(Search.DISTANCE_SEARCH_KEYWORD)
					&& !name.equals(JsonRestServlet.REQUEST_PARAMETER_LOOSE_SEARCH)
					&& !name.equals(JsonRestServlet.REQUEST_PARAMETER_PAGE_SIZE)
					&& !name.equals(JsonRestServlet.REQUEST_PARAMETER_PAGE_NUMBER)
					&& !name.equals(JsonRestServlet.REQUEST_PARAMETER_SORT_KEY)
					&& !name.equals(JsonRestServlet.REQUEST_PARAMETER_SORT_ORDER)
					&& !name.equals(JsonRestServlet.REQUEST_PARAMETER_OFFSET_ID)
					) {

					searchKey.append(request.getParameter(name)).append(" ");

				}

			}

			return new DistanceSearchAttribute(searchKey.toString(), dist, SearchOperator.AND);

		}

		return null;
	}


	protected List<SearchAttribute> extractSearchableAttributesForNodes(final SecurityContext securityContext,
	                                                                    final Class type,
	                                                                    final HttpServletRequest request) throws FrameworkException {
		return extractSearchableAttributes(securityContext,
		                                   type,
		                                   request,
		                                   NodeService.NodeIndex.fulltext.name(),
		                                   NodeService.NodeIndex.keyword.name());
	}

	protected List<SearchAttribute> extractSearchableAttributesForRelationships(final SecurityContext securityContext,
	                                                                            final Class type,
	                                                                            final HttpServletRequest request)
	                                                                            				throws FrameworkException {
		return extractSearchableAttributes(securityContext,
		                                   type,
		                                   request,
		                                   NodeService.RelationshipIndex.rel_fulltext.name(),
		                                   NodeService.RelationshipIndex.rel_keyword.name());
	}

	private static List<SearchAttribute> extractSearchableAttributes(final SecurityContext securityContext,
	                                                                 final Class type,
	                                                                 final HttpServletRequest request,
	                                                                 final String fulltextIndex,
	                                                                 final String keywordIndex) throws FrameworkException {
		List<SearchAttribute> searchAttributes = Collections.emptyList();

		// searchable attributes
		if (type != null && request != null &&!request.getParameterMap().isEmpty()) {

			final boolean looseSearch = parseInteger(request.getParameter(JsonRestServlet.REQUEST_PARAMETER_LOOSE_SEARCH)) == 1;

			final Set<PropertyKey> searchableProperties =
							getSearchableProperties(type,
							                        looseSearch,
							                        fulltextIndex,
							                        keywordIndex);

			searchAttributes = checkAndAssembleSearchAttributes(securityContext, request, type, looseSearch, searchableProperties);

		}
		return searchAttributes;

	}


	private static String removeQuotes(final String searchValue) {
		String resultStr = searchValue;

		if (resultStr.contains("\"")) {
			resultStr = resultStr.replaceAll("[\"]+", "");
		}

		if (resultStr.contains("'")) {
			resultStr = resultStr.replaceAll("[']+", "");
		}

		return resultStr;
	}


	private static SearchAttribute determineSearchType(final SecurityContext securityContext, final PropertyKey key, final String searchValue) {

		if (StringUtils.startsWith(searchValue, "[") && StringUtils.endsWith(searchValue, "]")) {

			// check for existance of range query string
			Matcher matcher = rangeQueryPattern.matcher(searchValue);
			if (matcher.matches()) {

				if (matcher.groupCount() == 2) {
					
					String rangeStart = matcher.group(1);
					String rangeEnd   = matcher.group(2);
					
					try {
					
						PropertyConverter inputConverter = key.inputConverter(securityContext);
						PropertyConverter databaseConverter  = key.databaseConverter(securityContext, null);
						Object rangeStartConverted       = rangeStart;
						Object rangeEndConverted         = rangeEnd;
						
						if (inputConverter != null) {
							
							rangeStartConverted = inputConverter.convert(rangeStartConverted);
							rangeEndConverted   = inputConverter.convert(rangeEndConverted);
						}
						
						if (databaseConverter != null) {
							
							rangeStartConverted = databaseConverter.convert(rangeStartConverted);
							rangeEndConverted   = databaseConverter.convert(rangeEndConverted);
						}
						
						return new RangeSearchAttribute(key, rangeStartConverted, rangeEndConverted, SearchOperator.AND);
						
					} catch(Throwable t) {
						
						t.printStackTrace();
					}
				}
				
				return null;
				
			} else {

				final String strippedValue = StringUtils.stripEnd(StringUtils.stripStart(searchValue, "["), "]");
				return Search.andMatchExactValues(key, strippedValue, SearchOperator.OR);

			}

		} else if (StringUtils.startsWith(searchValue, "(") && StringUtils.endsWith(searchValue, ")")) {

			final String strippedValue = StringUtils.stripEnd(StringUtils.stripStart(searchValue, "("), ")");
			return Search.andMatchExactValues(key, strippedValue, SearchOperator.AND);

		} else {
			return Search.andExactProperty(key, searchValue);
		}
	}

	private static Set<PropertyKey> getSearchableProperties(final Class type, final boolean loose,
	                                                        final String looseIndexName, final String exactIndexName) {
		Set<PropertyKey> searchableProperties;

		if (loose) {

			searchableProperties = EntityContext.getSearchableProperties(type, looseIndexName);

		} else {

			searchableProperties = EntityContext.getSearchableProperties(type, exactIndexName);

		}
		return searchableProperties;
	}



	private static List<SearchAttribute> checkAndAssembleSearchAttributes(final SecurityContext securityContext,
	                                                                      final HttpServletRequest request,
									      final Class type,
	                                                                      final boolean looseSearch,
	                                                                      final Set<PropertyKey> searchableProperties)
	                                                                    				  throws FrameworkException {

		List<SearchAttribute> searchAttributes = Collections.emptyList();

		if (searchableProperties != null) {

			checkForIllegalSearchKeys(request, type, searchableProperties);

			searchAttributes = new LinkedList<SearchAttribute>();

			for (final PropertyKey key : searchableProperties) {

				String searchValue = request.getParameter(key.jsonName());

				if (searchValue != null) {

					if (looseSearch) {

						// no quotes allowed in loose search queries!
						searchValue = removeQuotes(searchValue);

						searchAttributes.add(Search.andProperty(key, searchValue));
					} else {

						searchAttributes.add(determineSearchType(securityContext, key, searchValue));

					}

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
