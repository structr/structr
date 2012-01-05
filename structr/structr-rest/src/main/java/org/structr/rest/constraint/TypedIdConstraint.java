/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.rest.constraint;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.core.entity.AbstractNode;
import org.structr.rest.RestMethodResult;
import org.structr.core.VetoableGraphObjectListener;
import org.structr.rest.exception.NotFoundException;
import org.structr.rest.exception.PathException;

/**
 * Represents a type-constrained ID match. A TypedIdConstraint will always
 * result in a single element.
 * 
 * @author Christian Morgner
 */
public class TypedIdConstraint extends FilterableConstraint {

	private static final Logger logger = Logger.getLogger(TypedIdConstraint.class.getName());

	protected TypeConstraint typeConstraint = null;
	protected IdConstraint idConstraint = null;

	protected TypedIdConstraint(SecurityContext securityContext) {
		this.securityContext = securityContext;
		// empty protected constructor
	}

	public TypedIdConstraint(SecurityContext securityContext, IdConstraint idConstraint, TypeConstraint typeConstraint) {
		this.securityContext = securityContext;
		this.typeConstraint = typeConstraint;
		this.idConstraint = idConstraint;
	}

	@Override
	public boolean checkAndConfigure(String part, SecurityContext securityContext, HttpServletRequest request) {
		return false;	// we will not accept URI parts directly
	}

	@Override
	public List<GraphObject> doGet() throws PathException {

		List<GraphObject> results = new LinkedList<GraphObject>();
		AbstractNode node = getTypesafeNode();
		
		if(node != null) {

			results.add(node);
			return results;
		}

		throw new NotFoundException();
	}

	@Override
	public RestMethodResult doPost(Map<String, Object> propertySet) throws Throwable {
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

	public AbstractNode getTypesafeNode() throws PathException {
		
		AbstractNode node = idConstraint.getNode();
//		String type = typeConstraint.getType();

		// logger.log(Level.INFO, "type from TypeConstraint: {0}, type from node: {1}", new Object[] { type, node != null ? node.getType() : "null" } );
		
		if(node != null) { //  && type.equalsIgnoreCase(node.getType())) {
			return node;
		}

		
		throw new NotFoundException();
	}
	
	public TypeConstraint getTypeConstraint() {
		return typeConstraint;
	}

	public IdConstraint getIdConstraint() {
		return idConstraint;
	}

	@Override
	public ResourceConstraint tryCombineWith(ResourceConstraint next) throws PathException {

		if(next instanceof TypeConstraint) {

			// next constraint is a type constraint
			// => follow predefined statc relationship
			//    between the two types
			StaticRelationshipConstraint constraint = new StaticRelationshipConstraint(securityContext, this, (TypeConstraint)next);
			constraint.configureIdProperty(idProperty);
			return constraint;

		} else if(next instanceof TypedIdConstraint) {

			RelationshipFollowingConstraint constraint = new RelationshipFollowingConstraint(securityContext, this);
			constraint.configureIdProperty(idProperty);
			constraint.addTypedIdConstraint((TypedIdConstraint)next);

			return constraint;

		} else if(next instanceof RelationshipConstraint) {

			// make rel constraint wrap this
			((RelationshipConstraint)next).wrapConstraint(this);
			return next;

		} else if(next instanceof RelationshipIdConstraint) {

			((RelationshipIdConstraint)next).getRelationshipConstraint().wrapConstraint(this);
			return next;
		}

		return super.tryCombineWith(next);
	}

	@Override
	public String getUriPart() {
		return typeConstraint.getUriPart().concat("/").concat(idConstraint.getUriPart());
	}

	@Override
	public boolean isCollectionResource() {
		return false;
	}
}
