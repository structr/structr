
/*
* To change this template, choose Tools | Templates
* and open the template in the editor.
 */
package org.structr.rest.resource;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.EntityContext;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.node.CreateNodeCommand;
import org.structr.core.node.StructrTransaction;
import org.structr.core.node.TransactionCommand;
import org.structr.core.node.search.DistanceSearchAttribute;
import org.structr.core.node.search.Search;
import org.structr.core.node.search.SearchAttribute;
import org.structr.core.node.search.SearchNodeCommand;
import org.structr.rest.RestMethodResult;
import org.structr.rest.exception.IllegalPathException;
import org.structr.rest.exception.NotFoundException;

//~--- JDK imports ------------------------------------------------------------

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.structr.core.node.CypherQueryCommand;
import org.structr.core.node.search.*;

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

	private static final Logger logger = Logger.getLogger(TypeResource.class.getName());

	//~--- fields ---------------------------------------------------------

	protected Class entityClass          = null;
	protected String rawType             = null;
	protected HttpServletRequest request = null;

	//~--- methods --------------------------------------------------------

	@Override
	public boolean checkAndConfigure(String part, SecurityContext securityContext, HttpServletRequest request) throws FrameworkException {

		this.securityContext = securityContext;
		this.request         = request;
		this.rawType         = part;

		if (rawType != null) {

			// test if resource class exists
			entityClass = EntityContext.getEntityClassForRawType(rawType);

			if (entityClass == null) {

				// test if key is a known property
				if (!EntityContext.isKnownProperty(part)) {

					return false;

				}
			}
		}

		return true;
	}

	@Override
	public List<GraphObject> doGet() throws FrameworkException {

		List<SearchAttribute> searchAttributes = new LinkedList<SearchAttribute>();
		AbstractNode topNode                   = null;
		boolean includeDeleted                 = false;
		boolean publicOnly                     = false;

		if (rawType != null) {

//                      // test if resource class exists
			// test moved to checkAndConfigure
//                      Map cachedEntities = (Map<String, Class>) Services.command(securityContext, GetEntitiesCommand.class).execute();
//
//                      String normalizedEntityName = EntityContext.normalizeEntityName(rawType);
//
//                      if (!cachedEntities.containsKey(normalizedEntityName)) {
//
//                              throw new NotFoundException();
//
//                      }
//
//                      entityClass = (Class) cachedEntities.get(normalizedEntityName);
//
			if (entityClass == null) {

				throw new NotFoundException();

			}

			

			// distance search?
			DistanceSearchAttribute distanceSearch = getDistanceSearch(request);

			if (distanceSearch != null) {

				searchAttributes.add(distanceSearch);
                                searchAttributes.add(new FilterSearchAttribute(AbstractNode.Key.type.name(), EntityContext.normalizeEntityName(rawType), SearchOperator.AND));

			} else {
                                
                                searchAttributes.addAll(Search.andExactTypeAndSubtypes(EntityContext.normalizeEntityName(rawType)));

				// searchable attributes from EntityContext
				hasSearchableAttributes(rawType, request, searchAttributes);
			}

			// do search
			List<GraphObject> results = (List<GraphObject>) Services.command(securityContext, SearchNodeCommand.class).execute(topNode, includeDeleted, publicOnly, searchAttributes);

			if (!results.isEmpty()) {

				// only sort if distance search is not active
				if(distanceSearch == null) {
					applyDefaultSorting(results);
				}

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

	//~--- get methods ----------------------------------------------------

	@Override
	public String getUriPart() {
		return rawType;
	}

	public String getRawType() {
		return rawType;
	}

	public Class getEntityClass() {
		return entityClass;
	}

	public boolean hasSearchableAttributes(List<SearchAttribute> attributes) throws FrameworkException {
		return hasSearchableAttributes(rawType, request, attributes);
	}

	@Override
	public boolean isCollectionResource() {
		return true;
	}
}
