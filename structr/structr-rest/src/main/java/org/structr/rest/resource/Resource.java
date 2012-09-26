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

import org.structr.core.Result;
import org.apache.commons.lang.StringUtils;

import org.structr.common.GraphObjectComparator;
import org.structr.common.PropertyKey;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.InvalidSearchField;
import org.structr.core.*;
import org.structr.core.EntityContext;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.Value;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.entity.RelationClass;
import org.structr.core.entity.ResourceAccess;
import org.structr.core.node.*;
import org.structr.core.node.StructrTransaction;
import org.structr.core.node.TransactionCommand;
import org.structr.core.node.search.*;
import org.structr.rest.RestMethodResult;
import org.structr.rest.exception.IllegalPathException;
import org.structr.rest.exception.NoResultsException;
import org.structr.rest.servlet.JsonRestServlet;

//~--- JDK imports ------------------------------------------------------------

import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.structr.common.Permission;
import org.structr.rest.exception.NotAllowedException;

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

	public abstract Result doGet(String sortKey, boolean sortDescending, int pageSize, int page) throws FrameworkException;

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
			results = doGet(null, false, -1, -1).getResults();
		} catch (NoResultsException nre) {
			results = null;
		}

		if (results != null) {

			final Iterable<? extends GraphObject> finalResults = results;

			Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {

					for (GraphObject obj : finalResults) {
						
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

		final Iterable<? extends GraphObject> results = doGet(null, false, -1, -1).getResults();

		if (results != null) {

			StructrTransaction transaction = new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {

					for (GraphObject obj : results) {

						for (Entry<String, Object> attr : propertySet.entrySet()) {

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
	public void configurePropertyView(Value<String> propertyView) {}

	public void configureIdProperty(String idProperty) {
		this.idProperty = idProperty;
	}

	public void postProcessResultSet(Result result) {}

	// ----- protected methods -----
	protected RelationClass findRelationClass(TypedIdResource typedIdResource, TypeResource typeResource) {
		return findRelationClass(typedIdResource.getTypeResource(), typeResource);
	}

	protected RelationClass findRelationClass(TypeResource typeResource, TypedIdResource typedIdResource) {
		return findRelationClass(typeResource, typedIdResource.getTypeResource());
	}

	protected RelationClass findRelationClass(TypedIdResource typedIdResource1, TypedIdResource typedIdResource2) {
		return findRelationClass(typedIdResource1.getTypeResource(), typedIdResource2.getTypeResource());
	}

	protected RelationClass findRelationClass(TypeResource typeResource1, TypeResource typeResource2) {

		Class type1 = typeResource1.getEntityClass();
		Class type2 = typeResource2.getEntityClass();

		if ((type1 != null) && (type2 != null)) {

			return EntityContext.getRelationClass(type1, type2);

		}

		if (type1 != null) {

			return EntityContext.getRelationClassForProperty(type1, typeResource2.getRawType());

		}

		return null;
	}

	protected String buildLocationHeader(GraphObject newObject) {

		StringBuilder uriBuilder = securityContext.getBaseURI();

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

	protected void applyDefaultSorting(List<? extends GraphObject> list) {

		if (!list.isEmpty()) {

			// Apply default sorting, if defined
			PropertyKey defaultSort = list.get(0).getDefaultSortKey();

			if (defaultSort != null) {

				String defaultOrder = list.get(0).getDefaultSortOrder();

				Collections.sort(list, new GraphObjectComparator(defaultSort.name(), defaultOrder));

			}
		}
	}

	protected ResourceAccess findGrant() throws FrameworkException {

		Command search                         = Services.command(SecurityContext.getSuperUserInstance(), SearchNodeCommand.class);
		String uriPart                         = EntityContext.normalizeEntityName(this.getResourceSignature());
		List<SearchAttribute> searchAttributes = new LinkedList<SearchAttribute>();
		AbstractNode topNode                   = null;
		boolean includeDeletedAndHidden        = false;
		boolean publicOnly                     = false;
		ResourceAccess grant                   = null;

		searchAttributes.add(Search.andExactType(ResourceAccess.class.getSimpleName()));
		searchAttributes.add(Search.andExactProperty(ResourceAccess.Key.signature, uriPart));

		Result result = (Result) search.execute(topNode, includeDeletedAndHidden, publicOnly, searchAttributes);

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

			AbstractNode node = (AbstractNode) result.get(0);

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
	private int parseInteger(Object source) {

		try {
			return Integer.parseInt(source.toString());
		} catch (Throwable t) {}

		return -1;
	}

	private void checkForIllegalSearchKeys(final HttpServletRequest request, final Set<String> searchableProperties) throws FrameworkException {

		ErrorBuffer errorBuffer = new ErrorBuffer();

		// try to identify invalid search properties and throw an exception
		for (Enumeration<String> e = request.getParameterNames(); e.hasMoreElements(); ) {

			String requestParameterName = e.nextElement();

			if (!searchableProperties.contains(requestParameterName) &&!NON_SEARCH_FIELDS.contains(requestParameterName)) {

				errorBuffer.add("base", new InvalidSearchField(requestParameterName));

			}

		}

		if (errorBuffer.hasError()) {

			throw new FrameworkException(422, errorBuffer);

		}
	}

	public static void registerNonSearchField(String parameterName) {
		NON_SEARCH_FIELDS.add(parameterName);
	}

	//~--- get methods ----------------------------------------------------

	public abstract String getUriPart();

	public abstract Class getEntityClass();

	public abstract String getResourceSignature();

	public ResourceAccess getGrant() throws FrameworkException {
		return findGrant();
	}

	protected DistanceSearchAttribute getDistanceSearch(final HttpServletRequest request) throws FrameworkException {

		String distance = request.getParameter(Search.DISTANCE_SEARCH_KEYWORD);

		if ((request != null) &&!request.getParameterMap().isEmpty() && StringUtils.isNotBlank(distance)) {

			Double dist             = Double.parseDouble(distance);
			StringBuilder searchKey = new StringBuilder();
			Enumeration names       = request.getParameterNames();

			while (names.hasMoreElements()) {

				String name = (String) names.nextElement();

				if (!name.equals(Search.DISTANCE_SEARCH_KEYWORD)
					&& !name.equals(JsonRestServlet.REQUEST_PARAMETER_LOOSE_SEARCH)
					&& !name.equals(JsonRestServlet.REQUEST_PARAMETER_PAGE_SIZE)
					&& !name.equals(JsonRestServlet.REQUEST_PARAMETER_PAGE_NUMBER)
					&& !name.equals(JsonRestServlet.REQUEST_PARAMETER_SORT_KEY)
					&& !name.equals(JsonRestServlet.REQUEST_PARAMETER_SORT_ORDER)
					) {

					searchKey.append(request.getParameter(name)).append(" ");

				}

			}

			return new DistanceSearchAttribute(searchKey.toString(), dist, SearchOperator.AND);

		}

		return null;
	}

	protected boolean hasSearchableAttributesForNodes(final String rawType, final HttpServletRequest request, final List<SearchAttribute> searchAttributes) throws FrameworkException {

		boolean hasSearchableAttributes = false;

		// searchable attributes
		if ((rawType != null) && (request != null) &&!request.getParameterMap().isEmpty()) {

			boolean looseSearch              = parseInteger(request.getParameter(JsonRestServlet.REQUEST_PARAMETER_LOOSE_SEARCH)) == 1;
			Set<String> searchableProperties = null;

			if (looseSearch) {

				searchableProperties = EntityContext.getSearchableProperties(rawType, NodeService.NodeIndex.fulltext.name());

			} else {

				searchableProperties = EntityContext.getSearchableProperties(rawType, NodeService.NodeIndex.keyword.name());

			}

			if (searchableProperties != null) {

				checkForIllegalSearchKeys(request, searchableProperties);

				for (String key : searchableProperties) {

					String searchValue = request.getParameter(key);

					if (searchValue != null) {

						if (looseSearch) {

							// no quotes allowed in loose search queries!
							if (searchValue.contains("\"")) {

								searchValue = searchValue.replaceAll("[\"]+", "");

							}

							if (searchValue.contains("'")) {

								searchValue = searchValue.replaceAll("[']+", "");

							}

							searchAttributes.add(Search.andProperty(key, searchValue));
						} else {

							if (StringUtils.startsWith(searchValue, "[") && StringUtils.endsWith(searchValue, "]")) {

								String strippedValue = StringUtils.stripEnd(StringUtils.stripStart(searchValue, "["), "]");

								searchAttributes.add(Search.andMatchExactValues(key, strippedValue, SearchOperator.OR));

							} else if (StringUtils.startsWith(searchValue, "(") && StringUtils.endsWith(searchValue, ")")) {

								String strippedValue = StringUtils.stripEnd(StringUtils.stripStart(searchValue, "("), ")");

								searchAttributes.add(Search.andMatchExactValues(key, strippedValue, SearchOperator.AND));

							} else {

								searchAttributes.add(Search.andExactProperty(key, searchValue));

							}

						}

						hasSearchableAttributes = true;

					}

				}

			}

		}

		return hasSearchableAttributes;
	}

	protected boolean hasSearchableAttributesForRelationships(final String rawType, final HttpServletRequest request, final List<SearchAttribute> searchAttributes) throws FrameworkException {

		boolean hasSearchableAttributes = false;

		// searchable attributes
		if ((rawType != null) && (request != null) &&!request.getParameterMap().isEmpty()) {

			boolean looseSearch              = parseInteger(request.getParameter(JsonRestServlet.REQUEST_PARAMETER_LOOSE_SEARCH)) == 1;
			Set<String> searchableProperties = null;

			if (looseSearch) {

				searchableProperties = EntityContext.getSearchableProperties(rawType, NodeService.RelationshipIndex.rel_fulltext.name());

			} else {

				searchableProperties = EntityContext.getSearchableProperties(rawType, NodeService.RelationshipIndex.rel_keyword.name());

			}

			if (searchableProperties != null) {

				checkForIllegalSearchKeys(request, searchableProperties);

				for (String key : searchableProperties) {

					String searchValue = request.getParameter(key);

					if (searchValue != null) {

						if (looseSearch) {

							// no quotes allowed in loose search queries!
							if (searchValue.contains("\"")) {

								searchValue = searchValue.replaceAll("[\"]+", "");

							}

							if (searchValue.contains("'")) {

								searchValue = searchValue.replaceAll("[']+", "");

							}

							searchAttributes.add(Search.andProperty(key, searchValue));
						} else {

							if (StringUtils.startsWith(searchValue, "[") && StringUtils.endsWith(searchValue, "]")) {

								String strippedValue = StringUtils.stripEnd(StringUtils.stripStart(searchValue, "["), "]");

								searchAttributes.add(Search.andMatchExactValues(key, strippedValue, SearchOperator.OR));

							} else if (StringUtils.startsWith(searchValue, "(") && StringUtils.endsWith(searchValue, ")")) {

								String strippedValue = StringUtils.stripEnd(StringUtils.stripStart(searchValue, "("), ")");

								searchAttributes.add(Search.andMatchExactValues(key, strippedValue, SearchOperator.AND));

							} else {

								searchAttributes.add(Search.andExactProperty(key, searchValue));

							}

						}

						hasSearchableAttributes = true;

					}

				}

			}

		}

		return hasSearchableAttributes;
	}

	public abstract boolean isCollectionResource() throws FrameworkException;

	public boolean isPrimitiveArray() {
		return false;
	}

	//~--- set methods ----------------------------------------------------

	public void setSecurityContext(SecurityContext securityContext) {
		this.securityContext = securityContext;
	}
}
