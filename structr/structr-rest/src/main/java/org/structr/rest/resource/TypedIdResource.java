/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.rest.resource;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.entity.AbstractNode;
import org.structr.rest.RestMethodResult;
import org.structr.rest.exception.IllegalMethodException;
import org.structr.rest.exception.NotFoundException;

/**
 * Represents a type-constrained ID match. A TypedIdResource will always
 * result in a single element.
 * 
 * @author Christian Morgner
 */
public class TypedIdResource extends FilterableResource {

	private static final Logger logger = Logger.getLogger(TypedIdResource.class.getName());

	protected TypeResource typeResource = null;
	protected UuidResource idResource = null;

	protected TypedIdResource(SecurityContext securityContext) {
		this.securityContext = securityContext;
		// empty protected constructor
	}

	public TypedIdResource(SecurityContext securityContext, UuidResource idResource, TypeResource typeResource) {
		this.securityContext = securityContext;
		this.typeResource = typeResource;
		this.idResource = idResource;
	}

	@Override
	public boolean checkAndConfigure(String part, SecurityContext securityContext, HttpServletRequest request) throws FrameworkException {
		return false;	// we will not accept URI parts directly
	}

	@Override
	public List<GraphObject> doGet() throws FrameworkException {

		List<GraphObject> results = new LinkedList<GraphObject>();
		AbstractNode node = getTypesafeNode();
		
		if(node != null) {

			results.add(node);
			return results;
		}

		throw new NotFoundException();
	}

	@Override
	public RestMethodResult doPost(Map<String, Object> propertySet) throws FrameworkException {
		throw new IllegalMethodException();
	}

	@Override
	public RestMethodResult doHead() throws FrameworkException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public RestMethodResult doOptions() throws FrameworkException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public AbstractNode getTypesafeNode() throws FrameworkException {
		
		AbstractNode node = idResource.getNode();
//		String type = typeResource.getType();

		// logger.log(Level.INFO, "type from TypeResource: {0}, type from node: {1}", new Object[] { type, node != null ? node.getType() : "null" } );
		
		if(node != null) { //  && type.equalsIgnoreCase(node.getType())) {
			return node;
		}

		
		throw new NotFoundException();
	}
	
	public TypeResource getTypeResource() {
		return typeResource;
	}

	public UuidResource getIdResource() {
		return idResource;
	}

	@Override
	public Resource tryCombineWith(Resource next) throws FrameworkException {

		if(next instanceof TypeResource) {

			// next constraint is a type constraint
			// => follow predefined statc relationship
			//    between the two types
			StaticRelationshipResource resource = new StaticRelationshipResource(securityContext, this, (TypeResource)next);
			resource.configureIdProperty(idProperty);
			return resource;

		} else if(next instanceof TypedIdResource) {

			RelationshipFollowingResource resource = new RelationshipFollowingResource(securityContext, this);
			resource.configureIdProperty(idProperty);
			resource.addTypedIdResource((TypedIdResource)next);

			return resource;
		}

		return super.tryCombineWith(next);
	}

	@Override
	public String getUriPart() {
		return typeResource.getUriPart().concat("/").concat(idResource.getUriPart());
	}
        
	@Override
	public String getResourceSignature() {
		return typeResource.getUriPart();
	}
        
	@Override
	public boolean isCollectionResource() {
		return false;
	}
}
