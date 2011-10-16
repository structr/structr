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

import java.util.LinkedList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.neo4j.graphdb.Direction;
import org.structr.core.GraphObject;
import org.structr.core.entity.DirectedRelationship;
import org.structr.core.entity.StructrRelationship;
import org.structr.core.resource.IllegalPathException;
import org.structr.core.resource.PathException;
import org.structr.core.EntityContext;
import org.structr.core.resource.adapter.ResultGSONAdapter;

/**
 *
 * @author Christian Morgner
 */
public class StaticRelationshipConstraint extends ResourceConstraint {

	TypedIdConstraint typedIdConstraint = null;
	TypeConstraint typeConstraint = null;

	public StaticRelationshipConstraint(TypedIdConstraint typedIdConstraint, TypeConstraint typeConstraint) {
		this.typedIdConstraint = typedIdConstraint;
		this.typeConstraint = typeConstraint;
	}

	@Override
	public List<GraphObject> process(List<GraphObject> results, HttpServletRequest request) throws PathException {

		results = typedIdConstraint.process(results, request);
		if(results != null) {

			// get source and target type from previous constraints
			String sourceType = typedIdConstraint.getTypeConstraint().getType();
			String targetType = typeConstraint.getType();

			// fetch static relationship definition
			DirectedRelationship staticRel = EntityContext.getRelation(sourceType, targetType);
			if(staticRel != null) {

				LinkedList<GraphObject> transformedResults = new LinkedList<GraphObject>();
				for(GraphObject obj : results) {

					if(staticRel.getDirection().equals(Direction.INCOMING)) {

						List<StructrRelationship> rels = obj.getRelationships(staticRel.getRelType(), staticRel.getDirection());
						for(StructrRelationship rel : rels) {
							transformedResults.add(rel.getStartNode());
						}

					} else {

						List<StructrRelationship> rels = obj.getRelationships(staticRel.getRelType(), staticRel.getDirection());
						for(StructrRelationship rel : rels) {
							transformedResults.add(rel.getEndNode());
						}
					}
				}
				
				// return related nodes
				return transformedResults;
			}
		}

		throw new IllegalPathException();
	}

	@Override
	public boolean acceptUriPart(String part) {
		return false;
	}

	@Override
	public ResourceConstraint tryCombineWith(ResourceConstraint next) throws PathException {
		return null;
	}
}
