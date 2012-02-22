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
import org.neo4j.graphdb.RelationshipType;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Command;
import org.structr.core.EntityContext;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.entity.NamedRelation;
import org.structr.core.node.CreateRelationshipCommand;
import org.structr.core.node.search.Search;
import org.structr.core.node.search.SearchAttribute;
import org.structr.core.node.search.SearchRelationshipCommand;
import org.structr.rest.RestMethodResult;
import org.structr.rest.exception.IllegalMethodException;

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

		List<GraphObject> relationResults = new LinkedList<GraphObject>();
		if(wrappedResource != null) {

			// extract relationships from wrapped resource
			List<? extends GraphObject> results = wrappedResource.doGet();
			for(GraphObject obj : results) {
				if (obj instanceof AbstractNode) {
					relationResults.addAll(namedRelation.getRelationships((AbstractNode) obj));
				}
			}

		} else {

			// fetch all relationships of a specific type and return them
			List<SearchAttribute> searchAttributes = new LinkedList<SearchAttribute>();
			searchAttributes.add(Search.andRelType(namedRelation));

			relationResults.addAll((List<AbstractRelationship>)Services.command(securityContext, SearchRelationshipCommand.class).execute(searchAttributes));

		}
		
		return relationResults;
	}

	@Override
	public RestMethodResult doPost(Map<String, Object> propertySet) throws FrameworkException {

		// create new relationship of specified type here

		AbstractRelationship relationshipEntity = namedRelation.newEntityClass();
		if(relationshipEntity != null) {

			// initialize entity temporarily
			relationshipEntity.init(securityContext);

			Command createRel        = Services.command(securityContext, CreateRelationshipCommand.class);
			AbstractNode startNode   = relationshipEntity.identifyStartNode(namedRelation, propertySet);
			AbstractNode endNode     = relationshipEntity.identifyEndNode(namedRelation, propertySet);
			RelationshipType relType = namedRelation.getRelType();

			// create new relationship with startNode, endNode, relType and propertySet
			AbstractRelationship newRel = (AbstractRelationship)createRel.execute(startNode, endNode, relType, propertySet);
			RestMethodResult result = new RestMethodResult(201);
			result.addHeader("Location", buildLocationHeader(newRel));

			return result;

		}

		throw new FrameworkException(422, "FIXME");
	}

	@Override
	public RestMethodResult doPut(Map<String, Object> propertySet) throws FrameworkException {
		throw new IllegalMethodException();
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

		if(next instanceof UuidResource) {
			return new NamedRelationIdResource(this, (UuidResource)next, securityContext);
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
