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

//~--- JDK imports ------------------------------------------------------------

import java.util.List;
import java.util.Map;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.structr.common.error.FrameworkException;

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
	public List<? extends GraphObject> doGet() throws FrameworkException {

		List<GraphObject> results = typedIdConstraint.doGet();
		if (results != null) {

			// fetch static relationship definition
			DirectedRelationship staticRel = findDirectedRelationship(typedIdConstraint, typeConstraint);
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
	public RestMethodResult doDelete() throws FrameworkException {

		List<GraphObject> results = typedIdConstraint.doGet();

		if (results != null) {

			// fetch static relationship definition
			DirectedRelationship staticRel = findDirectedRelationship(typedIdConstraint, typeConstraint);
			if (staticRel != null) {

				AbstractNode startNode = typedIdConstraint.getTypesafeNode();

				if (startNode != null) {

					final List<StructrRelationship> rels = startNode.getRelationships(staticRel.getRelType(), staticRel.getDirection());
					StructrTransaction transaction       = new StructrTransaction() {

						@Override
						public Object execute() throws FrameworkException {

							for (StructrRelationship rel : rels) {
								rel.delete(securityContext);
							}

							return null;
						}
					};

					// execute transaction
					Services.command(securityContext, TransactionCommand.class).execute(transaction);
				}

			}
		}

		return new RestMethodResult(HttpServletResponse.SC_OK);
	}

	@Override
	public RestMethodResult doPost(final Map<String, Object> propertySet) throws FrameworkException {

		// create transaction closure
		StructrTransaction transaction = new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {

				AbstractNode sourceNode  = typedIdConstraint.getIdConstraint().getNode();
				AbstractNode newNode     = typeConstraint.createNode(propertySet);
				DirectedRelationship rel = EntityContext.getDirectedRelationship(sourceNode.getClass(), newNode.getClass());

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

		RestMethodResult result = new RestMethodResult(HttpServletResponse.SC_CREATED);
		if (newNode != null) {
			result.addHeader("Location", buildLocationHeader(newNode));
		}

		return result;
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
	public boolean checkAndConfigure(String part, SecurityContext securityContext, HttpServletRequest request) {
		return false;
	}

	@Override
	public ResourceConstraint tryCombineWith(ResourceConstraint next) throws FrameworkException {
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
