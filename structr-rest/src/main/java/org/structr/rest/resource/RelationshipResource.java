/**
 * Copyright (C) 2010-2015 Morgner UG (haftungsbeschränkt)
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

import org.structr.core.Result;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.neo4j.graphdb.Direction;
import org.neo4j.helpers.collection.Iterables;
import org.structr.core.property.PropertyKey;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.entity.AbstractNode;
import org.structr.rest.exception.IllegalPathException;

/**
 *
 * @author Christian Morgner
 */
public class RelationshipResource extends WrappingResource {

	private static final Logger logger = Logger.getLogger(RelationshipResource.class.getName());
	private Direction direction = null;

	@Override
	public boolean checkAndConfigure(String part, SecurityContext securityContext, HttpServletRequest request) {

		this.securityContext = securityContext;

		if("in".equals(part.toLowerCase())) {

			direction = Direction.INCOMING;
			return true;

		} else if("out".equals(part.toLowerCase())) {

			direction = Direction.OUTGOING;
			return true;

		}

		return false;
	}

	@Override
	public Result doGet(PropertyKey sortKey, boolean sortDescending, int pageSize, int page, String offsetId) throws FrameworkException {

		List<? extends GraphObject> results = wrappedResource.doGet(sortKey, sortDescending, pageSize, page, offsetId).getResults();
		if(results != null && !results.isEmpty()) {

			try {
				List<GraphObject> resultList = new LinkedList<>();
				for (GraphObject obj : results) {

					if (obj instanceof AbstractNode) {

						List relationships = Direction.INCOMING.equals(direction) ? 
							Iterables.toList(((AbstractNode) obj).getIncomingRelationships()) :
							Iterables.toList(((AbstractNode) obj).getOutgoingRelationships());
						
						if (relationships != null) {

							resultList.addAll(relationships);
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
	public Resource tryCombineWith(Resource next) throws FrameworkException {

		if(next instanceof UuidResource) {
			return next;
		}

		return super.tryCombineWith(next);
	}

	@Override
	public boolean isCollectionResource() {
		return true;
	}

        @Override
        public String getResourceSignature() {
                return wrappedResource.getResourceSignature();
        }
}
