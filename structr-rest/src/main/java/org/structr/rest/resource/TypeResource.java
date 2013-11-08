/**
 * Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 * This file is part of structr <http://structr.org>.
 *
 * structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.rest.resource;

import org.structr.core.Result;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.EntityContext;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.search.DistanceSearchAttribute;
import org.structr.core.graph.search.Search;
import org.structr.core.graph.search.SearchAttribute;
import org.structr.core.graph.search.SearchNodeCommand;
import org.structr.rest.RestMethodResult;
import org.structr.rest.exception.NotFoundException;

//~--- JDK imports ------------------------------------------------------------

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.structr.common.GraphObjectComparator;
import org.structr.core.property.PropertyKey;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.graph.CreateNodeCommand;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.search.SearchCommand;
import org.structr.core.graph.search.SearchRelationshipCommand;
import org.structr.core.property.PropertyMap;
import org.structr.rest.exception.IllegalPathException;
import static org.structr.rest.resource.Resource.parseInteger;
import org.structr.rest.servlet.JsonRestServlet;

//~--- classes ----------------------------------------------------------------

/**
 * Represents a bulk type match. A TypeResource will always result in a
 * list of elements when it is the last element in an URI. A TypeResource
 * that is not the first element in an URI will try to find a pre-defined
 * relationship between preceding and the node type (defined by
 * {@see AbstractNode#getRelationshipWith}) and follow that path.
 *
 * @author Christian Morgner
 */
public class TypeResource extends SortableResource {

	private static final Logger logger = Logger.getLogger(TypeResource.class.getName());

	//~--- fields ---------------------------------------------------------

	protected Class<? extends SearchCommand> searchCommandType = null;
	protected Class entityClass                            = null;
	protected String rawType                               = null;
	protected HttpServletRequest request                   = null;

	//~--- methods --------------------------------------------------------

	@Override
	public boolean checkAndConfigure(String part, SecurityContext securityContext, HttpServletRequest request) throws FrameworkException {

		this.securityContext = securityContext;
		this.request         = request;
		this.rawType         = part;

		if (rawType != null) {

			// test if resource class exists
			entityClass = EntityContext.getEntityClassForRawType(rawType);
			if (entityClass != null) {
				
				if (AbstractNode.class.isAssignableFrom(entityClass)) {
					searchCommandType = SearchNodeCommand.class;
					return true;
				}
				
				if (AbstractRelationship.class.isAssignableFrom(entityClass)) {
					searchCommandType = SearchRelationshipCommand.class;
					return true;
				}
			}
		}
		
		return true;

	}

	@Override
	public Result doGet(PropertyKey sortKey, boolean sortDescending, int pageSize, int page, String offsetId) throws FrameworkException {

		boolean inexactSearch                  = parseInteger(request.getParameter(JsonRestServlet.REQUEST_PARAMETER_LOOSE_SEARCH)) == 1;
		List<SearchAttribute> searchAttributes = new LinkedList<>();
		boolean includeDeletedAndHidden        = false;
		boolean publicOnly                     = false;

		if (rawType != null) {

			if (entityClass == null) {
				throw new NotFoundException();
			}

			final List<SearchAttribute> validAttributes = extractSearchableAttributes(securityContext, entityClass, request);
			final DistanceSearchAttribute distanceSearch = getDistanceSearch(request, keys(validAttributes));
			
			// distance search?
			if (distanceSearch != null) {
				searchAttributes.add(distanceSearch);
			}

			// add type to return
			
			searchAttributes.add(Search.andTypeAndSubtypes(entityClass, !inexactSearch));
			
			// searchable attributes from EntityContext
			searchAttributes.addAll(validAttributes);
			
			// default sort key & order
			if (sortKey == null) {
				
				try {
					
					GraphObject templateEntity  = ((GraphObject)entityClass.newInstance());
					PropertyKey sortKeyProperty = templateEntity.getDefaultSortKey();
					sortDescending              = GraphObjectComparator.DESCENDING.equals(templateEntity.getDefaultSortOrder());
					
					if (sortKeyProperty != null) {
						
						sortKey = sortKeyProperty;
						
					} else {
						
						sortKey = AbstractNode.name;
					}
					
				} catch(Throwable t) {
					
					// fallback to name
					sortKey = AbstractNode.name;
				}
			}
			
//			
//			// do search 
//			return query
//				.includeDeletedAndHidden(includeDeletedAndHidden)
//				.publicOnly(publicOnly)
//				.sort(actualSortKey)
//				.order(actualSortDescending)
//				.pageSize(pageSize)
//				.page(page)
//				.offsetId(offsetId)
//				.attributes(searchAttributes)
//				.getResult();
			
			
			// do search
			Result results = Services.command(securityContext, searchCommandType).execute(
				includeDeletedAndHidden,
				publicOnly,
				searchAttributes,
				sortKey,
				sortDescending,
				pageSize,
				page,
				offsetId
			);
			
			return results;
			
		} else {

			logger.log(Level.WARNING, "type was null");
		}

		List emptyList = Collections.emptyList();
		return new Result(emptyList, null, isCollectionResource(), isPrimitiveArray());
	}

	@Override
	public RestMethodResult doPost(final Map<String, Object> propertySet) throws FrameworkException {

		final App app         = StructrApp.getInstance(securityContext);
		NodeInterface newNode = null;

		try {
			app.beginTx();
			newNode = createNode(propertySet);
			app.commitTx();
			
		} finally {
			app.finishTx();
		}
		
		RestMethodResult result = new RestMethodResult(HttpServletResponse.SC_CREATED);
		if (newNode != null) {

			result.addHeader("Location", buildLocationHeader(newNode));
		}
		
		// finally: return 201 Created
		return result;
	}

	@Override
	public RestMethodResult doPut(final Map<String, Object> propertySet) throws FrameworkException {

		throw new IllegalPathException();

	}

	@Override
	public RestMethodResult doHead() throws FrameworkException {

		throw new UnsupportedOperationException("Not supported yet.");

	}

	public AbstractNode createNode(final Map<String, Object> propertySet) throws FrameworkException {

		if (entityClass != null) {

			PropertyMap properties = PropertyMap.inputTypeToJavaType(securityContext, entityClass, propertySet);
			properties.put(AbstractNode.type, entityClass.getSimpleName());

			return (AbstractNode) Services.command(securityContext, CreateNodeCommand.class).execute(properties);
			
		}
		
		throw new NotFoundException();
	}

	@Override
	public Resource tryCombineWith(Resource next) throws FrameworkException {

		if (next instanceof UuidResource) {

			TypedIdResource constraint = new TypedIdResource(securityContext, (UuidResource) next, this);

			constraint.configureIdProperty(idProperty);

			return constraint;

		} else if (next instanceof TypeResource) {

			throw new IllegalPathException();
		}

		return super.tryCombineWith(next);

	}

	//~--- get methods ----------------------------------------------------

	@Override
	public String getUriPart() {

		return rawType;

	}

	public String getRawType() {

		return rawType;

	}

	@Override
	public Class getEntityClass() {

		return entityClass;

	}

	@Override
	public String getResourceSignature() {

		return EntityContext.normalizeEntityName(getUriPart());

	}

	@Override
	public boolean isCollectionResource() {

		return true;

	}

	private Set<String> keys(final List<SearchAttribute> attrs) {

		Set<String> keys = new HashSet();
		
		for (SearchAttribute attr : attrs) {
			
			PropertyKey key = attr.getKey();
			if (key != null) {
				
				keys.add(attr.getKey().jsonName());
				
			}
				
		}
		
		return keys;
		
	}
}
