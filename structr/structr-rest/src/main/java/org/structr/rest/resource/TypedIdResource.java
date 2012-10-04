/*
 *  Copyright (C) 2010-2012 Axel Morgner, structr <structr@structr.org>
 * 
 *  This file is part of structr <http://structr.org>.
 * 
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.structr.rest.resource;

import org.structr.core.Result;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.EntityContext;
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
	public Result doGet(String sortKey, boolean sortDescending, int pageSize, int page, String offsetId) throws FrameworkException {

		List<GraphObject> results = new LinkedList<GraphObject>();
		AbstractNode node = getTypesafeNode();
		
		if (node != null) {

			results.add(node);
			return new Result(results, null, isCollectionResource(), isPrimitiveArray());
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
		String type = EntityContext.normalizeEntityName(typeResource.getRawType());

		logger.log(Level.FINE, "type from TypeResource: {0}, type from node: {1}", new Object[] { type, node != null ? node.getType() : "null" } );
		
		if(node != null && type.equalsIgnoreCase(node.getType())) {
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
