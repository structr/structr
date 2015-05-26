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
package org.structr.core.graph;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;

//~--- JDK imports ------------------------------------------------------------

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Result;
import org.structr.core.GraphObjectMap;
import org.structr.core.property.GenericProperty;

//~--- classes ----------------------------------------------------------------

/**
 * Executes the given Cypher query and tries to convert the result in a List
 * of {@link GraphObject}s.
 *
 * @author Christian Morgner
 */
public class CypherQueryCommand extends NodeServiceCommand {

	private static final Logger logger = Logger.getLogger(CypherQueryCommand.class.getName());

	//protected static final ThreadLocalExecutionEngine engine = new ThreadLocalExecutionEngine();

	//~--- methods --------------------------------------------------------

	public List<GraphObject> execute(String query) throws FrameworkException {
		return execute(query, null);
	}

	public List<GraphObject> execute(String query, Map<String, Object> parameters) throws FrameworkException {
		return execute(query, parameters, true);
	}

	public List<GraphObject> execute(String query, Map<String, Object> parameters, boolean includeHiddenAndDeleted) throws FrameworkException {
		return execute(query, parameters, includeHiddenAndDeleted, false);
	}

	public List<GraphObject> execute(String query, Map<String, Object> parameters, boolean includeHiddenAndDeleted, boolean publicOnly) throws FrameworkException {

		GraphDatabaseService graphDb    = (GraphDatabaseService) arguments.get("graphDb");
		RelationshipFactory relFactory  = new RelationshipFactory(securityContext);
		NodeFactory nodeFactory         = new NodeFactory(securityContext);
		List<GraphObject> resultList    = new LinkedList<>();
		Result result                   = null;

		if (parameters != null) {

			result = graphDb.execute(query, parameters);

		} else {

			result = graphDb.execute(query);
		}

		while (result.hasNext()) {

			final Map<String, Object> row = result.next();
			GraphObjectMap dummyObject    = null;

			for (Entry<String, Object> entry : row.entrySet()) {

				String key   = entry.getKey();
				Object value = entry.getValue();

				if (value instanceof Node) {

					NodeInterface node = nodeFactory.instantiate((Node) value, includeHiddenAndDeleted, publicOnly);

					if (node != null) {

						resultList.add(node);
					}

				} else if (value instanceof Relationship) {

					RelationshipInterface rel = relFactory.instantiate((Relationship) value);

					if (rel != null) {

						resultList.add(rel);
					}

				} else {

					if (dummyObject == null) {

						dummyObject = new GraphObjectMap();
						resultList.add(dummyObject);
					}

					dummyObject.setProperty(new GenericProperty(key), value);
				}

			}

		}

		return resultList;
	}

}
