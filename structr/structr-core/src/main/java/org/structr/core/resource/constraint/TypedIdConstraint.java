/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.resource.constraint;

import java.util.List;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.structr.core.GraphObject;
import org.structr.core.resource.IllegalPathException;
import org.structr.core.resource.NotFoundException;
import org.structr.core.resource.PathException;
import org.structr.core.resource.adapter.ResultGSONAdapter;

/**
 * Represents a type-constrained ID match. A TypedIdConstraint will always
 * result in a single element.
 * 
 * @author Christian Morgner
 */
public class TypedIdConstraint implements ResourceConstraint {

	private static final Logger logger = Logger.getLogger(TypedIdConstraint.class.getName());

	private TypeConstraint typeConstraint = null;
	private IdConstraint idConstraint = null;

	public TypedIdConstraint(IdConstraint idConstraint, TypeConstraint typeConstraint) {
		this.typeConstraint = typeConstraint;
		this.idConstraint = idConstraint;
	}

	@Override
	public boolean acceptUriPart(String part) {
		return false;	// we will not accept URI parts directly
	}

	@Override
	public List<GraphObject> process(List<GraphObject> results, HttpServletRequest request) throws PathException {

		results = idConstraint.process(results, request);
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
	public void configureContext(ResultGSONAdapter resultRenderer) {
	}

	@Override
	public ResourceConstraint tryCombineWith(ResourceConstraint next) {

		if(next instanceof TypeConstraint) {

			// next constraint is a type constraint
			// => follow predefined statc relationship
			//    between the two types
			return new StaticRelationshipConstraint(this, (TypeConstraint)next);
		}

		return null;
	}

	public TypeConstraint getTypeConstraint() {
		return typeConstraint;
	}

	public IdConstraint getIdConstraint() {
		return idConstraint;
	}
}
