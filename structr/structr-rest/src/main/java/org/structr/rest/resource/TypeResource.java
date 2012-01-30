/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.rest.resource;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.EntityContext;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
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

	protected HttpServletRequest request = null;
	protected String rawType = null;
	
	@Override
	public boolean checkAndConfigure(String part, SecurityContext securityContext, HttpServletRequest request) {

		this.securityContext = securityContext;
		this.request = request;
		this.rawType = part;

		return true;
	}

	@Override
	public List<GraphObject> doGet() throws FrameworkException {

		List<SearchAttribute> searchAttributes = new LinkedList<SearchAttribute>();
		boolean hasSearchableAttributes = false;
		AbstractNode topNode = null;
		boolean includeDeleted = false;
		boolean publicOnly = false;

		if(rawType != null) {

			searchAttributes.add(Search.andExactType(EntityContext.normalizeEntityName(rawType)));

			// searchable attributes from EntityContext
			hasSearchableAttributes = hasSearchableAttributes(searchAttributes);

			// do search
			List<GraphObject> results = (List<GraphObject>)Services.command(securityContext, SearchNodeCommand.class).execute(
				topNode,
				includeDeleted,
				publicOnly,
				searchAttributes
			);

			if(!results.isEmpty()) {
				return results;
			}
			
		} else {

			logger.log(Level.WARNING, "type was null");
		}

		// return 404 if search attributes were posted
		if(hasSearchableAttributes) {

			throw new NotFoundException();
			
		} else {

			// throw new NoResultsException();
			return Collections.emptyList();
		}
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
		AbstractNode newNode = (AbstractNode)Services.command(securityContext, TransactionCommand.class).execute(transaction);
		RestMethodResult result = new RestMethodResult(HttpServletResponse.SC_CREATED);

		if(newNode != null) {
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

		//propertySet.put(AbstractNode.Key.type.name(), StringUtils.toCamelCase(type));
		propertySet.put(AbstractNode.Key.type.name(), EntityContext.normalizeEntityName(rawType));

		return (AbstractNode)Services.command(securityContext, CreateNodeCommand.class).execute(propertySet);
	}

	@Override
	public Resource tryCombineWith(Resource next) throws FrameworkException {

		if(next instanceof IdResource) {
			
			TypedIdResource constraint = new TypedIdResource(securityContext, (IdResource)next, this);
			constraint.configureIdProperty(idProperty);
			return constraint;

		} else if(next instanceof TypeResource)	throw new IllegalPathException();

		return super.tryCombineWith(next);
	}

	@Override
	public String getUriPart() {
		return rawType;
	}

	@Override
	public boolean isCollectionResource() {
		return true;
	}
	
	public String getRawType() {
		return rawType;
	}

	// ----- protected methods -----
	protected boolean hasSearchableAttributes(List<SearchAttribute> searchAttributes) {

		boolean hasSearchableAttributes = false;

		// searchable attributes
		if(rawType != null && request != null && !request.getParameterMap().isEmpty()) {

			boolean looseSearch = parseInteger(request.getParameter(JsonRestServlet.REQUEST_PARAMETER_LOOSE_SEARCH)) == 1;

			Set<String> searchableAttributes;
			if (looseSearch) {
				searchableAttributes = EntityContext.getSearchableProperties(rawType, NodeIndex.fulltext.name());
			} else {
				searchableAttributes = EntityContext.getSearchableProperties(rawType, NodeIndex.keyword.name());
			}

			if(searchableAttributes != null) {

				for(String key : searchableAttributes) {

					String searchValue = request.getParameter(key);
					if(searchValue != null) {

						if(looseSearch) {
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
		try { return Integer.parseInt(source.toString()); } catch(Throwable t) {}
		return -1;
	}
}
