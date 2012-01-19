/*
 *  Copyright (C) 2011 Axel Morgner
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

package org.structr.rest.constraint;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.core.entity.StructrRelationship;
import org.structr.rest.RestMethodResult;
import org.structr.core.VetoableGraphObjectListener;
import org.structr.rest.exception.IllegalPathException;
import org.structr.rest.exception.PathException;

/**
 *
 * @author Christian Morgner
 */
public class RelationshipNodeConstraint extends WrappingConstraint {

	private static final Logger logger = Logger.getLogger(RelationshipNodeConstraint.class.getName());
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
	public List<? extends GraphObject> doGet() throws PathException {

		List<? extends GraphObject> results = wrappedConstraint.doGet();
		if(results != null && !results.isEmpty()) {

			try {
				List<GraphObject> resultList = new LinkedList<GraphObject>();
				for(GraphObject obj : results) {

					if(obj instanceof StructrRelationship) {

						StructrRelationship rel = (StructrRelationship)obj;
						if(startNode) {

							resultList.add(rel.getStartNode());

						} else {

							resultList.add(rel.getEndNode());
						}
					}
				}

				return resultList;

			} catch(Throwable t) {

				logger.log(Level.WARNING, "Exception while fetching relationships", t);
			}

		} else {

			logger.log(Level.INFO, "No results from parent..");

		}

		throw new IllegalPathException();
	}

	@Override
	public RestMethodResult doPost(Map<String, Object> propertySet) throws Throwable {
		if(wrappedConstraint != null) {
			return wrappedConstraint.doPost(propertySet);
		}

		throw new IllegalPathException();
	}

	@Override
	public RestMethodResult doHead() throws Throwable {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public RestMethodResult doOptions() throws Throwable {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public ResourceConstraint tryCombineWith(ResourceConstraint next) throws PathException {
		return super.tryCombineWith(next);
	}
}
