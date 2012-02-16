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
import org.structr.core.GraphObject;
import org.structr.core.entity.AbstractNode;
import org.structr.rest.RestMethodResult;

/**
 *
 * @author Christian Morgner
 */
public class NamedRelationIdResource extends Resource {

	private static final Logger logger = Logger.getLogger(NamedRelationIdResource.class.getName());

	private NamedRelationResource namedRelationResource = null;
	private IdResource idResource = null;

	public NamedRelationIdResource(NamedRelationResource namedRelationResource, IdResource idResource, SecurityContext securityContext) {
		this.namedRelationResource = namedRelationResource;
		this.idResource = idResource;

		this.securityContext = securityContext;
	}

	@Override
	public boolean checkAndConfigure(String part, SecurityContext securityContext, HttpServletRequest request) {
		return false; // no direct instantiation
	}

	@Override
	public List<? extends GraphObject> doGet() throws FrameworkException {

		List<? extends GraphObject> results = namedRelationResource.doGet();
		List<GraphObject> relationResults = new LinkedList<GraphObject>();
		String uuid = idResource.getUriPart();

		// StructrRelationship relationship = idResource.getRelationship();

		// use uuid to filter results from namedRelationResource

		// TODO: we need a relationship index

		// for now, we have no other choice as to iterate over the relationships
		for(GraphObject obj : results) {
			if(uuid.equals(obj.getProperty(AbstractNode.Key.uuid.name()))) {
				relationResults.add(obj);
				break;
			}
		}

		return relationResults;
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

	@Override
	public Resource tryCombineWith(Resource next) throws FrameworkException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public String getUriPart() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public boolean isCollectionResource() {
		return false;
	}

	public NamedRelationResource getNamedRelationResource() {
		return namedRelationResource;
	}

	public IdResource getIdResource() {
		return idResource;
	}

}
