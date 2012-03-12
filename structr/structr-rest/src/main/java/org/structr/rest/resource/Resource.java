
/*
* To change this template, choose Tools | Templates
* and open the template in the editor.
 */
package org.structr.rest.resource;

import java.util.*;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.SecurityContext;
import org.structr.core.EntityContext;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.Value;
import org.structr.core.VetoableGraphObjectListener;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.node.RemoveNodeFromIndex;
import org.structr.core.node.StructrTransaction;
import org.structr.core.node.TransactionCommand;
import org.structr.rest.RestMethodResult;
import org.structr.rest.exception.IllegalPathException;
import org.structr.rest.exception.NoResultsException;

//~--- JDK imports ------------------------------------------------------------

import java.util.Map.Entry;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.structr.common.AbstractGraphObjectComparator;
import org.structr.common.PropertyKey;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.InvalidSearchField;
import org.structr.core.*;
import org.structr.core.entity.RelationClass;
import org.structr.core.node.*;
import org.structr.core.node.search.Search;
import org.structr.core.node.search.SearchAttribute;
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

	private static final Logger logger = Logger.getLogger(Resource.class.getName());
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

	public abstract boolean checkAndConfigure(String part, SecurityContext securityContext, HttpServletRequest request);

	public abstract List<? extends GraphObject> doGet() throws FrameworkException;

	public abstract RestMethodResult doPost(final Map<String, Object> propertySet) throws FrameworkException;

	public abstract RestMethodResult doHead() throws FrameworkException;

	public abstract RestMethodResult doOptions() throws FrameworkException;

	public abstract Resource tryCombineWith(Resource next) throws FrameworkException;

	// ----- methods -----
	public RestMethodResult doDelete() throws FrameworkException {

		final Command deleteNode	= Services.command(securityContext, DeleteNodeCommand.class);
		final Command deleteRel		= Services.command(securityContext, DeleteRelationshipCommand.class);
		
		Iterable<? extends GraphObject> results;

		// catch 204, DELETE must return 200 if resource is empty
		try {
			results = doGet();
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
							
							// remove object from index
							Services.command(securityContext, RemoveRelationshipFromIndex.class).execute(obj);

							deleteRel.execute(obj);
							
						} else if (obj instanceof AbstractNode) {
						
							// remove object from index
							Services.command(securityContext, RemoveNodeFromIndex.class).execute(obj);

//							// 2: delete relationships
//							if (obj instanceof AbstractNode) {
//
//								List<AbstractRelationship> rels = ((AbstractNode) obj).getRelationships();
//
//								for (AbstractRelationship rel : rels) {
//
//									deleteRel.execute(rel);
//
//								}
//
//							}
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

		final Iterable<? extends GraphObject> results = doGet();

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

	public boolean isPrimitiveArray() {
		return false;
	}
	
	public void postProcessResultSet(Result result) {
		// override me
	}

	// ----- protected methods -----
	protected RelationClass findRelationClass(TypedIdResource constraint1, TypeResource constraint2) {
		return findRelationClass(constraint1.getTypeResource(), constraint2);
	}

	protected RelationClass findRelationClass(TypeResource constraint1, TypedIdResource constraint2) {
		return findRelationClass(constraint1, constraint2.getTypeResource());
	}

	protected RelationClass findRelationClass(TypedIdResource constraint1, TypedIdResource constraint2) {
		return findRelationClass(constraint1.getTypeResource(), constraint2.getTypeResource());
	}

	protected RelationClass findRelationClass(TypeResource constraint1, TypeResource constraint2) {

		String type1             = constraint1.getRawType();
		String type2             = constraint2.getRawType();

		// try raw type first..
		return EntityContext.getDirectedRelationship(type1, type2);

	}

	protected boolean notifyOfTraversal(List<GraphObject> traversedNodes, ErrorBuffer errorBuffer) {

		boolean hasError = false;
		for (VetoableGraphObjectListener listener : EntityContext.getModificationListeners()) {

			hasError |= listener.wasVisited(traversedNodes, -1, errorBuffer, securityContext);

		}
		return hasError;
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

	protected void applyDefaultSorting(List<GraphObject> list) {

		if (!list.isEmpty()) {

			// Apply default sorting, if defined
			PropertyKey defaultSort = list.get(0).getDefaultSortKey();

			if (defaultSort != null) {

				String defaultOrder = list.get(0).getDefaultSortOrder();

				Collections.sort(list, new AbstractGraphObjectComparator(defaultSort.name(), defaultOrder));

			}
		}
		
	}
	
	protected boolean hasSearchableAttributes(final String rawType, final HttpServletRequest request, final List<SearchAttribute> searchAttributes) throws FrameworkException {

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

							searchAttributes.add(Search.andProperty(key, searchValue));

						} else {

							searchAttributes.add(Search.andExactProperty(key, searchValue));

						}

						hasSearchableAttributes = true;

					}

				}

			}

		}

		return hasSearchableAttributes;
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

	//~--- get methods ----------------------------------------------------

	public abstract String getUriPart();

	public abstract boolean isCollectionResource() throws FrameworkException;

	//~--- set methods ----------------------------------------------------

	public void setSecurityContext(SecurityContext securityContext) {
		this.securityContext = securityContext;
	}
}
