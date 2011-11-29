/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.rest.constraint;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.structr.common.CaseHelper;
import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.node.search.Search;
import org.structr.core.node.search.SearchAttribute;
import org.structr.core.node.search.SearchNodeCommand;
import org.structr.rest.VetoableGraphObjectListener;
import org.structr.rest.exception.IllegalPathException;
import org.structr.rest.exception.NoResultsException;
import org.structr.rest.exception.NotFoundException;
import org.structr.rest.exception.PathException;

/**
 * Like {@link TypeConstraint} but matches inheriting subclasses as well
 * 
 * @author Axel Morgner
 */
public class InheritingTypeConstraint extends TypeConstraint {

	private static final Logger logger = Logger.getLogger(InheritingTypeConstraint.class.getName());

	@Override
	public boolean checkAndConfigure(String part, SecurityContext securityContext, HttpServletRequest request) {

		this.securityContext = securityContext;

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

			//searchAttributes.add(new TextualSearchAttribute("type", type, SearchOperator.OR));
			searchAttributes.addAll(Search.andExactTypeAndSubtypes(CaseHelper.toCamelCase(type)));

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
	public ResourceConstraint tryCombineWith(ResourceConstraint next) throws PathException {

		if(next instanceof IdConstraint)	return new InheritingTypedIdConstraint(securityContext, (IdConstraint)next, this); else
		if(next instanceof TypeConstraint)	throw new IllegalPathException();
		
		return super.tryCombineWith(next);
	}
}
