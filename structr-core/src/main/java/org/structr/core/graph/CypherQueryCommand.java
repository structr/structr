/*
 *  Copyright (C) 2010-2012 Axel Morgner, structr <structr@structr.org>
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



package org.structr.core.graph;

import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;

//~--- JDK imports ------------------------------------------------------------

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.structr.common.SecurityContext;
import org.structr.core.Services;

//~--- classes ----------------------------------------------------------------

/**
 * Executes the given Cypher query and tries to convert the result in a List
 * of {@link GraphObject}s.
 *
 * @author Christian Morgner
 */
public class CypherQueryCommand extends NodeServiceCommand {

	private static final Logger logger = Logger.getLogger(CypherQueryCommand.class.getName());
	
	protected static final ThreadLocalExecutionEngine engine = new ThreadLocalExecutionEngine();
	
	//~--- methods --------------------------------------------------------

	public List<GraphObject> execute(String query) throws FrameworkException {
		return execute(query, null);
	}

	public List<GraphObject> execute(String query, Map<String, Object> parameters) throws FrameworkException {
		return execute(query, parameters, false);
	}

	public List<GraphObject> execute(String query, Map<String, Object> parameters, boolean includeHiddenAndDeleted) throws FrameworkException {
		return execute(query, parameters, includeHiddenAndDeleted, false);
	}
	
	public List<GraphObject> execute(String query, Map<String, Object> parameters, boolean includeHiddenAndDeleted, boolean publicOnly) throws FrameworkException {

		RelationshipFactory relFactory  = (RelationshipFactory) arguments.get("relationshipFactory");
		//GraphDatabaseService graphDb    = (GraphDatabaseService) arguments.get("graphDb");
		NodeFactory nodeFactory         = new NodeFactory(securityContext);

		List<GraphObject> resultList = new LinkedList<GraphObject>();
		ExecutionResult result       = null;

		if (parameters != null) {

			result = engine.get().execute(query, parameters);
			
		} else {

			result = engine.get().execute(query);
		}

		for (Map<String, Object> row : result) {

			for (Object o : row.values()) {

				if (o instanceof Node) {

					AbstractNode node = nodeFactory.createNode((Node) o, includeHiddenAndDeleted, publicOnly);

					if (node != null) {

						resultList.add(node);
					}

				} else if (o instanceof Relationship) {

					AbstractRelationship rel = relFactory.instantiateRelationship(securityContext, (Relationship) o);

					if (rel != null) {

						resultList.add(rel);
					}

				}

			}

		}

		return resultList;
	}
	
	/**
	 * A thread local version of the neo4j cypher execution engine.
	 */
	protected static class ThreadLocalExecutionEngine extends ThreadLocal<ExecutionEngine> {
		
		@Override
		protected ExecutionEngine initialValue() {
			
			try {
		
				return new ExecutionEngine((GraphDatabaseService)Services.command(SecurityContext.getSuperUserInstance(), GraphDatabaseCommand.class).execute());
				
			} catch (Throwable t) {}
			
			return null;
		}
	}	

}
