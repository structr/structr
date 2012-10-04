/*
 *  Copyright (C) 2010-2012 Axel Morgner
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
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.entity.AbstractRelationship;
import org.structr.rest.RestMethodResult;
import org.structr.rest.exception.NotFoundException;

/**
 *
 * @author Christian Morgner
 */
public class RelationshipIdResource extends FilterableResource {

	private static final Logger logger = Logger.getLogger(TypedIdResource.class.getName());

	protected RelationshipResource relationshipResource = null;
	protected UuidResource idResource = null;

	protected RelationshipIdResource(SecurityContext securityContext) {
		this.securityContext = securityContext;
		// empty protected constructor
	}

	public RelationshipIdResource(SecurityContext securityContext, RelationshipResource relationshipResource, UuidResource idResource) {
		this.securityContext = securityContext;
		this.relationshipResource = relationshipResource;
		this.idResource = idResource;
	}

	@Override
	public boolean checkAndConfigure(String part, SecurityContext securityContext, HttpServletRequest request) {
		return false;	// we will not accept URI parts directly
	}

	@Override
	public Result doGet(String sortKey, boolean sortDescending, int pageSize, int page, String offsetId) throws FrameworkException {

		List<GraphObject> uuidResult = new LinkedList<GraphObject>();

		AbstractRelationship rel = idResource.getRelationship();

		if(rel != null) {

			// TODO: do additional type check here!
			
			uuidResult.add(rel);

			return new Result(uuidResult, null, isCollectionResource(), isPrimitiveArray());

		} else {

			throw new NotFoundException();
		}
	}

	@Override
	public RestMethodResult doPost(Map<String, Object> propertySet) throws FrameworkException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public RestMethodResult doHead() throws FrameworkException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public RestMethodResult doOptions() throws FrameworkException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public RelationshipResource getRelationshipResource() {
		return relationshipResource;
	}

	public UuidResource getIdResource() {
		return idResource;
	}

	@Override
	public Resource tryCombineWith(Resource next) throws FrameworkException {

		if(next instanceof RelationshipNodeResource) {

			((RelationshipNodeResource)next).wrapResource(this);
			return next;
		}

		return super.tryCombineWith(next);
	}

	@Override
	public String getUriPart() {
		return relationshipResource.getUriPart().concat("/").concat(idResource.getUriPart());
	}

	@Override
	public boolean isCollectionResource() {
		return false;
	}

        @Override
        public String getResourceSignature() {
                return getUriPart();
        }
}
