/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.rest.constraint;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.structr.common.CaseHelper;
import org.structr.common.ErrorBuffer;
import org.structr.common.SecurityContext;
import org.structr.core.EntityContext;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.StructrRelationship;
import org.structr.core.node.CreateNodeCommand;
import org.structr.core.node.NodeService.NodeIndex;
import org.structr.core.node.RemoveNodeFromIndex;
import org.structr.core.node.StructrTransaction;
import org.structr.core.node.TransactionCommand;
import org.structr.core.node.search.Search;
import org.structr.core.node.search.SearchAttribute;
import org.structr.core.node.search.SearchNodeCommand;
import org.structr.rest.RestMethodResult;
import org.structr.rest.VetoableGraphObjectListener;
import org.structr.rest.exception.IllegalPathException;
import org.structr.rest.exception.NoResultsException;
import org.structr.rest.exception.NotFoundException;
import org.structr.rest.exception.PathException;
import org.structr.rest.servlet.JsonRestServlet;

/**
 * Represents a bulk type match. A TypeConstraint will always result in a
 * list of elements when it is the last element in an URI. A TypeConstraint
 * that is not the first element in an URI will try to find a pre-defined
 * relationship between preceding and the node type (defined by
 * {@see AbstractNode#getRelationshipWith}) and follow that path.
 * 
 * @author Christian Morgner
 */
public class TypeConstraint extends SortableConstraint {
	
	private static final Logger logger = Logger.getLogger(TypeConstraint.class.getName());

	protected HttpServletRequest request = null;
	protected String type = null;
	
	@Override
	public boolean checkAndConfigure(String part, SecurityContext securityContext, HttpServletRequest request) {

		this.securityContext = securityContext;
		this.request = request;

		// todo: check if type exists etc.
		this.setType(part);

		return true;
	}

	@Override
	public List<GraphObject> doGet(final List<VetoableGraphObjectListener> listeners) throws PathException {

		List<SearchAttribute> searchAttributes = new LinkedList<SearchAttribute>();
		boolean hasSearchableAttributes = false;
		AbstractNode topNode = null;
		boolean includeDeleted = false;
		boolean publicOnly = false;

		if(type != null) {

			searchAttributes.add(Search.andExactType(CaseHelper.toCamelCase(type)));

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
			throw new NoResultsException();
		}
	}

	@Override
	public RestMethodResult doPost(final Map<String, Object> propertySet, final List<VetoableGraphObjectListener> listeners) throws Throwable {

		// create transaction closure
		StructrTransaction transaction = new StructrTransaction() {

			@Override
			public Object execute() throws Throwable {

				AbstractNode newNode = createNode(listeners, propertySet);
				ErrorBuffer errorBuffer = new ErrorBuffer();

				if(!validAfterCreation(listeners, newNode, errorBuffer)) {
					throw new IllegalArgumentException(errorBuffer.toString());
				}

				return newNode;
			}
		};

		// execute transaction: create new node
		AbstractNode newNode = (AbstractNode)Services.command(securityContext, TransactionCommand.class).execute(transaction);
		if(newNode == null) {

			// re-throw transaction exception cause
			if(transaction.getCause() != null) {
				throw transaction.getCause();
			}
		}

		// finally: return 201 Created
		RestMethodResult result = new RestMethodResult(HttpServletResponse.SC_CREATED);
		result.addHeader("Location", buildLocationHeader(newNode));
		return result;
	}

	@Override
	public RestMethodResult doHead() throws Throwable {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public RestMethodResult doOptions() throws Throwable {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		
		this.type = type.toLowerCase();

		if(this.type.endsWith("ies")) {
			logger.log(Level.FINEST, "Replacing trailing 'ies' with 'y' for type {0}", type);
			this.type = this.type.substring(0, this.type.length() - 3).concat("y");
		} else
		if(this.type.endsWith("s")) {
			logger.log(Level.FINEST, "Removing trailing plural 's' from type {0}", type);
			this.type = this.type.substring(0, this.type.length() - 1);
		}

		// determine real type
	}

	public AbstractNode createNode(final List<VetoableGraphObjectListener> listeners, final Map<String, Object> propertySet) throws Throwable {

		//propertySet.put(AbstractNode.Key.type.name(), StringUtils.toCamelCase(type));
		propertySet.put(AbstractNode.Key.type.name(), CaseHelper.toCamelCase(type));

		AbstractNode newNode = (AbstractNode)Services.command(securityContext, CreateNodeCommand.class).execute(propertySet);
		ErrorBuffer errorBuffer = new ErrorBuffer();

		if(!mayCreate(listeners, newNode, errorBuffer)) {
			throw new IllegalArgumentException(errorBuffer.toString());
		}

		return newNode;
	}

	@Override
	public ResourceConstraint tryCombineWith(ResourceConstraint next) throws PathException {

		if(next instanceof IdConstraint)	return new TypedIdConstraint(securityContext, (IdConstraint)next, this); else
		if(next instanceof TypeConstraint)	throw new IllegalPathException();

		return super.tryCombineWith(next);
	}

	@Override
	public String getUriPart() {
		return type;
	}

	@Override
	public boolean isCollectionResource() {
		return true;
	}

	// ----- protected methods -----
	protected boolean hasSearchableAttributes(List<SearchAttribute> searchAttributes) {

		boolean hasSearchableAttributes = false;

		// searchable attributes
		if(type != null && request != null && !request.getParameterMap().isEmpty()) {

			boolean strictSearch = parseInteger(request.getParameter(JsonRestServlet.REQUEST_PARAMETER_SEARCH_STRICT)) == 1;

			Set<String> searchableAttributes;
			if (strictSearch) {
				searchableAttributes = EntityContext.getSearchableProperties(type, NodeIndex.keyword.name());
			} else {
				searchableAttributes = EntityContext.getSearchableProperties(type, NodeIndex.fulltext.name());
			}

			if(searchableAttributes != null) {

				for(String key : searchableAttributes) {

					String searchValue = request.getParameter(key);
					if(searchValue != null) {

						if(strictSearch) {
							searchAttributes.add(Search.andExactProperty(key, searchValue));
						} else {
							searchAttributes.add(Search.andProperty(key, searchValue));
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
