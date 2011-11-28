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

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.Evaluator;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.kernel.Traversal;
import org.neo4j.kernel.Uniqueness;
import org.structr.common.SecurityContext;
import org.structr.core.EntityContext;
import org.structr.core.GraphObject;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.DirectedRelationship;
import org.structr.core.node.StructrNodeFactory;
import org.structr.rest.RestMethodResult;
import org.structr.rest.VetoableGraphObjectListener;
import org.structr.rest.exception.IllegalPathException;
import org.structr.rest.exception.NotFoundException;
import org.structr.rest.exception.PathException;

/**
 * A constraint that collects TypedIdConstraints and tries
 * to verify that a relationship exists between the nodes
 * returned by the collected constraints.
 *
 * @author Christian Morgner
 */
public class RelationshipFollowingConstraint extends SortableConstraint implements Evaluator {

	private static final Logger logger = Logger.getLogger(RelationshipFollowingConstraint.class.getName());

	private Set<DirectedRelationship> visitedRelationships = null;
	private TraversalDescription traversalDescription = null;
	private TypedIdConstraint firstConstraint = null;
	private TypedIdConstraint lastConstraint = null;
	private List<String> uriParts = null;
	private Set<Long> idSet = null;
	private int pathLength = 0;

	public RelationshipFollowingConstraint(SecurityContext securityContext, TypedIdConstraint typedIdConstraint) {

		this.traversalDescription = Traversal.description()
			.depthFirst()
			.uniqueness(Uniqueness.NODE_GLOBAL)
			.evaluator(Evaluators.excludeStartPosition()
		);
		
		this.visitedRelationships = new LinkedHashSet<DirectedRelationship>();
		this.securityContext      = securityContext;
		this.idSet                = new LinkedHashSet<Long>();
		this.uriParts             = new LinkedList<String>();

		// add TypedIdConstraint to list of evaluators
		traversalDescription = traversalDescription.evaluator(this);

		// store first and last constraint separately
		// to be able to access them faster afterwards
		firstConstraint = typedIdConstraint;
		lastConstraint = typedIdConstraint;

		logger.log(Level.INFO, "Adding id {0} to id set", typedIdConstraint.getIdConstraint().getId());

		// add id from TypedIdConstraint to idSet
		idSet.add(typedIdConstraint.getIdConstraint().getId());
	}

	public void addTypedIdConstraint(TypedIdConstraint typedIdConstraint) throws PathException {
		
		logger.log(Level.INFO, "Adding id {0} to id set", typedIdConstraint.getIdConstraint().getId());

		// add id from TypedIdConstraint to idSet
		if(!idSet.add(typedIdConstraint.getIdConstraint().getId())) {
			// id alread in set, this is an illegal path!
			throw new IllegalPathException();
		}

		uriParts.add(typedIdConstraint.getUriPart());

		// find static relationship between the two types
		String type1 = lastConstraint.getTypeConstraint().getType();
		String type2 = typedIdConstraint.getTypeConstraint().getType();
		DirectedRelationship rel = EntityContext.getRelation(type1, type2);
		if(rel != null) {

			if(!visitedRelationships.contains(rel)) {

				traversalDescription.relationships(rel.getRelType(), rel.getDirection());
				visitedRelationships.add(rel);
			}

		} else {

			logger.log(Level.INFO, "No relationship defined between {0} and {1}, illegal path", new Object[] { type1, type2 } );

			// no relationship defined, illegal path
			throw new IllegalPathException();
		}

		// store last constraint separately
		lastConstraint = typedIdConstraint;
	}

	@Override
	public boolean checkAndConfigure(String part, SecurityContext securityContext, HttpServletRequest request) {
		return false;
	}

	@Override
	public List<GraphObject> doGet(List<VetoableGraphObjectListener> listeners) throws PathException {

		Path path = getValidatedPath();
		if(path != null) {

			StructrNodeFactory nodeFactory = new StructrNodeFactory<AbstractNode>(securityContext);
			List<GraphObject> nodeList = new LinkedList<GraphObject>();

			for(Node node : path.nodes()) {
				AbstractNode traversedNode = nodeFactory.createNode(securityContext, node);
				nodeList.add(traversedNode);
			}

			return lastConstraint.doGet(listeners);

		} else {

			logger.log(Level.INFO, "No matching path with length {0}", pathLength);
		}

		throw new NotFoundException();
	}

	@Override
	public RestMethodResult doPost(Map<String, Object> propertySet, List<VetoableGraphObjectListener> listeners) throws Throwable {
		return lastConstraint.doPost(propertySet, listeners);
	}

	@Override
	public RestMethodResult doHead() throws Throwable {
		return lastConstraint.doHead();
	}

	@Override
	public RestMethodResult doOptions() throws Throwable {
		return lastConstraint.doOptions();
	}

	@Override
	public ResourceConstraint tryCombineWith(ResourceConstraint next) throws PathException {

		if(next instanceof TypedIdConstraint) {

			addTypedIdConstraint((TypedIdConstraint)next);
			return this;

		} else if(next instanceof TypeConstraint) {

			// validate path before combining constraints
			if(getValidatedPath() != null) {
				return new StaticRelationshipConstraint(securityContext, lastConstraint, (TypeConstraint)next);
			}
		}
		
		return super.tryCombineWith(next);
	}

	@Override
	public String getUriPart() {
		StringBuilder uri = new StringBuilder();

		for(String part : uriParts) {
			uri.append(part);
			uri.append("/");
		}

		return uri.toString();
	}

	@Override
	public boolean isCollectionResource() {
		return lastConstraint.isCollectionResource();
	}

	// ----- interface Evaluator -----
	@Override
	public Evaluation evaluate(Path path) {

//		logger.log(Level.INFO, "Evaluating node with id {0}", path.endNode().getId());

		// only continue if we are on the right track :)
		if(idSet.contains(path.endNode().getId())) {

//			logger.log(Level.INFO, "Including node with id {0}", path.endNode().getId());
			if(path.length() == pathLength) {
				return Evaluation.INCLUDE_AND_PRUNE;
			} else {
				return Evaluation.INCLUDE_AND_CONTINUE;
			}
		}

		// dead end, stop here
		return Evaluation.EXCLUDE_AND_PRUNE;
	}

	// ----- private methods -----
	private Path getValidatedPath() throws PathException {

		// the nodes we want to find an existing path for.
		Node startNode = firstConstraint.getTypesafeNode().getNode();
		Node endNode = lastConstraint.getTypesafeNode().getNode();

		// set desired path length we want to get
		pathLength = idSet.size();

		// traversal should return exactly one path
		Map<Integer, Path> paths = new HashMap<Integer, Path>();
		for(Iterator<Path> it = traversalDescription.traverse(startNode).iterator(); it.hasNext();) {
			Path path = it.next();
			paths.put(path.length(), path);
		}

		Path path = paths.get(pathLength-1);
		if(path != null && path.startNode().equals(startNode) && path.endNode().equals(endNode)) {
			return path;
		}

		return null;
	}
}
