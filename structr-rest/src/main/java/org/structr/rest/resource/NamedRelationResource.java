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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import org.neo4j.graphdb.RelationshipType;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.common.SecurityContext;
import org.structr.common.error.EmptyPropertyToken;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.Command;
import org.structr.core.EntityContext;
import org.structr.core.GraphObject;
import org.structr.core.Predicate;
import org.structr.core.Result;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.entity.RelationshipMapping;
import org.structr.core.graph.CreateRelationshipCommand;
import org.structr.core.graph.search.Search;
import org.structr.core.graph.search.SearchAttribute;
import org.structr.core.graph.search.SearchRelationshipCommand;
import org.structr.core.graph.search.TextualSearchAttribute;
import org.structr.rest.RestMethodResult;
import org.structr.rest.exception.IllegalMethodException;

/**
 *
 * @author Christian Morgner
 */
public class NamedRelationResource extends WrappingResource {

	private static final Logger logger = Logger.getLogger(NamedRelationResource.class.getName());

	private RelationshipMapping namedRelation = null;
	private HttpServletRequest request = null;

	@Override
	public boolean checkAndConfigure(final String part, final SecurityContext securityContext, final HttpServletRequest request) {

		this.namedRelation = EntityContext.getNamedRelation(part);
		this.securityContext = securityContext;
		this.request = request;

		return namedRelation != null;
	}

	@Override
	public Result doGet(final PropertyKey sortKey, final boolean sortDescending, final int pageSize, final int page, final String offsetId) throws FrameworkException {

		final List<GraphObject> relationResults = new LinkedList<GraphObject>();
		if(wrappedResource != null) {

			// extract relationships from wrapped resource
			final List<? extends GraphObject> results = wrappedResource.doGet(sortKey, sortDescending, pageSize, page, offsetId).getResults();
			for(final GraphObject obj : results) {
				if (obj instanceof AbstractNode) {
					relationResults.addAll(namedRelation.getRelationships(obj));
				}
			}

		} else {

			// fetch all relationships of a specific combinedType and return them
			final List<SearchAttribute> searchAttributes = new LinkedList<SearchAttribute>();
			searchAttributes.add(Search.andExactRelType(namedRelation));

			// add searchable attributes from EntityContext
			searchAttributes.addAll(extractSearchableAttributesForRelationships(securityContext, namedRelation.getEntityClass(), request));

			relationResults.addAll((List<AbstractRelationship>) Services.command(securityContext, SearchRelationshipCommand.class).execute(searchAttributes));

		}

		// filter by searchable properties
		final List<SearchAttribute> filterAttributes =
						extractSearchableAttributesForRelationships(securityContext, namedRelation.getEntityClass(), request);

		if(!filterAttributes.isEmpty()) {

			final Predicate<GraphObject> predicate = new Predicate<GraphObject>() {

				@Override
				public boolean evaluate(final SecurityContext securityContext, final GraphObject... objs) {

					if(objs.length > 0) {

						final GraphObject obj = objs[0];

						for(final SearchAttribute attr : filterAttributes) {

							final String value    = ((TextualSearchAttribute)attr).getValue();
							final PropertyKey key = ((TextualSearchAttribute)attr).getKey();

							final Object val = "\"" + obj.getProperty(key) + "\"";
							if(val != null && val.equals(value)) {
								return true;
							}
						}
					}

					return false;
				}
			};

			for(final Iterator<GraphObject> it = relationResults.iterator(); it.hasNext();) {

				if(!predicate.evaluate(securityContext, it.next())) {
					it.remove();
				}
			}
		}

		return new Result(relationResults, null, isCollectionResource(), isPrimitiveArray());
	}

	@Override
	public RestMethodResult doPost(final Map<String, Object> propertySet) throws FrameworkException {

		// create new relationship of specified combinedType here

		final AbstractRelationship relationshipEntity = namedRelation.newEntityClass();
		if(relationshipEntity != null) {

			// initialize entity temporarily
			relationshipEntity.init(securityContext);

			CreateRelationshipCommand createRel = Services.command(securityContext, CreateRelationshipCommand.class);
			AbstractNode startNode              = relationshipEntity.identifyStartNode(namedRelation, propertySet);
			AbstractNode endNode                = relationshipEntity.identifyEndNode(namedRelation, propertySet);
			RelationshipType relType            = namedRelation.getRelType();
			ErrorBuffer errorBuffer             = new ErrorBuffer();
			boolean hasError                    = false;

			if(startNode == null) {
				errorBuffer.add(namedRelation.getName(), new EmptyPropertyToken(relationshipEntity.getStartNodeIdKey()));
				hasError = true;
			}

			if(endNode == null) {
				errorBuffer.add(namedRelation.getName(), new EmptyPropertyToken(relationshipEntity.getEndNodeIdKey()));
				hasError = true;
			}

			if(hasError) {
				throw new FrameworkException(422, errorBuffer);
			}

			final Class sourceType = namedRelation.getSourceType();
			final Class destType = namedRelation.getDestType();

			propertySet.put(AbstractRelationship.combinedType.dbName(), EntityContext.createCombinedRelationshipType(sourceType, relType, destType));

			// convertFromInput properties
			PropertyMap properties = PropertyMap.inputTypeToJavaType(securityContext, relationshipEntity.getClass(), propertySet);
			
			// create new relationship with startNode, endNode, relType and propertySet
			final AbstractRelationship newRel = createRel.execute(startNode, endNode, relType, properties, false);
			final RestMethodResult result = new RestMethodResult(201);

			result.addHeader("Location", buildLocationHeader(newRel));

			return result;

		}

		throw new FrameworkException(422, "FIXME");
	}

	@Override
	public RestMethodResult doPut(final Map<String, Object> propertySet) throws FrameworkException {
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
	public Resource tryCombineWith(final Resource next) throws FrameworkException {

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

        @Override
        public String getResourceSignature() {
                return getUriPart();
        }
}
