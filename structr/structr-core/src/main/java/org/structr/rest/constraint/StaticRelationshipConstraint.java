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
import javax.servlet.http.HttpServletRequest;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.RelationshipType;
import org.structr.core.GraphObject;
import org.structr.core.entity.DirectedRelationship;
import org.structr.core.entity.StructrRelationship;
import org.structr.rest.exception.IllegalPathException;
import org.structr.rest.exception.PathException;
import org.structr.core.EntityContext;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.node.CreateRelationshipCommand;
import org.structr.core.node.StructrTransaction;
import org.structr.core.node.TransactionCommand;
import org.structr.rest.wrapper.PropertySet;

/**
 *
 * @author Christian Morgner
 */
public class StaticRelationshipConstraint extends FilterableConstraint {

	TypedIdConstraint typedIdConstraint = null;
	TypeConstraint typeConstraint = null;

	public StaticRelationshipConstraint(TypedIdConstraint typedIdConstraint, TypeConstraint typeConstraint) {
		this.typedIdConstraint = typedIdConstraint;
		this.typeConstraint = typeConstraint;
	}

	@Override
	public List<GraphObject> doGet() throws PathException {

		List<GraphObject> results = typedIdConstraint.doGet();
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
	public void doDelete() throws PathException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void doPost(PropertySet propertySet) throws Throwable {

		final AbstractNode sourceNode = typedIdConstraint.getIdConstraint().getNode();
		final AbstractNode newNode = typeConstraint.createNode(propertySet);
		final DirectedRelationship rel = EntityContext.getRelation(sourceNode.getClass(), newNode.getClass());

		if(rel != null) {

			final RelationshipType relType = rel.getRelType();

			// create transaction closure
			StructrTransaction transaction = new StructrTransaction() {

				@Override
				public Object execute() throws Throwable {

					return Services.command(securityContext, CreateRelationshipCommand.class).execute(sourceNode, newNode, relType);
				}
			};

			Services.command(securityContext, TransactionCommand.class).execute(transaction);
			if(transaction.getCause() != null) {
				throw transaction.getCause();
			}
		}

		throw new IllegalPathException();
	}

	@Override
	public void doPut(PropertySet propertySet) throws PathException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void doHead() throws PathException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void doOptions() throws PathException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public boolean checkAndConfigure(String part, HttpServletRequest request) {
		return false;
	}

	@Override
	public ResourceConstraint tryCombineWith(ResourceConstraint next) throws PathException {
		return super.tryCombineWith(next);
	}
}
