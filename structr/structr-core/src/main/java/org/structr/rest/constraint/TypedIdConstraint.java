/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.rest.constraint;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.RelationshipType;
import org.structr.core.EntityContext;
import org.structr.core.GraphObject;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.DirectedRelationship;
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

		List<GraphObject> results = new LinkedList<GraphObject>();
		AbstractNode node = getTypesafeNode();
		
		if(node != null) {

			results.add(node);
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

	public AbstractNode getTypesafeNode() throws PathException {
		
		AbstractNode node = idConstraint.getNode();
		String type = typeConstraint.getType();
		
		if(type.equalsIgnoreCase(node.getType())) {
			return node;
		}

		logger.log(Level.WARNING, "Path rejected because of type mismatch. Expected {0}, encountered {1}", new Object[] { type, node.getType() } );
		
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
			return new StaticRelationshipConstraint(this, (TypeConstraint)next);

		} else if(next instanceof TypedIdConstraint) {

			return handleNestedIdConstraint((TypedIdConstraint)next);

		} else if(next instanceof StaticRelationshipConstraint) {

			return handleNestedIdConstraint(((StaticRelationshipConstraint)next).getTypedIdConstraint());
		}

		return super.tryCombineWith(next);
	}

	// ----- private methods -----
	private ResourceConstraint handleNestedIdConstraint(TypedIdConstraint next) throws PathException {

		AbstractNode node1 = this.getTypesafeNode();
		AbstractNode node2 = next.getTypesafeNode();

		String type1 = node1.getType();
		String type2 = node2.getType();

		// TODO: verify relationship of correct type between the two nodes
		DirectedRelationship rel = EntityContext.getRelation(type1, type2);
		if(rel != null) {

			RelationshipType relType = rel.getRelType();
			Direction direction = rel.getDirection();

			// use hash code for fast identification
			Set<AbstractNode> relatedNodes = node1.getTraversalResults(relType, direction, type2);
			if(relatedNodes.contains(node2)) {

				// if a relationship exists, this constraint combination
				// is valid and we can remove the preceding constraint
				// from the list. (thus we return next)

				// this is the place where a nested path can be constructed
				// constr2.addPathElement(....);

				return next;
			}
		}

		logger.log(Level.WARNING, "No relationship found between {0} and {1}, throwing 404", new Object[] { node1.getId(), node2.getId() } );

		throw new NotFoundException();

	}
}
