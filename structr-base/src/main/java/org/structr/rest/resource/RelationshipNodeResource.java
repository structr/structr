/*
 * Copyright (C) 2010-2024 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.rest.resource;


import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.search.SortOrder;
import org.structr.api.util.Iterables;
import org.structr.api.util.PagingIterable;
import org.structr.api.util.ResultStream;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.entity.AbstractRelationship;
import org.structr.rest.RestMethodResult;
import org.structr.rest.exception.IllegalPathException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 *
 */
public class RelationshipNodeResource extends WrappingResource {

	private static final Logger logger = LoggerFactory.getLogger(RelationshipNodeResource.class.getName());
	private boolean startNode = false;

	@Override
	public boolean checkAndConfigure(String part, SecurityContext securityContext, HttpServletRequest request) {

		this.securityContext = securityContext;

		// only "start" selects the start node, everything else means end node
		if("start".equals(part.toLowerCase())) {
			startNode = true;
		}

		return true;
	}

	@Override
	public ResultStream doGet(final SortOrder sortOrder, int pageSize, int page) throws FrameworkException {

		List<? extends GraphObject> results = Iterables.toList(wrappedResource.doGet(sortOrder, pageSize, page));
		if(results != null && !results.isEmpty()) {

			try {
				List<GraphObject> resultList = new ArrayList<>();
				for(GraphObject obj : results) {

					if(obj instanceof AbstractRelationship) {

						AbstractRelationship rel = (AbstractRelationship)obj;
						if(startNode) {

							resultList.add(rel.getSourceNode());

						} else {

							resultList.add(rel.getTargetNode());
						}
					}
				}

				return new PagingIterable<>("/" + getUriPart(), resultList, pageSize, page);

			} catch(Throwable t) {

				logger.warn("Exception while fetching relationships", t);
			}

		} else {

			logger.info("No results from parent..");

		}

		throw new IllegalPathException(getResourceSignature() + " can only be applied to a non-empty resource");

	}

	@Override
	public RestMethodResult doPost(Map<String, Object> propertySet) throws FrameworkException {
		if(wrappedResource != null) {
			return wrappedResource.doPost(propertySet);
		}

		throw new IllegalPathException(getResourceSignature() + " can only be applied to a non-empty resource");
	}

	@Override
	public Resource tryCombineWith(Resource next) throws FrameworkException {
		return super.tryCombineWith(next);
	}

        @Override
        public String getResourceSignature() {
                return getUriPart();
        }
}
