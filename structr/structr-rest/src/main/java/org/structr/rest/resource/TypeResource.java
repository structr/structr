
/*
* To change this template, choose Tools | Templates
* and open the template in the editor.
 */
package org.structr.rest.resource;

import org.structr.common.AbstractGraphObjectComparator;
import org.structr.common.PropertyKey;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.InvalidSearchField;
import org.structr.core.EntityContext;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.module.GetEntitiesCommand;
import org.structr.core.node.CreateNodeCommand;
import org.structr.core.node.NodeService.NodeIndex;
import org.structr.core.node.StructrTransaction;
import org.structr.core.node.TransactionCommand;
import org.structr.core.node.search.Search;
import org.structr.core.node.search.SearchAttribute;
import org.structr.core.node.search.SearchNodeCommand;
import org.structr.rest.RestMethodResult;
import org.structr.rest.exception.IllegalPathException;
import org.structr.rest.exception.NotFoundException;
import org.structr.rest.servlet.JsonRestServlet;

//~--- JDK imports ------------------------------------------------------------

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

//~--- classes ----------------------------------------------------------------

/**
 * Represents a bulk type match. A TypeResource will always result in a
 * list of elements when it is the last element in an URI. A TypeResource
 * that is not the first element in an URI will try to find a pre-defined
 * relationship between preceding and the node type (defined by
 * {@see AbstractNode#getRelationshipWith}) and follow that path.
 *
 * @author Christian Morgner
 */
public class TypeResource extends SortableResource {

	private static final Logger logger                 = Logger.getLogger(TypeResource.class.getName());
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

	protected String rawType             = null;
	protected HttpServletRequest request = null;

	//~--- methods --------------------------------------------------------

	@Override
	public boolean checkAndConfigure(String part, SecurityContext securityContext, HttpServletRequest request) {

		this.securityContext = securityContext;
		this.request         = request;
		this.rawType         = part;

		return true;
	}

	@Override
	public List<GraphObject> doGet() throws FrameworkException {

		List<SearchAttribute> searchAttributes = new LinkedList<SearchAttribute>();
		AbstractNode topNode                   = null;
		boolean includeDeleted                 = false;
		boolean publicOnly                     = false;

		if (rawType != null) {

			// test if resource class exists
			Map cachedEntities = (Map<String, Class>) Services.command(securityContext, GetEntitiesCommand.class).execute();

			if (!cachedEntities.containsKey(EntityContext.normalizeEntityName(rawType))) {

				throw new NotFoundException();

			}

			searchAttributes.add(Search.andExactType(EntityContext.normalizeEntityName(rawType)));

			// searchable attributes from EntityContext
			hasSearchableAttributes(searchAttributes);

			// do search
			List<GraphObject> results = (List<GraphObject>) Services.command(securityContext, SearchNodeCommand.class).execute(topNode, includeDeleted, publicOnly, searchAttributes);

			if (!results.isEmpty()) {

				applyDefaultSorting(results);

				return results;

			}
		} else {

			logger.log(Level.WARNING, "type was null");

		}

		return Collections.emptyList();

//              // return 404 if search attributes were posted
//              if(hasSearchableAttributes) {
//
//                      throw new NotFoundException();
//
//              } else {
//                      // throw new NoResultsException();
//                      return Collections.emptyList();
//
//              }
	}

	@Override
	public RestMethodResult doPost(final Map<String, Object> propertySet) throws FrameworkException {

		// create transaction closure
		StructrTransaction transaction = new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {
				return createNode(propertySet);
			}
		};

		// execute transaction: create new node
		AbstractNode newNode    = (AbstractNode) Services.command(securityContext, TransactionCommand.class).execute(transaction);
		RestMethodResult result = new RestMethodResult(HttpServletResponse.SC_CREATED);

		if (newNode != null) {

			result.addHeader("Location", buildLocationHeader(newNode));

		}

		// finally: return 201 Created
		return result;
	}

	@Override
	public RestMethodResult doPut(final Map<String, Object> propertySet) throws FrameworkException {
		throw new IllegalPathException();
	}

	@Override
	public RestMethodResult doHead() throws FrameworkException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public RestMethodResult doOptions() throws FrameworkException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public AbstractNode createNode(final Map<String, Object> propertySet) throws FrameworkException {

		// propertySet.put(AbstractNode.Key.type.name(), StringUtils.toCamelCase(type));
		propertySet.put(AbstractNode.Key.type.name(), EntityContext.normalizeEntityName(rawType));

		return (AbstractNode) Services.command(securityContext, CreateNodeCommand.class).execute(propertySet);
	}

	@Override
	public Resource tryCombineWith(Resource next) throws FrameworkException {

		if (next instanceof UuidResource) {

			TypedIdResource constraint = new TypedIdResource(securityContext, (UuidResource) next, this);

			constraint.configureIdProperty(idProperty);

			return constraint;

		} else if (next instanceof TypeResource) {

			throw new IllegalPathException();

		}

		return super.tryCombineWith(next);
	}

	// ----- private methods -----
	private int parseInteger(Object source) {

		try {
			return Integer.parseInt(source.toString());
		} catch (Throwable t) {}

		return -1;
	}

	private void checkForIllegalSearchKeys(Set<String> searchableProperties) throws FrameworkException {

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

	@Override
	public String getUriPart() {
		return rawType;
	}

	public String getRawType() {
		return rawType;
	}

	// ----- protected methods -----
	protected boolean hasSearchableAttributes(List<SearchAttribute> searchAttributes) throws FrameworkException {

		boolean hasSearchableAttributes = false;

		// searchable attributes
		if ((rawType != null) && (request != null) &&!request.getParameterMap().isEmpty()) {

			boolean looseSearch              = parseInteger(request.getParameter(JsonRestServlet.REQUEST_PARAMETER_LOOSE_SEARCH)) == 1;
			Set<String> searchableProperties = null;

			if (looseSearch) {

				searchableProperties = EntityContext.getSearchableProperties(rawType, NodeIndex.fulltext.name());

			} else {

				searchableProperties = EntityContext.getSearchableProperties(rawType, NodeIndex.keyword.name());

			}

			if (searchableProperties != null) {

				checkForIllegalSearchKeys(searchableProperties);

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

	@Override
	public boolean isCollectionResource() {
		return true;
	}
}
