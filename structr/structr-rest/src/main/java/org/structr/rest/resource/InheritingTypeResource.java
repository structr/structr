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
import org.structr.core.node.search.Search;
import org.structr.core.node.search.SearchAttribute;
import org.structr.core.node.search.SearchNodeCommand;
import org.structr.rest.exception.IllegalPathException;

//~--- JDK imports ------------------------------------------------------------

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import org.apache.lucene.search.SortField;
import org.structr.common.GraphObjectComparator;
import org.structr.common.PropertyKey;
import org.structr.core.converter.DateConverter;
import org.structr.core.converter.IntConverter;
import org.structr.core.converter.PropertyConverter;

//~--- classes ----------------------------------------------------------------

/**
 * Like {@link TypeResource} but matches inheriting subclasses as well
 *
 * @author Axel Morgner
 */
public class InheritingTypeResource extends TypeResource {

	private static final Logger logger = Logger.getLogger(InheritingTypeResource.class.getName());

	//~--- methods --------------------------------------------------------

	@Override
	public boolean checkAndConfigure(String part, SecurityContext securityContext, HttpServletRequest request) throws FrameworkException {

		return super.checkAndConfigure(part, securityContext, request);

	}

	@Override
	public Result doGet(String sortKey, boolean sortDescending, int pageSize, int page) throws FrameworkException {

		List<SearchAttribute> searchAttributes = new LinkedList<SearchAttribute>();
		AbstractNode topNode                   = null;
		boolean includeDeletedAndHidden        = false;
		boolean publicOnly                     = false;

		if (rawType != null) {

			// searchAttributes.add(new TextualSearchAttribute("type", type, SearchOperator.OR));
			searchAttributes.add(Search.andExactTypeAndSubtypes(EntityContext.normalizeEntityName(rawType)));

			// searchable attributes from EntityContext
			hasSearchableAttributes(searchAttributes);
			
			// default sort key & order
			if(sortKey == null) {
				
				try {
					
					GraphObject templateEntity  = ((GraphObject)entityClass.newInstance());
					PropertyKey sortKeyProperty = templateEntity.getDefaultSortKey();
					sortDescending              = GraphObjectComparator.DESCENDING.equals(templateEntity.getDefaultSortOrder());
					
					if(sortKeyProperty != null) {
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
				sortType
			);
			
			// TODO: SORTING: remove default sorting below
			//applyDefaultSorting(results.getResults());
			
			return results;
			
		} else {

			logger.log(Level.WARNING, "type was null");
		}

		List emptyList = Collections.emptyList();
		return new Result(emptyList, null, isCollectionResource(), isPrimitiveArray());

	}

	@Override
	public Resource tryCombineWith(Resource next) throws FrameworkException {

		if (next instanceof UuidResource) {

			return new InheritingTypedIdResource(securityContext, (UuidResource) next, this);
		} else if (next instanceof TypeResource) {

			throw new IllegalPathException();
		}

		return super.tryCombineWith(next);

	}

}
