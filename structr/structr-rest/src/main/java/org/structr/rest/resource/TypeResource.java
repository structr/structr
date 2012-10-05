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
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.EntityContext;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.node.CreateNodeCommand;
import org.structr.core.node.StructrTransaction;
import org.structr.core.node.TransactionCommand;
import org.structr.core.node.search.*;
import org.structr.core.node.search.DistanceSearchAttribute;
import org.structr.core.node.search.Search;
import org.structr.core.node.search.SearchAttribute;
import org.structr.core.node.search.SearchNodeCommand;
import org.structr.rest.RestMethodResult;
import org.structr.rest.exception.IllegalPathException;
import org.structr.rest.exception.NotFoundException;

//~--- JDK imports ------------------------------------------------------------

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.lucene.search.SortField;
import org.structr.common.GraphObjectComparator;
import org.structr.common.PropertyKey;
import org.structr.core.converter.DateConverter;
import org.structr.core.converter.IntConverter;
import org.structr.core.converter.PropertyConverter;

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

	protected Class entityClass          = null;
	protected String rawType             = null;
	protected HttpServletRequest request = null;

	//~--- methods --------------------------------------------------------

	@Override
	public boolean checkAndConfigure(String part, SecurityContext securityContext, HttpServletRequest request) throws FrameworkException {

		this.securityContext = securityContext;
		this.request         = request;
		this.rawType         = part;

		if (rawType != null) {

			// test if resource class exists
			entityClass = EntityContext.getEntityClassForRawType(rawType);

			if (entityClass == null) {

				// test if key is a known property
				if (!EntityContext.isKnownProperty(part)) {

					return false;
				}
			}
		}

		return true;

	}

	@Override
	public Result doGet(String sortKey, boolean sortDescending, int pageSize, int page, String offsetId) throws FrameworkException {

		List<SearchAttribute> searchAttributes = new LinkedList<SearchAttribute>();
		AbstractNode topNode                   = null;
		boolean includeDeletedAndHidden        = false;
		boolean publicOnly                     = false;

		if (rawType != null) {

//                      // test if resource class exists
			// test moved to checkAndConfigure
//                      Map cachedEntities = (Map<String, Class>) Services.command(securityContext, GetEntitiesCommand.class).execute();
//
//                      String normalizedEntityName = EntityContext.normalizeEntityName(rawType);
//
//                      if (!cachedEntities.containsKey(normalizedEntityName)) {
//
//                              throw new NotFoundException();
//
//                      }
//
//                      entityClass = (Class) cachedEntities.get(normalizedEntityName);
//
			if (entityClass == null) {

				throw new NotFoundException();
			}

			// distance search?
			DistanceSearchAttribute distanceSearch = getDistanceSearch(request);

			if (distanceSearch != null) {

				searchAttributes.add(distanceSearch);
				searchAttributes.add(new FilterSearchAttribute(AbstractNode.Key.type.name(), EntityContext.normalizeEntityName(rawType), SearchOperator.AND));

			} else {

				searchAttributes.add(Search.andExactTypeAndSubtypes(EntityContext.normalizeEntityName(rawType)));

				// searchable attributes from EntityContext
				hasSearchableAttributesForNodes(rawType, request, searchAttributes);

			}
			
			// default sort key & order
			if (sortKey == null) {
				
				try {
					
					GraphObject templateEntity  = ((GraphObject)entityClass.newInstance());
					PropertyKey sortKeyProperty = templateEntity.getDefaultSortKey();
					sortDescending              = GraphObjectComparator.DESCENDING.equals(templateEntity.getDefaultSortOrder());
					
					if (sortKeyProperty != null) {
						sortKey = sortKeyProperty.name();
					}
					
				} catch(Throwable t) {
					
					// fallback to name
					sortKey = "name";
				}
			}
			
			Integer sortType = null;
			PropertyConverter converter = EntityContext.getPropertyConverter(securityContext, entityClass, sortKey);
			if (converter != null) {
				if (converter instanceof IntConverter) {
					sortType = SortField.INT;
				} else if (converter instanceof DateConverter) {
					sortType = SortField.LONG;
				}
			}
			// do search
			Result results = (Result) Services.command(securityContext, SearchNodeCommand.class).execute(
				topNode,
				includeDeletedAndHidden,
				publicOnly,
				searchAttributes,
				sortKey,
				sortDescending,
				pageSize,
				page,
				offsetId,
				sortType
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

		// create transaction closure
		StructrTransaction transaction = new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {

				return createNode(propertySet);

			}

		};

		// execute transaction: create new node
		AbstractNode newNode    = (AbstractNode) Services.command(securityContext, TransactionCommand.class).execute(transaction);
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

	@Override
	public RestMethodResult doOptions() throws FrameworkException {

		throw new UnsupportedOperationException("Not supported yet.");

	}

	public AbstractNode createNode(final Map<String, Object> propertySet) throws FrameworkException {

		// propertySet.put(AbstractNode.Key.type.name(), StringUtils.toCamelCase(type));
		propertySet.put(AbstractNode.Key.type.name(), EntityContext.normalizeEntityName(rawType));

		return (AbstractNode) Services.command(securityContext, CreateNodeCommand.class).execute(propertySet);
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

	public Class getEntityClass() {

		return entityClass;

	}

	@Override
	public String getResourceSignature() {

		return getUriPart();

	}

	public boolean hasSearchableAttributes(List<SearchAttribute> attributes) throws FrameworkException {

		return hasSearchableAttributesForNodes(rawType, request, attributes);

	}

	@Override
	public boolean isCollectionResource() {

		return true;

	}

}
