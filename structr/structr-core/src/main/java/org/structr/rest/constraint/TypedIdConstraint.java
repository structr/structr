/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.rest.constraint;

import java.util.List;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.structr.core.GraphObject;
import org.structr.rest.RestMethodResult;
import org.structr.rest.VetoableGraphObjectListener;
import org.structr.rest.exception.IllegalPathException;
import org.structr.rest.exception.NotFoundException;
import org.structr.rest.exception.PathException;
import org.structr.rest.wrapper.PropertySet;

/**
 * Represents a type-constrained ID match. A TypedIdConstraint will always
 * result in a single element.
 * 
 * @author Christian Morgner
 */
public class TypedIdConstraint extends FilterableConstraint {

	private static final Logger logger = Logger.getLogger(TypedIdConstraint.class.getName());

	private TypeConstraint typeConstraint = null;
	private IdConstraint idConstraint = null;

	public TypedIdConstraint(IdConstraint idConstraint, TypeConstraint typeConstraint) {
		this.securityContext = idConstraint.securityContext;
		this.typeConstraint = typeConstraint;
		this.idConstraint = idConstraint;
	}

	@Override
	public boolean checkAndConfigure(String part, HttpServletRequest request) {
		return false;	// we will not accept URI parts directly
	}

	@Override
	public List<GraphObject> doGet() throws PathException {

		List<GraphObject> results = idConstraint.doGet();
		if(results != null) {

			String type = typeConstraint.getType();

			for(GraphObject obj : results) {
				if(!type.equalsIgnoreCase(obj.getType())) {
					throw new IllegalPathException();
				}
			}

			return results;
		}

		throw new NotFoundException();
	}

	@Override
	public RestMethodResult doPost(PropertySet propertySet, List<VetoableGraphObjectListener> listeners) throws Throwable {
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
	public ResourceConstraint tryCombineWith(ResourceConstraint next) throws PathException {

		if(next instanceof TypeConstraint) {

			// next constraint is a type constraint
			// => follow predefined statc relationship
			//    between the two types
			return new StaticRelationshipConstraint(this, (TypeConstraint)next);
		}

		return super.tryCombineWith(next);
	}

	public TypeConstraint getTypeConstraint() {
		return typeConstraint;
	}

	public IdConstraint getIdConstraint() {
		return idConstraint;
	}
}
