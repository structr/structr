/*
 *  Copyright (C) 2012 Axel Morgner
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

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.EntityContext;
import org.structr.core.GraphObject;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.NamedRelation;
import org.structr.rest.RestMethodResult;
import org.structr.rest.exception.IllegalPathException;

/**
 *
 * @author Christian Morgner
 */
public class NamedRelationResource extends WrappingResource {

	private static final Logger logger = Logger.getLogger(NamedRelationResource.class.getName());

	private NamedRelation namedRelation = null;

	@Override
	public boolean checkAndConfigure(String part, SecurityContext securityContext, HttpServletRequest request) {
		this.securityContext = securityContext;
		this.namedRelation = EntityContext.getNamedRelation(part);

		return namedRelation != null;
	}

	@Override
	public List<? extends GraphObject> doGet() throws FrameworkException {

		if(wrappedResource != null) {
			
			List<? extends GraphObject> results = wrappedResource.doGet();
			List<GraphObject> relationResults = new LinkedList<GraphObject>();

			for(GraphObject obj : results) {
				relationResults.addAll(namedRelation.getRelationships(obj));
			}

			return relationResults;
		}
		
		throw new IllegalPathException();
	}

	@Override
	public RestMethodResult doPost(Map<String, Object> propertySet) throws FrameworkException {
		return new RestMethodResult(200);
	}

	@Override
	public RestMethodResult doPut(Map<String, Object> propertySet) throws FrameworkException {
		return new RestMethodResult(200);
	}

	@Override
	public RestMethodResult doDelete() throws FrameworkException {
		return new RestMethodResult(200);
	}

	@Override
	public RestMethodResult doHead() throws FrameworkException {
		return new RestMethodResult(200);
	}

	@Override
	public RestMethodResult doOptions() throws FrameworkException {
		return new RestMethodResult(200);
	}

	@Override
	public Resource tryCombineWith(Resource next) throws FrameworkException {

		if(next instanceof IdResource) {
			return new NamedRelationIdResource(this, (IdResource)next, securityContext);
		}

		return super.tryCombineWith(next);
	}

	@Override
	public String getUriPart() {
		return namedRelation.getName();
	}

	@Override
	public boolean isCollectionResource() {
		return true;
	}
}
