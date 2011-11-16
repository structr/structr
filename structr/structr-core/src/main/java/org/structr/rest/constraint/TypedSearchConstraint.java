/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.rest.constraint;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.structr.common.CaseHelper;
import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.node.search.SearchAttribute;
import org.structr.core.node.search.SearchAttributeGroup;
import org.structr.core.node.search.SearchNodeCommand;
import org.structr.core.node.search.SearchOperator;
import org.structr.core.node.search.TextualSearchAttribute;
import org.structr.rest.RestMethodResult;
import org.structr.rest.VetoableGraphObjectListener;
import org.structr.rest.exception.NoResultsException;
import org.structr.rest.exception.PathException;

/**
 * Represents a keyword match using the search term given in the constructor of
 * this class. A SearchConstraint will always result in a list of elements, and
 * will throw an IllegalPathException if it is NOT the last element in an URI.
 *
 * @author Christian Morgner
 */
public class TypedSearchConstraint extends SortableConstraint {

	private static final Logger logger = Logger.getLogger(TypedSearchConstraint.class.getName());

	private TypeConstraint typeConstraint = null;
	private String searchString = null;

	protected TypedSearchConstraint(SecurityContext securityContext) {
		this.securityContext = securityContext;
	}

	public TypedSearchConstraint(SecurityContext securityContext, TypeConstraint typeConstraint, String searchString) {
		this.securityContext = securityContext;
		this.typeConstraint = typeConstraint;
		this.searchString = searchString;
	}

	@Override
	public boolean checkAndConfigure(String part, SecurityContext securityContext, HttpServletRequest request) {
		return false;	// we will not accept URI parts directly
	}

	@Override
	public List<GraphObject> doGet(List<VetoableGraphObjectListener> listeners) throws PathException {

		// build search results
		return getSearchResults(searchString);
	}

	@Override
	public RestMethodResult doPost(Map<String, Object> propertySet, List<VetoableGraphObjectListener> listeners) throws Throwable {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public RestMethodResult doHead() throws Throwable {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public RestMethodResult doOptions() throws Throwable {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public boolean isCollectionResource() {
		return true;
	}

	// ----- private methods -----
	private List<GraphObject> getSearchResults(String searchString) throws PathException {

		List<SearchAttribute> searchAttributes = new LinkedList<SearchAttribute>();
		AbstractNode topNode = null;
		boolean includeDeleted = false;
		boolean publicOnly = false;

		if(searchString != null) {
			
			// prepend "*" to the beginning of the search string
			if(!searchString.startsWith("*")) {
				searchString = "*".concat(searchString);
			}

			// append "*" to the end of the search string
			if(!searchString.endsWith("*")) {
				searchString = searchString.concat("*");
			}

			SearchAttributeGroup typeGroup = new SearchAttributeGroup(SearchOperator.AND);
			//typeGroup.add(new TextualSearchAttribute("type", StringUtils.capitalize(typeConstraint.getType()), SearchOperator.OR));
			typeGroup.add(new TextualSearchAttribute("type", CaseHelper.toCamelCase(typeConstraint.getType()), SearchOperator.OR));
			searchAttributes.add(typeGroup);

			// TODO: configureContext searchable fields
			SearchAttributeGroup nameGroup = new SearchAttributeGroup(SearchOperator.AND);
			nameGroup.add(new TextualSearchAttribute("name",	searchString, SearchOperator.OR));
			nameGroup.add(new TextualSearchAttribute("shortName",	searchString, SearchOperator.OR));
			searchAttributes.add(nameGroup);

			return (List<GraphObject>)Services.command(securityContext, SearchNodeCommand.class).execute(
				topNode,
				includeDeleted,
				publicOnly,
				searchAttributes
			);
		}

		throw new NoResultsException();
	}
}
