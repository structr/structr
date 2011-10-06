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

package org.structr.core.resource.constraint;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.structr.core.GraphObject;
import org.structr.core.entity.StructrRelationship;
import org.structr.core.resource.IllegalPathException;
import org.structr.core.resource.PathException;

/**
 *
 * @author Christian Morgner
 */
public class RelationshipNodeConstraint implements ResourceConstraint {

	private static final Logger logger = Logger.getLogger(RelationshipNodeConstraint.class.getName());
	private boolean startNode = false;

	@Override
	public Result processParentResult(Result result, HttpServletRequest request) throws PathException {

		if(result != null) {

			try {
				List<GraphObject> list = result.getResults();
				if(list != null && list.size() == 1) {

					// we can only operate on a single element here
					GraphObject obj = list.get(0);

					if(obj instanceof StructrRelationship) {

						StructrRelationship rel = (StructrRelationship)obj;
						if(startNode) {

							return new Result(rel.getStartNode());
							
						} else {

							return new Result(rel.getEndNode());
						}
					}
				}

			} catch(Throwable t) {

				logger.log(Level.WARNING, "Exception while fetching relationships", t);
			}

		} else {

			logger.log(Level.INFO, "No results from parent..");

		}

		throw new IllegalPathException();
	}

	@Override
	public boolean acceptUriPart(String part) {

		// only "start" selects the start node, everything else means end node
		if("start".equals(part.toLowerCase())) {
			startNode = true;
		}

		return true;
	}

	@Override
	public ResourceConstraint tryCombineWith(ResourceConstraint next) {
		return null;
	}
}
