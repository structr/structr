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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import org.structr.common.GraphObjectComparator;
import org.structr.core.property.PropertyKey;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.EntityContext;
import org.structr.core.GraphObject;
import org.structr.core.Result;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.search.Search;
import org.structr.core.graph.search.SearchAttribute;
import org.structr.core.graph.search.SearchNodeCommand;
import org.structr.rest.exception.IllegalPathException;
//~--- JDK imports ------------------------------------------------------------

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
	public boolean checkAndConfigure(final String part, final SecurityContext securityContext, final HttpServletRequest request) throws FrameworkException {

		return super.checkAndConfigure(part, securityContext, request);

	}

	@Override
	public Result doGet(PropertyKey sortKey, boolean sortDescending, final int pageSize, final int page, final String offsetId) throws FrameworkException {

		final List<SearchAttribute> searchAttributes = new LinkedList<SearchAttribute>();
		final boolean includeDeletedAndHidden        = false;
		final boolean publicOnly                     = false;

		if (rawType != null) {

			// searchAttributes.add(new TextualSearchAttribute("type", type, SearchOperator.OR));
			searchAttributes.add(Search.andExactTypeAndSubtypes(EntityContext.normalizeEntityName(rawType)));

			// searchable attributes from EntityContext
			searchAttributes.addAll(extractSearchableAttributesFromRequest(securityContext));

			// default sort key & order
			if(sortKey == null) {

				try {

					// FIXME: this is potentially slow..
					final GraphObject templateEntity  = (GraphObject)entityClass.newInstance();
					final PropertyKey sortKeyProperty = templateEntity.getDefaultSortKey();
					sortDescending                    = GraphObjectComparator.DESCENDING.equals(templateEntity.getDefaultSortOrder());

					if(sortKeyProperty != null) {
						sortKey = sortKeyProperty;
					}

				} catch(final Throwable t) {

					// fallback to name
					sortKey = AbstractNode.name;
				}
			}

			// do search
			final Result results = Services.command(securityContext, SearchNodeCommand.class).execute(
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

		final List emptyList = Collections.emptyList();
		return new Result(emptyList, null, isCollectionResource(), isPrimitiveArray());

	}

	@Override
	public Resource tryCombineWith(final Resource next) throws FrameworkException {

		if (next instanceof UuidResource) {

			return new InheritingTypedIdResource(securityContext, (UuidResource) next, this);
		} else if (next instanceof TypeResource) {

			throw new IllegalPathException();
		}

		return super.tryCombineWith(next);

	}

}
