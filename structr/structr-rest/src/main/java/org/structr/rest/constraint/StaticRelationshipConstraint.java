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

import java.util.Collections;
import org.structr.common.SecurityContext;
import org.structr.core.EntityContext;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.DirectedRelationship;
import org.structr.core.entity.StructrRelationship;
import org.structr.core.node.StructrTransaction;
import org.structr.core.node.TransactionCommand;
import org.structr.rest.RestMethodResult;
import org.structr.rest.exception.IllegalPathException;
import org.structr.rest.exception.NotAllowedException;
import org.structr.rest.exception.PathException;

//~--- JDK imports ------------------------------------------------------------

import java.util.List;
import java.util.Map;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Christian Morgner
 */
public class StaticRelationshipConstraint extends SortableConstraint {

	private static final Logger logger = Logger.getLogger(StaticRelationshipConstraint.class.getName());
	
	TypeConstraint typeConstraint       = null;
	TypedIdConstraint typedIdConstraint = null;

	//~--- constructors ---------------------------------------------------

	public StaticRelationshipConstraint(SecurityContext securityContext, TypedIdConstraint typedIdConstraint, TypeConstraint typeConstraint) {

		this.securityContext   = securityContext;
		this.typedIdConstraint = typedIdConstraint;
		this.typeConstraint    = typeConstraint;
	}

	//~--- methods --------------------------------------------------------

	@Override
	public List<? extends GraphObject> doGet() throws PathException {

		List<GraphObject> results = typedIdConstraint.doGet();
		if (results != null) {

			// get source and target type from previous constraints
			String sourceType = typedIdConstraint.getTypeConstraint().getType();
			String targetType = typeConstraint.getRawType();

			// logger.log(Level.INFO, "sourceType {0}, targetType {1}", new Object[] { sourceType, targetType } );
			
			// fetch static relationship definition
			DirectedRelationship staticRel = EntityContext.getRelation(sourceType, targetType);

			if (staticRel != null) {

				List<AbstractNode> relatedNodes = staticRel.getRelatedNodes(securityContext, typedIdConstraint.getTypesafeNode());

				if (!relatedNodes.isEmpty()) {

					return relatedNodes;

				}

				//throw new NoResultsException();
				return Collections.emptyList();

			}
		}

		throw new IllegalPathException();
	}

	@Override
	public RestMethodResult doDelete() throws Throwable {

		List<GraphObject> results = typedIdConstraint.doGet();

		if (results != null) {

			// get source and target type from previous constraints
			String sourceType = typedIdConstraint.getTypeConstraint().getType();
			String targetType = typeConstraint.getType();

			// fetch static relationship definition
			DirectedRelationship staticRel = EntityContext.getRelation(sourceType, targetType);

			if (staticRel != null) {

				AbstractNode startNode = typedIdConstraint.getTypesafeNode();

				if (startNode != null) {

					final List<StructrRelationship> rels = startNode.getRelationships(staticRel.getRelType(), staticRel.getDirection());
					StructrTransaction transaction       = new StructrTransaction() {

						@Override
						public Object execute() throws Throwable {

							for (StructrRelationship rel : rels) {
								rel.delete(securityContext);
							}

							return null;
						}
					};

					// execute transaction
					Services.command(securityContext, TransactionCommand.class).execute(transaction);

					// re-throw exception that may occur during transaction
					if(transaction.getCause() != null) {
						throw transaction.getCause();
					}

				}

			}
		}

		return new RestMethodResult(HttpServletResponse.SC_OK);
	}

	@Override
	public RestMethodResult doPost(final Map<String, Object> propertySet) throws Throwable {

		if ((securityContext != null) && (securityContext.getUser() != null)) {

			// create transaction closure
			StructrTransaction transaction = new StructrTransaction() {

				@Override
				public Object execute() throws Throwable {

					AbstractNode sourceNode  = typedIdConstraint.getIdConstraint().getNode();
					AbstractNode newNode     = typeConstraint.createNode(propertySet);
					DirectedRelationship rel = EntityContext.getRelation(sourceNode.getClass(), typeConstraint.getRawType());

					// we try the property name first, then the node type as a fallback
					if(rel == null) {
						rel = EntityContext.getRelation(sourceNode.getClass(), newNode.getType());
					}

					if ((sourceNode != null) && (newNode != null) && (rel != null)) {

						rel.createRelationship(securityContext, sourceNode, newNode);
						return newNode;

					} else {

						logger.log(Level.WARNING, "Unable to create nested node, source node type {0}, new node type {1}, relationship type {2}",
							new Object[] {
								sourceNode != null ? sourceNode.getType() : "null",
								newNode != null ? newNode.getType() : "null",
								rel != null ? rel.getRelType() : "null"
							}
						);
					}

					throw new IllegalPathException();
				}
			};

			// execute transaction: create new node
			AbstractNode newNode = (AbstractNode) Services.command(securityContext, TransactionCommand.class).execute(transaction);
			if (newNode == null) {

				// re-throw transaction exception cause
				if (transaction.getCause() != null) {
					throw transaction.getCause();

				}
			}

			RestMethodResult result = new RestMethodResult(HttpServletResponse.SC_CREATED);
			result.addHeader("Location", buildLocationHeader(newNode));

			return result;

		} else {

			throw new NotAllowedException();

		}
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
	public boolean checkAndConfigure(String part, SecurityContext securityContext, HttpServletRequest request) {
		return false;
	}

	@Override
	public ResourceConstraint tryCombineWith(ResourceConstraint next) throws PathException {
		return super.tryCombineWith(next);
	}

	//~--- get methods ----------------------------------------------------

	@Override
	public String getUriPart() {
		return typedIdConstraint.getUriPart().concat("/").concat(typeConstraint.getUriPart());
	}

	public TypedIdConstraint getTypedIdConstraint() {
		return typedIdConstraint;
	}

	public TypeConstraint getTypeConstraint() {
		return typeConstraint;
	}

	@Override
	public boolean isCollectionResource() {
		return true;
	}
}
