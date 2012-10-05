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
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.entity.AbstractRelationship;
import org.structr.rest.RestMethodResult;
import org.structr.rest.exception.IllegalPathException;

/**
 *
 * @author Christian Morgner
 */
public class RelationshipNodeResource extends WrappingResource {

	private static final Logger logger = Logger.getLogger(RelationshipNodeResource.class.getName());
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
	public Result doGet(String sortKey, boolean sortDescending, int pageSize, int page, String offsetId) throws FrameworkException {

		List<? extends GraphObject> results = wrappedResource.doGet(sortKey, sortDescending, pageSize, page, offsetId).getResults();
		if(results != null && !results.isEmpty()) {

			try {
				List<GraphObject> resultList = new LinkedList<GraphObject>();
				for(GraphObject obj : results) {

					if(obj instanceof AbstractRelationship) {

						AbstractRelationship rel = (AbstractRelationship)obj;
						if(startNode) {

							resultList.add(rel.getStartNode());

						} else {

							resultList.add(rel.getEndNode());
						}
					}
				}

				return new Result(resultList, null, isCollectionResource(), isPrimitiveArray());

			} catch(Throwable t) {

				logger.log(Level.WARNING, "Exception while fetching relationships", t);
			}

		} else {

			logger.log(Level.INFO, "No results from parent..");

		}

		throw new IllegalPathException();
	}

	@Override
	public RestMethodResult doPost(Map<String, Object> propertySet) throws FrameworkException {
		if(wrappedResource != null) {
			return wrappedResource.doPost(propertySet);
		}

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

	@Override
	public Resource tryCombineWith(Resource next) throws FrameworkException {
		return super.tryCombineWith(next);
	}

        @Override
        public String getResourceSignature() {
                return getUriPart();
        }
}
