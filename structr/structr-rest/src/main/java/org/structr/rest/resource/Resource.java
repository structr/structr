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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.structr.common.GraphObjectComparator;
import org.structr.common.Permission;
import org.structr.common.PropertyKey;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.InvalidSearchField;
import org.structr.core.Command;
import org.structr.core.EntityContext;
import org.structr.core.GraphObject;
import org.structr.core.Result;
import org.structr.core.Services;
import org.structr.core.Value;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.entity.RelationClass;
import org.structr.core.entity.ResourceAccess;
import org.structr.core.node.DeleteNodeCommand;
import org.structr.core.node.DeleteRelationshipCommand;
import org.structr.core.node.NodeFactory;
import org.structr.core.node.NodeService;
import org.structr.core.node.StructrTransaction;
import org.structr.core.node.TransactionCommand;
import org.structr.core.node.search.DistanceSearchAttribute;
import org.structr.core.node.search.Search;
import org.structr.core.node.search.SearchAttribute;
import org.structr.core.node.search.SearchNodeCommand;
import org.structr.core.node.search.SearchOperator;
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

	protected String idProperty               = null;
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

	public abstract Result doGet(String sortKey, boolean sortDescending, int pageSize, int page, String offsetId) throws FrameworkException;

	public abstract RestMethodResult doPost(final Map<String, Object> propertySet) throws FrameworkException;

	public abstract RestMethodResult doHead() throws FrameworkException;

	public abstract RestMethodResult doOptions() throws FrameworkException;

	public abstract Resource tryCombineWith(Resource next) throws FrameworkException;

	// ----- methods -----
	public RestMethodResult doDelete() throws FrameworkException {

		final Command deleteNode = Services.command(securityContext, DeleteNodeCommand.class);
		final Command deleteRel  = Services.command(securityContext, DeleteRelationshipCommand.class);
		Iterable<? extends GraphObject> results;

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

							deleteRel.execute(obj);

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
							deleteNode.execute(obj, true);
						}

					}

					return null;
				}

			});

		}

		return new RestMethodResult(HttpServletResponse.SC_OK);
	}

	public RestMethodResult doPut(final Map<String, Object> propertySet) throws FrameworkException {

		final Iterable<? extends GraphObject> results = doGet(null, false, NodeFactory.DEFAULT_PAGE_SIZE, NodeFactory.DEFAULT_PAGE, null).getResults();

		if (results != null) {

			final StructrTransaction transaction = new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {

					for (final GraphObject obj : results) {

						for (final Entry<String, Object> attr : propertySet.entrySet()) {

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

	public void configureIdProperty(final String idProperty) {
		this.idProperty = idProperty;
	}

	public void postProcessResultSet(final Result result) {}

	// ----- protected methods -----
	protected RelationClass findRelationClass(final TypedIdResource typedIdResource, final TypeResource typeResource) {
		return findRelationClass(typedIdResource.getTypeResource(), typeResource);
	}

	protected RelationClass findRelationClass(final TypeResource typeResource, final TypedIdResource typedIdResource) {
		return findRelationClass(typeResource, typedIdResource.getTypeResource());
	}

	protected RelationClass findRelationClass(final TypedIdResource typedIdResource1, final TypedIdResource typedIdResource2) {
		return findRelationClass(typedIdResource1.getTypeResource(), typedIdResource2.getTypeResource());
	}

	protected RelationClass findRelationClass(final TypeResource typeResource1, final TypeResource typeResource2) {

		final Class type1 = typeResource1.getEntityClass();
		final Class type2 = typeResource2.getEntityClass();

		if (type1 != null && type2 != null) {

			return EntityContext.getRelationClass(type1, type2);

		}

		if (type1 != null) {

			return EntityContext.getRelationClassForProperty(type1, typeResource2.getRawType());

		}

		return null;
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

	protected void applyDefaultSorting(final List<? extends GraphObject> list, final String sortKey, final boolean sortDescending) {

		if (!list.isEmpty()) {

			String finalSortKey   = sortKey;
			String finalSortOrder = sortDescending ? "desc" : "asc";

			if (finalSortKey == null) {

				// Apply default sorting, if defined
				final GraphObject obj = list.get(0);

				final PropertyKey defaultSort = obj.getDefaultSortKey();

				if (defaultSort != null) {

					finalSortKey   = defaultSort.name();
					finalSortOrder = obj.getDefaultSortOrder();
				}
			}

			if (finalSortKey != null) {

				Collections.sort(list, new GraphObjectComparator(finalSortKey, finalSortOrder));
			}
		}
	}

	protected ResourceAccess findGrant() throws FrameworkException {

		final Command search                         = Services.command(SecurityContext.getSuperUserInstance(), SearchNodeCommand.class);
		final String uriPart                         = EntityContext.normalizeEntityName(this.getResourceSignature());
		final List<SearchAttribute> searchAttributes = new LinkedList<SearchAttribute>();
		final AbstractNode topNode                   = null;
		final boolean includeDeletedAndHidden        = false;
		final boolean publicOnly                     = false;
		ResourceAccess grant                   = null;

		searchAttributes.add(Search.andExactType(ResourceAccess.class.getSimpleName()));
		searchAttributes.add(Search.andExactProperty(ResourceAccess.Key.signature, uriPart));

		final Result result = (Result) search.execute(topNode, includeDeletedAndHidden, publicOnly, searchAttributes);

		if (result.isEmpty()) {

			logger.log(Level.FINE, "No resource access object found for {0}", uriPart);

//                      // create new grant
//                      final Command create = Services.command(SecurityContext.getSuperUserInstance(), CreateNodeCommand.class);
//                      final Map<String, Object> newGrantAttributes = new LinkedHashMap<String, Object>();
//
//                      newGrantAttributes.put(AbstractNode.Key.type.name(), ResourceAccess.class.getSimpleName());
//                      newGrantAttributes.put(ResourceAccess.Key.signature.name(), uriPart);
//                      newGrantAttributes.put(ResourceAccess.Key.flags.name(), SecurityContext.getResourceFlags(uriPart));
//
//                      grant = (ResourceAccess)Services.command(SecurityContext.getSuperUserInstance(), TransactionCommand.class).execute(new StructrTransaction() {
//
//                              @Override public Object execute() throws FrameworkException {
//                                      return create.execute(newGrantAttributes);
//                              }
//                      });

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

	private static void checkForIllegalSearchKeys(final HttpServletRequest request, final Set<String> searchableProperties) throws FrameworkException {

		final ErrorBuffer errorBuffer = new ErrorBuffer();

		// try to identify invalid search properties and throw an exception
		for (final Enumeration<String> e = request.getParameterNames(); e.hasMoreElements(); ) {

			final String requestParameterName = e.nextElement();

			if (!searchableProperties.contains(requestParameterName) &&!NON_SEARCH_FIELDS.contains(requestParameterName)) {

				errorBuffer.add("base", new InvalidSearchField(requestParameterName));

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

	public abstract Class getEntityClass();

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


	protected List<SearchAttribute> extractSearchableAttributesForNodes(final String rawType,
	                                                                    final HttpServletRequest request) throws FrameworkException {
		return extractSearchableAttributes(rawType,
		                                   request,
		                                   NodeService.NodeIndex.fulltext.name(),
		                                   NodeService.NodeIndex.keyword.name());
	}


	protected List<SearchAttribute> extractSearchableAttributesForRelationships(final String rawType,
	                                                                            final HttpServletRequest request)
	                                                                            				throws FrameworkException {
		return extractSearchableAttributes(rawType,
		                                   request,
		                                   NodeService.RelationshipIndex.rel_fulltext.name(),
		                                   NodeService.RelationshipIndex.rel_keyword.name());
	}

	private static List<SearchAttribute> extractSearchableAttributes(final String rawType,
	                                                          final HttpServletRequest request,
	                                                          final String fulltextIndex,
	                                                          final String keywordIndex) throws FrameworkException {
		List<SearchAttribute> searchAttributes = Collections.emptyList();

		// searchable attributes
		if (rawType != null && request != null &&!request.getParameterMap().isEmpty()) {

			final boolean looseSearch = parseInteger(request.getParameter(JsonRestServlet.REQUEST_PARAMETER_LOOSE_SEARCH)) == 1;

			final Set<String> searchableProperties =
							getSearchableProperties(rawType,
							                        looseSearch,
							                        fulltextIndex,
							                        keywordIndex);

			searchAttributes = checkAndAssembleSearchAttributes(request, looseSearch, searchableProperties);

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


	private static SearchAttribute determineSearchType(final String key, final String searchValue) {

		if (StringUtils.startsWith(searchValue, "[") && StringUtils.endsWith(searchValue, "]")) {

			final String strippedValue = StringUtils.stripEnd(StringUtils.stripStart(searchValue, "["), "]");
			return Search.andMatchExactValues(key, strippedValue, SearchOperator.OR);

		} else if (StringUtils.startsWith(searchValue, "(") && StringUtils.endsWith(searchValue, ")")) {

			final String strippedValue = StringUtils.stripEnd(StringUtils.stripStart(searchValue, "("), ")");
			return Search.andMatchExactValues(key, strippedValue, SearchOperator.AND);

		} else {
			return Search.andExactProperty(key, searchValue);
		}
	}


	private static Set<String> getSearchableProperties(final String rawType, final boolean loose,
	                                                   final String looseIndexName, final String exactIndexName) {
		Set<String> searchableProperties;

		if (loose) {

			searchableProperties = EntityContext.getSearchableProperties(rawType, looseIndexName);

		} else {

			searchableProperties = EntityContext.getSearchableProperties(rawType, exactIndexName);

		}
		return searchableProperties;
	}



	private static List<SearchAttribute> checkAndAssembleSearchAttributes(final HttpServletRequest request,
	                                                                      final boolean looseSearch,
	                                                                      final Set<String> searchableProperties)
	                                                                    				  throws FrameworkException {

		List<SearchAttribute> searchAttributes = Collections.emptyList();

		if (searchableProperties != null) {

			checkForIllegalSearchKeys(request, searchableProperties);

			searchAttributes = new LinkedList<SearchAttribute>();

			for (final String key : searchableProperties) {

				String searchValue = request.getParameter(key);

				if (searchValue != null) {

					if (looseSearch) {

						// no quotes allowed in loose search queries!
						searchValue = removeQuotes(searchValue);

						searchAttributes.add(Search.andProperty(key, searchValue));
					} else {

						searchAttributes.add(determineSearchType(key, searchValue));

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
