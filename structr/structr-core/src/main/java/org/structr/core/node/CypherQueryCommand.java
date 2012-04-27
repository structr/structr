/*
 *  Copyright (C) 2012 Axel Morgner
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
package org.structr.core.node;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.helpers.collection.IteratorUtil;
import org.structr.common.error.FrameworkException;
import org.structr.core.UnsupportedArgumentError;
import org.structr.core.entity.AbstractNode;

/**
 *
 * @author Christian Morgner
 */
public class CypherQueryCommand extends NodeServiceCommand {

	@Override
	public Object execute(Object... parameters) throws FrameworkException {

		GraphDatabaseService graphDb = (GraphDatabaseService)arguments.get("graphDb");
		NodeFactory nodeFactory = (NodeFactory)arguments.get("nodeFactory");

		ExecutionEngine engine = new ExecutionEngine(graphDb);
		
		if(parameters.length < 1) {
			throw new UnsupportedArgumentError("No parameters given. Required parameters: String query");
		}
		
		if(parameters[0] instanceof String) {
			
			List<AbstractNode> resultList = new LinkedList<AbstractNode>();
			ExecutionResult result = engine.execute((String)parameters[0]);
			
			for(String column : result.columns()) {
			
				for(Object o : IteratorUtil.asIterable(result.columnAs(column))) {
				
					if(o instanceof Node) {
						resultList.add(nodeFactory.createNode(securityContext, (Node)o));
					}
				}
			}
			
			return resultList;
			
		} else {
			
			throw new UnsupportedArgumentError("First argument must be of type String!");
		}
	}
}











































